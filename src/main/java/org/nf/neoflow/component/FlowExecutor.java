package org.nf.neoflow.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.constants.*;
import org.nf.neoflow.dto.execute.*;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.enums.CacheEnums;
import org.nf.neoflow.enums.LockEnums;
import org.nf.neoflow.exception.NeoExecuteException;
import org.nf.neoflow.exception.NeoFlowConfigException;
import org.nf.neoflow.models.InstanceNode;
import org.nf.neoflow.models.ModelNode;
import org.nf.neoflow.repository.InstanceNodeRepository;
import org.nf.neoflow.repository.ModelNodeRepository;
import org.nf.neoflow.utils.JacksonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * 流程执行组件
 * @author PC8650
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowExecutor {

    private final NeoFlowConfig config;
    private final BaseUserChoose userChoose;
    private final NeoLockManager lockManager;
    private final NeoCacheManager cacheManager;
    private final OperatorManager operatorManager;
    private final ModelNodeRepository modelNodeRepository;
    private final InstanceNodeRepository instanceNodeRepository;
    private final TransactionTemplate transactionTemplate;

    private final Map<Integer, BiFunction<ExecuteForm, Boolean, UpdateResult>> excuteMap = Map.of(
            InstanceOperationType.INITIATE, this::initiate,
            InstanceOperationType.PASS, this::pass,
            InstanceOperationType.REJECTED, this::reject,
            InstanceOperationType.FORWARD, this::forward,
            InstanceOperationType.TERMINATED, this::terminate
    );

    private UserBaseInfo systemOperator;

    private ExecutorService autoNodeExecutor;

    private final String[] DURING_CHAR = new String[4];

    /**
     * 定时检测ASSIGNED_PENDING_COUNT是否归零
     */
    private final ScheduledExecutorService AUTO_LOCK_CHECK_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    /**
     * 定时检测AUTO_EXECUTE是否过期
     */
    private final ScheduledExecutorService AUTO_LOCK_EXPIRED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    /**
     * 已分配且正在执行的节点数
     */
    private final AtomicInteger ASSIGNED_PENDING_COUNT = new AtomicInteger();

    @PostConstruct
    private void init() {
        //DURING_CHAR 初始化
        if (config.getInitialsDuring()) {
            DURING_CHAR[0] = "D";
            DURING_CHAR[1] = "H";
            DURING_CHAR[2] = "M";
            DURING_CHAR[3] = "S";
        } else {
            DURING_CHAR[0] = "天";
            DURING_CHAR[1] = "时";
            DURING_CHAR[2] = "分";
            DURING_CHAR[3] = "秒";
        }
        //systemOperator 初始化
        systemOperator = new UserBaseInfo(config.getAutoId(), config.getAutoName());

        //autoNodeExecutor 初始化
        BlockingQueue<Runnable> queue;
        switch (config.getQueueType().toLowerCase()) {
            case "array" -> queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
            case "linked" -> queue = new LinkedBlockingQueue<>(config.getQueueCapacity());
            case "synchronous" -> queue = new SynchronousQueue<>(false);
            case "priority" -> queue = new PriorityBlockingQueue<>(config.getQueueCapacity());
            default -> throw new NeoFlowConfigException(String.format("队列类型错误，%s", config.getQueueType()));
        }

        String prefix = "auto-node-executor";
        if (config.getEnableVirtual()) {
            //newVirtualThreadPerTaskExecutor()不支持设置参数，自定义ThreadFactory进行控制
            autoNodeExecutor = Executors.newThreadPerTaskExecutor(
                    new ThreadFactory() {
                        //通过信号量控制最大虚拟线程数和等待时间
                        private final Semaphore semaphore = new Semaphore(config.getMaxPoolSize());
                        private final AtomicInteger nextId = new AtomicInteger(1);
                        @Override
                        public Thread newThread(Runnable r) {
                            boolean get;
                            try {
                                get = semaphore.tryAcquire(config.getVirtualAwaitTime(), TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return null;
                            }

                            if (!get) {
                                log.info("线程池已达配置上限");
                                return null;
                            }

                            Thread thread = Thread.ofVirtual().unstarted(() -> {
                                try {
                                    r.run();
                                } finally {
                                    semaphore.release();
                                }
                            });
                            thread.setName(prefix + "-" + nextId.getAndIncrement());
                            return thread;
                        }
                    }
            );
            return;
        }

        RejectedExecutionHandler rejectedExecutionHandler;
        switch (config.getRejectionPolicy().toLowerCase()) {
            case "abort" -> rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
            case "caller-runs" -> rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
            case "discard" -> rejectedExecutionHandler = new ThreadPoolExecutor.DiscardPolicy();
            case "discard-oldest" -> rejectedExecutionHandler = new ThreadPoolExecutor.DiscardOldestPolicy();
            default -> throw new NeoFlowConfigException(String.format("拒绝策略类型错误，%s", config.getRejectionPolicy()));
        }

        autoNodeExecutor = new ThreadPoolExecutor(
                config.getCorePoolSize(),
                config.getMaxPoolSize(),
                config.getKeepAliveTime(), TimeUnit.SECONDS,
                queue,
                new ThreadFactory() {
                    private final AtomicInteger nextId = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName(prefix + "-" + nextId.getAndIncrement());
                        return thread;
                    }
                },
                rejectedExecutionHandler
        );
    }

    /**
     * 实例版本移植，使其在原有基础上使用新的版本模型。
     * 仅在当前节点为待办时，由候选人可确认移植。
     * {@link  GraftForm#getGraftNodeUid() GraftForm.graftNodeUid} 为空时，需要当前节点的modelNodeUid在移植版本的模型节点中有对应；
     * 当前操作类型默认为 {@link  InstanceOperationType#PASS pass} ，将默认选择跳转到移植节点的条件，
     * 是否执行方法由 {@link  GraftForm#getExecuteMethod() GraftForm.executeMethod}决定，默认false
     * 并以移植节点模型生成下一个实例节点
     * @param form 表单
     */
    public void instanceVersionGraft(GraftForm form) {
        UpdateResult updateResult = transactionTemplate.execute(status -> {
            try {
                return executeGraft(form);
            }catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        rightNowNext(updateResult, form.getBusinessKey());
    }

    /**
     * 批量操作
     * @param forms 表单
     * @param clazz 表单类型
     * @return BatchResultDto
     */
    public BatchResultDto batchOperation(Set<?> forms, Class<?> clazz) {
        if (CollectionUtils.isEmpty(forms)) {
            throw new NeoExecuteException("提交表单不能为空");
        }
        int size = forms.size();
        int limit = config.getBatchSize();
        if (size > limit) {
            throw new NeoExecuteException(String.format("当前提交数量-%s，应小于- %s", size, limit));
        }
        int success = 0;
        int fail = 0;
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();
        if (clazz == ExecuteForm.class) {
            //批量执行流程
            ExecuteForm f;
            for (Object form : forms) {
                f = (ExecuteForm) form;
                try {
                    executor(f);
                    success += 1;
                    successList.add(f.getBusinessKey());
                }catch (Exception e) {
                    log.error(e.getMessage(), e);
                    fail += 1;
                    failList.add(f.getBusinessKey() + "：" + e.getMessage());
                }
            }
        }else if (clazz == GraftForm.class) {
            //批量移植流程版本
            GraftForm f;
            for (Object form : forms) {
                f = (GraftForm) form;
                try {
                    instanceVersionGraft(f);
                    success += 1;
                    successList.add(f.getBusinessKey());
                }catch (Exception e) {
                    log.error(e.getMessage(), e);
                    fail += 1;
                    failList.add(f.getBusinessKey() + "：" + e.getMessage());
                }
            }
        }

        return new BatchResultDto(size, success, fail, successList, failList);
    }

    /**
     * 执行器
     * @param form 表单
     */
    public void executor(ExecuteForm form) {
        int operationType = form.getOperationType();
        BiFunction<ExecuteForm, Boolean, UpdateResult> function = excuteMap.get(operationType);
        if (function == null) {
            log.error("错误的操作类型-{}", operationType);
            throw new NeoExecuteException("错误的操作类型");
        }

        //执行当前节点
        UpdateResult updateResult = transactionTemplate.execute(status -> {
            try {
                return function.apply(form, false);
            }catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        rightNowNext(updateResult, form.getBusinessKey());
    }

    /**
     * 执行器
     * @param form 表单
     * @param getLockByLast 是否在上个节点获取锁
     */
    private void executor(ExecuteForm form, Boolean getLockByLast) {
        BiFunction<ExecuteForm, Boolean, UpdateResult> function = excuteMap.get(form.getOperationType());

        //执行当前节点
        UpdateResult updateResult = transactionTemplate.execute(status -> {
            try {
                return function.apply(form, getLockByLast);
            }catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        rightNowNext(updateResult, form.getBusinessKey());
    }

    /**
     * 立即执行下一节点
     * @param updateResult 当前节点更新结果
     * @param businessKey 业务key
     */
    private void rightNowNext(UpdateResult updateResult, String businessKey) {
        //若线程池异常/判断或构建表单异常，需要释放锁
        try {
            //按需自动执行下一节点
            if (updateResult != null && updateResult.autoRightNow()) {
                ExecuteForm nextForm = autoNextForm(updateResult);
                autoNodeExecutor.execute(() -> executor(nextForm, updateResult.getLock()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            lockManager.releaseLock(businessKey, updateResult.getLock(), LockEnums.FLOW_EXECUTE);
        }
    }

    /**
     * 发起流程
     * @param form 表单
     * @param getLockByLast 是否在上个节点获取锁
     * @return 更新结果
     */
    private UpdateResult initiate(ExecuteForm form, Boolean getLockByLast) {
        String processName = form.getProcessName();
        log.info("发起流程 {}", processName);
        boolean getLock = false;
        boolean autoNextRightNow = false;
        try {
            //判断发起条件和加锁
            if (StringUtils.isNotBlank(form.getBusinessKey())) {
                //判断实例是否存在
                canInitiate(form.getBusinessKey());
                //加锁
                if (!getLockByLast) {
                    getLock = lockManager.getLock(form.getBusinessKey(), LockEnums.FLOW_EXECUTE);
                }
            } else {
                if (config.getIndependence()) {
                    log.error("流程执行失败，未设置流程实例业务key：流程 {}-版本 {}", form.getProcessName(), form.getVersion());
                    throw new NeoExecuteException("流程执行失败，未设置流程实例业务key");
                }
                getLock = getLockByLast;
            }

            form.setNum(1);

            //获取当前实例节点并执行节点方法
            NodeQueryDto<InstanceNode> dto = getCurrentInstanceAndOperateMethod(form, false);
            //若发起时未加锁，此时获得businessKey后加锁
            if (!getLock) {
                getLock = lockManager.getLock(form.getBusinessKey(), LockEnums.FLOW_EXECUTE);
            }
            //判断businessKey是否已存在
            canInitiate(form.getBusinessKey());
            //设置流程实例开始时间缓存
            cacheManager.setCache(CacheEnums.I_B_T.getType(), form.getBusinessKey(), dto.getNode().getBeginTime());
            //更新流程
            UpdateResult ur = updateFlowAfterPass(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRightNow();

            return ur;
        } catch (Exception e) {
            lockManager.releaseLock(form.getBusinessKey(), getLock, LockEnums.FLOW_EXECUTE);
            getLock = false;
            throw e;
        } finally {
            if (!autoNextRightNow) {
                lockManager.releaseLock(form.getBusinessKey(), getLock, LockEnums.FLOW_EXECUTE);
            }
        }
    }

    /**
     * 同意
     * @param form 表单
     * @param getLockByLast 是否在上个节点获取锁
     * @return 更新结果
     */
    private UpdateResult pass(ExecuteForm form, Boolean getLockByLast) {
        form.baseCheck();
        String processName = form.getProcessName();
        String businessKey = form.getBusinessKey();
        boolean getLock = false;
        boolean autoNextRightNow = false;
        log.info("同意：流程 {}-版本 {}-key {}- 当前节点位置{}", processName, form.getVersion(), businessKey, form.getNum());
        try {
            //获取锁
            if (!getLockByLast) {
                getLock = lockManager.getLock(businessKey, LockEnums.FLOW_EXECUTE);
            }else {
                getLock = getLockByLast;
            }

            //获取当前实例节点并执行节点方法
            NodeQueryDto<InstanceNode> dto = getCurrentInstanceAndOperateMethod(form, false);

            //更新流程
            UpdateResult ur = updateFlowAfterPass(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRightNow();

            return ur;
        } catch (Exception e) {
            lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            getLock = false;
            throw e;
        } finally {
            if (!autoNextRightNow) {
                lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            }
        }
    }

    /**
     * 拒绝
     * @param form 表单
     * @param getLockByLast 是否在上个节点获取锁
     * @return 更新结果
     */
    private UpdateResult reject(ExecuteForm form, Boolean getLockByLast) {
        form.baseCheck();
        String processName = form.getProcessName();
        String businessKey = form.getBusinessKey();
        boolean getLock = false;
        boolean autoNextRightNow = false;
        log.info("拒绝：流程 {}-版本 {}-key {}- 当前节点位置{}", processName, form.getVersion(), businessKey, form.getNum());
        try {
            //获取锁
            if (!getLockByLast) {
                getLock = lockManager.getLock(businessKey, LockEnums.FLOW_EXECUTE);
            }else {
                getLock = getLockByLast;
            }

            //获取当前实例节点并执行节点方法
            NodeQueryDto<InstanceNode> dto = getCurrentInstanceAndOperateMethod(form, false);

            //更新流程
            UpdateResult ur = updateFlowAfterReject(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRightNow();

            return ur;
        } catch (Exception e) {
            lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            getLock = false;
            throw e;
        } finally {
            if (!autoNextRightNow) {
                lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            }
        }
    }

    /**
     * 转发
     * @param form 表单
     * @param getLockByLast 是否在上个节点获取锁
     * @return 更新结果
     */
    private UpdateResult forward(ExecuteForm form, Boolean getLockByLast) {
        form.forwardCheck();
        String processName = form.getProcessName();
        String businessKey = form.getBusinessKey();
        boolean getLock = false;
        boolean autoNextRightNow = false;
        log.info("转发：流程 {}-版本 {}-key {}- 当前节点位置{}", processName, form.getVersion(), businessKey, form.getNum());
        try {
            //获取锁
            if (!getLockByLast) {
                getLock = lockManager.getLock(businessKey, LockEnums.FLOW_EXECUTE);
            }else {
                getLock = getLockByLast;
            }

            //获取当前实例节点并执行节点方法
            NodeQueryDto<InstanceNode> dto = getCurrentInstanceAndOperateMethod(form, false);

            //更新流程
            UpdateResult ur = updateFlowAfterForward(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRightNow();

            return ur;
        } catch (Exception e) {
            lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            getLock = false;
            throw e;
        } finally {
            if (!autoNextRightNow) {
                lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            }
        }
    }

    /**
     * 终止
     * 强行终止流程，如有必要，请确保版本配置的终止方法能回滚对应的业务数据
     * @param form 表单
     * @param getLockByLast 是否在上个节点获取锁
     * @return 更新结果
     */
    private UpdateResult terminate(ExecuteForm form, Boolean getLockByLast) {
        form.baseCheck();
        String processName = form.getProcessName();
        String businessKey = form.getBusinessKey();
        boolean getLock = false;
        boolean autoNextRightNow = false;
        log.info("终止：流程 {}-版本 {}-key {}- 当前节点位置{}", processName, form.getVersion(), businessKey, form.getNum());
        try {
            //获取锁
            if (!getLockByLast) {
                getLock = lockManager.getLock(businessKey, LockEnums.FLOW_EXECUTE);
            }else {
                getLock = getLockByLast;
            }

            //获取当前实例节点并执行节点方法
            NodeQueryDto<InstanceNode> dto = getCurrentInstanceAndOperateMethod(form, true);

            //更新流程
            UpdateResult ur = updateFlowAfterTerminate(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRightNow();

            return ur;
        } catch (Exception e) {
            lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            getLock = false;
            throw e;
        } finally {
            if (!autoNextRightNow) {
                lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            }
        }
    }

    /**
     * 执行流程实例版本移植
     * @param form 表单
     * @return updateResult
     */
    public UpdateResult executeGraft(GraftForm form) {
        boolean getLock = false;
        boolean autoNextRightNow = false;
        String businessKey = form.getBusinessKey();
        log.info("移植流程版本：流程 {}-版本 {}-key {} -移植版本 {}",
                form.getProcessName(), form.getVersion(), businessKey, form.getGraftVersion());
        try {
            form.check();
            getLock = lockManager.getLock(businessKey, LockEnums.FLOW_EXECUTE);
            //获取当前节点信息
            ExecuteForm executeForm = new ExecuteForm(form);
            executeForm.setOperator(userChoose.user(form.getOperator()));
            NodeQueryDto<InstanceNode> dto = queryCurrentInstanceNode(executeForm);
            InstanceNode current = dto.getNode();

            //校验候选人
            inCandidate(executeForm, current, false);

            //查询移植版本模型节点
            if (StringUtils.isBlank(form.getGraftNodeUid())) {
                form.setGraftNodeUid(current.getModelNodeUid());
            }
            NodeQueryDto<ModelNode> modeDto = graftVersionModelNode(form);
            executeForm.setOperationType(InstanceOperationType.PASS);

            //执行节点方法
            if (form.getExecuteMethod()) {
                executeForm.setOperationMethod(current.getOperationMethod());
                operateMethod(executeForm);
            }
            //设置跳转条件为移植版本模型节点跳转条件
            executeForm.setCondition(modeDto.getCondition());

            //构建下一个实例节点
            InstanceNode next = constructInstanceNode(modeDto.getNode(), executeForm);

            //设置结束时间、实际操作人、状态
            current.setEndTime(LocalDateTime.now());
            current.setOperationBy(JacksonUtils.toJson(form.getOperator()));
            current.setStatus(operateTypeToNodeStatus(executeForm.getOperationType()));
            current.setDuring(getDuring(Duration.between(current.getBeginTime(), current.getEndTime())));

            next.setBeginTime(current.getEndTime());

            //更新流程
            UpdateResult updateResult = updateInstanceByGraft(current, next, executeForm, form);
            autoNextRightNow = updateResult.autoRightNow();
            return updateResult;
        } catch (Exception e) {
            lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            getLock = false;
            throw e;
        } finally {
            if (!autoNextRightNow) {
                lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            }
        }
    }

    /**
     * 获取当前实例节点并执行节点方法
     * @param form 表单
     * @param isTerminated 是否为终止操作
     * @return 实例节点查询数据
     */
    private NodeQueryDto<InstanceNode> getCurrentInstanceAndOperateMethod(ExecuteForm form, Boolean isTerminated) {
        //获取/校验当前用户信息
        form.setOperator(userChoose.user(form.getOperator()));
        //获取实例节点
        NodeQueryDto<InstanceNode> dto = getInstanceNode(form);
        //执行
        operateMethod(form, dto.getNode(), dto.getTerminatedMethod(), isTerminated);

        return dto;
    }

    /**
     * 执行节点方法
     * @param form 表单
     * @param current 当前实例节点
     * @param terminatedMethod 终止方法
     * @param isTerminated 是否为终止操作
     */
    private void operateMethod(ExecuteForm form, InstanceNode current, String terminatedMethod, Boolean isTerminated) {
        //判断候选人身份
        inCandidate(form, current, isTerminated);

        log.info("流程操作类型：{}", form.getOperationType());

        //执行节点方法
        if (config.getIndependence()) {
            log.info("独立部署，跳过流程方法");
        } else if ((current.getOnlyPassExecute() && form.getOperationType() < InstanceOperationType.REJECTED)
                || (!current.getOnlyPassExecute() && form.getOperationType() < InstanceOperationType.FORWARD)) {
            //记录关键数据
            String businessKey = form.getBusinessKey();
            String processName = form.getProcessName();
            Integer version = form.getVersion();
            Long nodeId = form.getNodeId();
            Integer num = form.getNum();

            //执行节点方法
            log.info("集成部署，执行流程方法-{}", form.getOperationMethod());
            form = operatorManager.operate(form);

            //判断返回的businessKey
            if (StringUtils.isBlank(form.getBusinessKey())) {
                log.error("流程执行失败，未设置流程实例业务key：流程 {}-版本 {}", form.getProcessName(), form.getVersion());
                throw new NeoExecuteException("流程执行失败，未设置流程实例业务key");
            }

            //校验关键数据一致性
            if ((StringUtils.isNotBlank(businessKey) && !Objects.equals(form.getBusinessKey(), businessKey)) ||
                    !Objects.equals(form.getProcessName(), processName) ||
                    !Objects.equals(form.getVersion(), version) ||
                    !Objects.equals(form.getNodeId(), nodeId) ||
                    !Objects.equals(form.getNum(), num)) {
                log.error("流程执行失败，关键数据不一致：流程 {}-版本 {}-key {}-当前节点位置 {}", processName, version, businessKey, num);
                throw new NeoExecuteException("流程执行失败，节点方法后关键数据变更");
            }
        } else if (isTerminated) {
            log.info("执行流程终止方法-{}", terminatedMethod);
            form.setOperationMethod(terminatedMethod);
            form = operatorManager.operate(form);
        }

        //发起、通过 必须有跳转条件
        if (form.getOperationType() < InstanceOperationType.REJECTED && current.getLocation() <= NodeLocationType.MIDDLE) {
            if (form.getCondition() == null) {
                if (current.getDefaultPassCondition() == null) {
                    log.error("流程执行失败，缺失跳转条件：流程 {}-版本 {}-key {}-当前节点位置 {}",
                            form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
                    throw new NeoExecuteException("流程执行失败，缺失跳转条件");
                }
                form.setCondition(current.getDefaultPassCondition());
            }
        }
    }

    /**
     * 执行节点方法
     * @param form 表单
     */
    private void operateMethod(ExecuteForm form) {
        log.info("移植流程实例版本，执行节点方法");
        //执行节点方法
        if (config.getIndependence()) {
            log.info("独立部署，跳过流程方法");
            return;
        }

        //记录关键数据
        String businessKey = form.getBusinessKey();
        String processName = form.getProcessName();
        Integer version = form.getVersion();
        Long nodeId = form.getNodeId();
        Integer num = form.getNum();

        //执行节点方法
        log.info("集成部署，执行流程方法-{}", form.getOperationMethod());
        form = operatorManager.operate(form);

        //判断返回的businessKey
        if (StringUtils.isBlank(form.getBusinessKey())) {
            log.error("流程执行失败，未设置流程实例业务key：流程 {}-版本 {}", form.getProcessName(), form.getVersion());
            throw new NeoExecuteException("流程执行失败，未设置流程实例业务key");
        }

        //校验关键数据一致性
        if (!Objects.equals(form.getBusinessKey(), businessKey) ||
                !Objects.equals(form.getProcessName(), processName) ||
                !Objects.equals(form.getVersion(), version) ||
                !Objects.equals(form.getNodeId(), nodeId) ||
                !Objects.equals(form.getNum(), num)) {
            log.error("流程执行失败，关键数据不一致：流程 {}-版本 {}-key {}-当前节点位置 {}", processName, version, businessKey, num);
            throw new NeoExecuteException("流程执行失败，节点方法后关键数据变更");
        }
    }

    /**
     * 移植版本更新流程
     * @param current 当前实例节点
     * @param next  下一个实例节点
     * @param executeForm 执行表单
     * @param graftForm 移植表单
     * @return 更新结果
     */
    private UpdateResult updateInstanceByGraft(InstanceNode current, InstanceNode next, ExecuteForm executeForm, GraftForm graftForm) {
        current.setOperationRemark(executeForm.getOperationRemark());
        executeForm.setOperationRemark(null);
        //计算流程持续时间
        if (!InstanceOperationType.INITIATE.equals(executeForm.getOperationType())) {
            current.setProcessDuring(getInstanceDuring(executeForm, current.getEndTime()));
        }
        //spring-data-neo4j复杂对象作为参数，需转成map，LocalDateTime、LocalDate会被转成数组，需手动处理
        Map<String, Object> cMap = JacksonUtils.objToMap(current);
        Map<String, Object> nMap = JacksonUtils.objToMap(next);
        updateMapReady(current, next, cMap, nMap);
        Integer flowStatus = getFlowStatus(current, next);
        Long nextId;

        log.info("移植流程版本：流程 {}-版本 {}-key {} -移植版本 {}",
                executeForm.getProcessName(), executeForm.getVersion(), executeForm.getBusinessKey(), graftForm.getGraftVersion());
        if (executeForm.getNum() < 5) {
            nextId = instanceNodeRepository.updateFlowInstanceByGraft(executeForm.getProcessName(), executeForm.getVersion(),
                    executeForm.getNodeId(), executeForm.getBusinessKey(), executeForm.getCondition(), flowStatus, graftForm.getGraftVersion(),
                    graftForm.getListData(), graftForm.getVariableData(), cMap, nMap);
        } else {
            nextId = instanceNodeRepository.updateFlowInstanceByGraftTooLong(executeForm.getProcessName(), executeForm.getVersion(),
                    executeForm.getNodeId(), graftForm.getNum(), executeForm.getBusinessKey(), executeForm.getCondition(), flowStatus, graftForm.getGraftVersion(),
                    graftForm.getListData(), graftForm.getVariableData(), cMap, nMap);
        }
        log.info("流程状态移植：流程 {}-版本 {}-key {} -移植版本 {}",
                executeForm.getProcessName(), executeForm.getVersion(), executeForm.getBusinessKey(), graftForm.getGraftVersion());

        //删除实例操作历史缓存i_o_h
        cacheManager.deleteCache(CacheEnums.I_O_H.getType(), List.of(executeForm.getBusinessKey(), cacheManager.mergeKey(executeForm.getBusinessKey(), executeForm.getNum().toString())));

        next.setId(nextId);
        //移植表单自增num和设置nodeId，用于返回
        graftForm.increment(nextId);
        //执行表单这种版本为移植版本，用于后续执行自动节点
        executeForm.setVersion(graftForm.getGraftVersion());
        return new UpdateResult(executeForm, next, next.getAutoTime() != null, true);
    }

    /**
     * 更新流程
     * @param current 当前实例节点
     * @param next  下一个实例节点
     * @param form 表单
     * @param autoNextRightNow 是否自动执行下一个节点
     * @param getLock 是否获取锁
     * @return 更新结果
     */
    private UpdateResult updateInstance(InstanceNode current, InstanceNode next, ExecuteForm form,
                                        Boolean autoNextRightNow, Boolean getLock) {
        current.setOperationRemark(form.getOperationRemark());
        form.setOperationRemark(null);
        //计算流程持续时间
        if (!InstanceOperationType.INITIATE.equals(form.getOperationType())) {
             current.setProcessDuring(getInstanceDuring(form, current.getEndTime()));
        }
        //spring-data-neo4j复杂对象作为参数，需转成map，LocalDateTime、LocalDate会被转成数组，需手动处理
        Map<String, Object> cMap = JacksonUtils.objToMap(current);
        Map<String, Object> nMap = JacksonUtils.objToMap(next);
        updateMapReady(current, next, cMap, nMap);
        Integer flowStatus = getFlowStatus(current, next);
        Long nextId;

        log.info("更新流程状态：流程 {}-版本 {}-key {}", form.getProcessName(), form.getVersion(), form.getBusinessKey());
        if (form.getNum() < 5) {
            nextId = instanceNodeRepository.updateFlowInstance(form.getProcessName(), form.getVersion(),
                    form.getNodeId(), form.getBusinessKey(), form.getCondition(), flowStatus, form.getListData(), form.getVariableData(), cMap, nMap);
        } else {
            nextId = instanceNodeRepository.updateFlowInstanceTooLong(form.getProcessName(), form.getVersion(),
                    form.getNodeId(), form.getNum(), form.getBusinessKey(), form.getCondition(), flowStatus, form.getListData(), form.getVariableData(), cMap, nMap);
        }
        log.info("流程状态更新：流程 {}-版本 {}-key {}", form.getProcessName(), form.getVersion(), form.getBusinessKey());

        //删除实例操作历史缓存i_o_h
        cacheManager.deleteCache(CacheEnums.I_O_H.getType(), List.of(form.getBusinessKey(), cacheManager.mergeKey(form.getBusinessKey(), form.getNum().toString())));

        if (next != null && nextId != null) {
            next.setId(nextId);
            return new UpdateResult(form, next, autoNextRightNow, getLock);
        } else {
            return new UpdateResult(form, null, false, getLock);
        }
    }

    /**
     * 更新前准备参数Map
     * @param current 当前实例节点
     * @param next 下一个实例节点
     * @param cMap 当前实例节点map
     * @param nMap 下一个实例节点map
     */
    public void updateMapReady(InstanceNode current, InstanceNode next, Map<String, Object> cMap, Map<String, Object> nMap) {
        if (cMap != null) {
            cMap.replace("beginTime", current.getBeginTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (current.getEndTime() != null) {
                cMap.replace("endTime", current.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (current.getAutoTime() != null) {
                cMap.replace("autoTime", current.getAutoTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        }

        if (nMap != null) {
            nMap.replace("beginTime", next.getBeginTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (next.getAutoTime() != null) {
                nMap.replace("autoTime", next.getAutoTime().format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        }
    }

    /**
     * 通过后更新流程
     * @param form 表单
     * @param current 当前实例节点
     * @param getLock 当前是否加锁，用于初始化UpdateResult，在需要立即执行下一步时，告知下一步是否需要获取锁
     * @return UpdateResult
     */
    private UpdateResult updateFlowAfterPass(ExecuteForm form, InstanceNode current, Boolean getLock) {
        //设置结束时间、实际操作人、状态
        current.setEndTime(LocalDateTime.now());
        current.setOperationBy(JacksonUtils.toJson(form.getOperator()));
        current.setStatus(operateTypeToNodeStatus(form.getOperationType()));
        current.setDuring(getDuring(Duration.between(current.getBeginTime(), current.getEndTime())));

        //查询下一模型节点
        InstanceNode next = null;
        boolean autoNextRightNow = false;
        if (current.getLocation() <= NodeLocationType.MIDDLE) {
            ModelNode modelNode = findNextModelNode(form, current.getModelNodeUid());
            if (modelNode == null) {
                log.error("流程执行失败，未找到下一节点：流程 {}-版本 {}-key {}-当前节点位置 {}",
                        form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
                throw new NeoExecuteException("流程执行失败，未找到下一节点");
            }
            autoNextRightNow = Objects.equals(modelNode.getAutoInterval(), 0);
            next = constructInstanceNode(modelNode, form);
        }

        //更新流程
        return updateInstance(current, next, form, autoNextRightNow, getLock);
    }

    /**
     * 拒绝后更新流程
     * @param form 表单
     * @param current 当前实例节点
     * @param getLock 当前是否加锁，用于初始化UpdateResult，在需要立即执行下一步时，告知下一步是否需要获取锁
     * @return UpdateResult
     */
    private UpdateResult updateFlowAfterReject(ExecuteForm form, InstanceNode current, Boolean getLock) {
        ModelNode terminateNode = null;
        //拒绝时带有跳转条件，下一个节点不为终止节点且当前不为发起节点，执行通过逻辑
        if (form.getCondition() != null) {
            terminateNode = findNextModelNode(form, current.getModelNodeUid());
            if (terminateNode != null && !NodeLocationType.TERMINATE.equals(terminateNode.getLocation())) {
                return updateFlowAfterPass(form, current, getLock);
            }
        }

        //判断节点能否拒绝、获取模型终止节点
        if (terminateNode == null) {
            terminateNode = getModelTerminateNode(form, current);
        }

        //设置结束时间、实际操作人、状态
        current.setEndTime(LocalDateTime.now());
        current.setOperationBy(JacksonUtils.toJson(form.getOperator()));
        current.setStatus(operateTypeToNodeStatus(form.getOperationType()));
        current.setDuring(getDuring(Duration.between(current.getBeginTime(), current.getEndTime())));
        //构建下一个实例节点
        InstanceNode next;
        boolean autoNextRightNow;
        if (canCycle(form)) {
            //获取开始节点实例，退回发起人
            next = getInstanceInitiateNode(form);
            next.setOperationType(config.getInitiatorFlag());
            autoNextRightNow = false;
        }else {
            //结束流程
            next = constructInstanceNode(terminateNode, form);
            autoNextRightNow = Objects.equals(terminateNode.getAutoInterval(), 0);
        }

        //更新流程
        return updateInstance(current, next, form, autoNextRightNow, getLock);
    }

    /**
     * 转发后更新流程
     * @param form 表单
     * @param current 当前实例节点
     * @param getLock 当前是否加锁，用于初始化UpdateResult，在需要立即执行下一步时，告知下一步是否需要获取锁
     * @return UpdateResult
     */
    private UpdateResult updateFlowAfterForward(ExecuteForm form, InstanceNode current, Boolean getLock) {
        LocalDateTime time = LocalDateTime.now();
        //设置结束时间、实际操作人、状态
        current.setEndTime(time);
        current.setOperationBy(JacksonUtils.toJson(form.getOperator()));
        current.setStatus(operateTypeToNodeStatus(form.getOperationType()));
        current.setDuring(getDuring(Duration.between(current.getBeginTime(), current.getEndTime())));
        //构建下一个实例节点
        InstanceNode next = new InstanceNode();
        BeanUtils.copyProperties(current, next, current.ignoreCopyPropertyListOfForward());
        next.setStatus(InstanceNodeStatus.PENDING);
        next.setBeginTime(time);
        next.setOperationType(form.getForwardOperationType());
        next.setOperationCandidate(JacksonUtils.toJson(form.getForwardOperator()));

        //更新流程
        return updateInstance(current, next, form, false, getLock);
    }

    /**
     * 终止后更新流程
     * @param form 表单
     * @param current 当前实例节点
     * @param getLock 当前是否加锁，用于初始化UpdateResult，在需要立即执行下一步时，告知下一步是否需要获取锁
     * @return UpdateResult
     */
    private UpdateResult updateFlowAfterTerminate(ExecuteForm form, InstanceNode current, Boolean getLock) {
        //设置结束时间、实际操作人、状态
        current.setEndTime(LocalDateTime.now());
        current.setOperationBy(JacksonUtils.toJson(form.getOperator()));
        current.setStatus(operateTypeToNodeStatus(form.getOperationType()));
        current.setDuring(getDuring(Duration.between(current.getBeginTime(), current.getEndTime())));

        //更新
        return updateInstance(current, null, form, false, getLock);
    }

    /**
     * 能否发起流程（businessKey去重）
     * @param businessKey 业务key
     */
    private void canInitiate(String businessKey) {
        Boolean exist;
        //缓存
        String cacheType = CacheEnums.F_I_E.getType();
        NeoCacheManager.CacheValue<Boolean> cache = cacheManager.getCache(cacheType, businessKey, Boolean.class);
        if (cache.filter() || cache.value() != null) {
            exist = Objects.equals(cache.value(), true);
        } else {
            exist = instanceNodeRepository.instanceIsExists(businessKey);
        }

        if (exist) {
            //存在才加入缓存
            cacheManager.setCache(cacheType, businessKey, exist);
            log.error("流程执行失败，业务key已存在，请勿重复发起：{}", businessKey);
            throw new NeoExecuteException("流程执行失败，业务key已存在, 请勿重复发起");
        }
    }

    /**
     * 获取实例节点
     * @param form 表单
     * @return InstanceNode
     */
    private NodeQueryDto<InstanceNode> getInstanceNode(ExecuteForm form) {
        NodeQueryDto<InstanceNode> dto;
        InstanceNode currentNode;
        if (InstanceOperationType.INITIATE.equals(form.getOperationType())) {
            NodeQueryDto<ModelNode> modelDto = findActiveVersionModelFirstNode(form.getProcessName());
            currentNode = constructInstanceNode(JacksonUtils.toObj(modelDto.getNodeJson(), ModelNode.class), form);
            dto = new NodeQueryDto<>();
            dto.setNode(currentNode);
            form.setOperationMethod(currentNode.getOperationMethod());
            form.setNodeId(currentNode.getId());
            form.setVersion(modelDto.getVersion());
        }else {
            dto =queryCurrentInstanceNode(form);
        }

        return dto;
    }

    /**
     * 查询当前实例节点
     * @param form 表单
     * @return 查询结果
     */
    private NodeQueryDto<InstanceNode> queryCurrentInstanceNode(ExecuteForm form) {
        NodeQueryDto<InstanceNode> dto;
        if (form.getNum() < 5) {
            dto = instanceNodeRepository.queryCurrentInstanceNode(form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNodeId());
        }else {
            dto = instanceNodeRepository.queryCurrentInstanceNodeTooLong(form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNodeId(), form.getNum());
        }

        //查询结果校验
        if (dto == null || StringUtils.isBlank(dto.getNodeJson())) {
            log.error("流程执行失败，未找到当前实例节点：流程 {}-位置 {}-nodeId {}", form.getProcessName(), form.getNum(), form.getNodeId());
            throw new NeoExecuteException("流程执行失败，未找到当前实例节点");
        }
        //前置节点校验
        if (dto.getBefore() == null) {
            log.error("流程执行失败，当前流程实例节点未找到前置节点：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程执行失败，当前流程实例节点未找到指定前置节点");
        }
        if (!dto.getBefore()) {
            log.error("流程执行失败，前置节点未执行：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程执行失败，前置节点未执行");
        }

        dto.setNode(JacksonUtils.toObj(dto.getNodeJson(), InstanceNode.class));

        //校验当前节点
        if (!InstanceNodeStatus.PENDING.equals(dto.getNode().getStatus())) {
            log.error("流程执行失败，当前节点已执行：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程执行失败，当前节点已执行");
        }

        form.setOperationMethod(dto.getNode().getOperationMethod());
        form.setVersion(dto.getVersion());

        return dto;
    }

    /**
     * 查询当前流程激活版本的发起节点
     * @param processName 流程名称
     * @return ModelNode
     */
    private NodeQueryDto<ModelNode> findActiveVersionModelFirstNode(String processName) {
        NodeQueryDto<ModelNode> dto = modelNodeRepository.queryActiveVersionModelFirstNode(processName);
        if (dto == null) {
            log.error("流程执行失败，未找到当前流程激活版本的发起节点：{}", processName);
            throw new NeoExecuteException("流程执行失败，未找到当前流程激活版本的发起节点");
        }
        return dto;
    }

    /**
     * 构建实例节点
     * @param modelNode 模型节点
     * @param form 表单
     * @return InstanceNode
     */
    private InstanceNode constructInstanceNode(ModelNode modelNode, ExecuteForm form) {
        //复制关键属性
        InstanceNode instanceNode = new InstanceNode();
        BeanUtils.copyProperties(modelNode, instanceNode,
                modelNode.ignoreCopyPropertyList());
        instanceNode.setModelNodeUid(modelNode.getNodeUid());

        //设置自动执行日期 和 候选人
        Integer autoInterval = modelNode.getAutoInterval();
        if (autoInterval != null) {
            if (NodeLocationType.INITIATE.equals(modelNode.getLocation())) {
                setInstanceNodeCandidateByModel(modelNode.getOperationCandidate(), modelNode.getOperationType(), instanceNode);
            }
            instanceNode.setAutoTime(LocalDate.now().plusDays(autoInterval));
        } else if (Objects.equals(modelNode.getOperationType(), config.getInitiatorFlag())) {
            //发起流程时，发起实例节点还未入库，若下一节点操作类型为"发起人操作"，则手动设置候选人
            if (InstanceOperationType.INITIATE.equals(form.getOperationType())) {
                String candidatesJson = "["+ JacksonUtils.toJson(form.getOperator())+"]";
                instanceNode.setOperationCandidate(candidatesJson);
            } else {
                InstanceNode initiateNode = getInstanceInitiateNode(form);
                instanceNode.setOperationCandidate(initiateNode.getOperationCandidate());
            }
        }else if (StringUtils.isNotBlank(modelNode.getOperationCandidate())){
            setInstanceNodeCandidateByModel(modelNode.getOperationCandidate(), modelNode.getOperationType(), instanceNode);
        }

        //设置状态
        instanceNode.setStatus(InstanceNodeStatus.PENDING);

        //设置时间
        instanceNode.setBeginTime(LocalDateTime.now());

        return instanceNode;
    }

    /**
     * 通过模型设置实例节点候选人
     * @param modelCandidateJson 模型节点候选人json
     * @param modeOperationType 模型节点操作类型
     * @param instanceNode 实例节点
     */
    private void setInstanceNodeCandidateByModel(String modelCandidateJson, Integer modeOperationType , InstanceNode instanceNode) {
        List<UserBaseInfo> candidates = (List<UserBaseInfo>) JacksonUtils.toObj(modelCandidateJson, List.class, UserBaseInfo.class);
        candidates = userChoose.getCandidateUsers(modeOperationType, candidates);
        instanceNode.setOperationCandidate(JacksonUtils.toJson(candidates));
    }

    /**
     * 校验当前用户是否在候选人范围
     * @param form 表单
     * @param instanceNode 实例节点
     * @param isTerminated 是否为终止操作
     */
    private void inCandidate(ExecuteForm form, InstanceNode instanceNode, Boolean isTerminated) {
        String msg;
        String candidateJson;
        List<UserBaseInfo> candidate;
        UserBaseInfo user = form.getOperator();
        //终止流程
        if (isTerminated) {
            //获取当前流程发起节点，只有流程发起人能终止流程
            InstanceNode initNode = getInstanceInitiateNode(form);
            candidateJson = initNode.getOperationCandidate();
            msg = "流程终止失败，不是发起人";
            candidate = (List<UserBaseInfo>) JacksonUtils.toObj(candidateJson, List.class, UserBaseInfo.class);
            if (CollectionUtils.isEmpty(candidate) || candidate.stream().noneMatch(x -> x.equals(user))) {
                log.error("{}：流程 {}-版本 {}-key {}-当前位置{}-当前用户{}", msg,
                        form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum(), user);
                throw new NeoExecuteException(msg);
            }
            return;
        }

        //忽略非发起节点的自动节点
        if (instanceNode.getAutoTime() != null && !NodeLocationType.INITIATE.equals(instanceNode.getLocation())) {
            return;
        }

        msg = "流程执行失败，操作人不在候选人中";
        candidateJson = instanceNode.getOperationCandidate();
        candidate = (List<UserBaseInfo>) JacksonUtils.toObj(candidateJson, List.class, UserBaseInfo.class);
        if (!userChoose.checkCandidateUser(instanceNode.getOperationType(), user, candidate)) {
            log.error("{}：流程 {}-版本 {}-key {}-当前位置{}-当前用户{}", msg,
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum(), user);
            throw new NeoExecuteException(msg);
        }
    }

    /**
     * 查询下一模型节点
     * @param form 表单
     * @param nodeUid 当前模型节点uid
     * @return ModelNode
     */
    private ModelNode findNextModelNode(ExecuteForm form, String nodeUid) {
        String processName = form.getProcessName();
        Integer version = form.getVersion();
        Integer condition = form.getCondition();
        String conditionKey = condition == null ? "null" : condition.toString();

        String cacheType = CacheEnums.N_M_N.getType();
        String key = cacheManager.mergeKey(processName, version.toString(), nodeUid, conditionKey);
        NeoCacheManager.CacheValue<ModelNode> cache = cacheManager.getCache(cacheType, key, ModelNode.class);
        ModelNode modelNode;
        if (cache.filter() || cache.value() != null) {
            modelNode = cache.value();
        } else {
            modelNode = modelNodeRepository.queryNextModelNode(processName, version, nodeUid, condition);
            cacheManager.setCache(cacheType, key, modelNode);
        }

        return modelNode;
    }

    /**
     * 查询当前流程实例发起节点
     * @param form 表单
     * @return InstanceNode
     */
    private InstanceNode getInstanceInitiateNode(ExecuteForm form) {
        InstanceNode initiateNode;
        String cacheType = CacheEnums.I_I_N.getType();
        NeoCacheManager.CacheValue<InstanceNode> cache = cacheManager.getCache(cacheType, form.getBusinessKey(), InstanceNode.class);
        if (cache.filter() || cache.value() != null) {
            initiateNode =  cache.value();
        } else {
            initiateNode = instanceNodeRepository.queryInstanceInitiateNode(form.getProcessName(), form.getVersion(), form.getBusinessKey());
            cacheManager.setCache(cacheType, form.getBusinessKey(), initiateNode);
            cacheManager.setCache(CacheEnums.I_B_T.getType(), form.getBusinessKey(), initiateNode.getBeginTime());
        }

        if (initiateNode == null) {
            log.error("未找到当前流程实例发起节点：{}", form.getBusinessKey());
            throw new NeoExecuteException("未找到当前流程实例发起节点");
        }

        initiateNode.setBeginTime(LocalDateTime.now());

        return initiateNode;
    }

    /**
     * 获取模型终止节点
     * @param form 表单
     * @param current 当前流程实例节点
     * @return ModelNode
     */
    private ModelNode getModelTerminateNode(ExecuteForm form, InstanceNode current) {
        //中间节点，判断节点是否与终止节点相连
        if (NodeLocationType.MIDDLE.equals(current.getLocation())) {
            //缓存-能否拒绝
            String cacheType = CacheEnums.M_C_T.getType();
            String key = cacheManager.mergeKey(form.getProcessName(), form.getVersion().toString(), current.getModelNodeUid());
            NeoCacheManager.CacheValue<Boolean> cache = cacheManager.getCache(cacheType, key, Boolean.class);
            if (cache.filter() || cache.value() != null) {
                if (Objects.equals(cache.value(), true)) {
                    log.error("流程拒绝失败，当前节点不能拒绝：流程 {}-版本 {}-key {}- 当前节点位置{}",
                            form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
                    throw new NeoExecuteException("流程拒绝失败，当前节点不能拒绝");
                }
            }
            //缓存-模型终止节点
            key = cacheManager.mergeKey(form.getProcessName(), form.getVersion().toString());
            NeoCacheManager.CacheValue<ModelNode> ct = cacheManager.getCache(cacheType, key, ModelNode.class);
            ModelNode terminateNode;
            if (ct.filter() || ct.value() != null) {
                terminateNode = ct.value();
            }else {
                terminateNode = modelNodeRepository.MiddleNodeCanReject(form.getProcessName(), form.getVersion(), current.getModelNodeUid());
                cacheManager.setCache(cacheType, key, terminateNode);
            }
            //当前节点没与终止节点相连
            if (terminateNode == null) {
                cacheManager.setCache(cacheType, key, false);
                log.error("流程拒绝失败，当前节点不能拒绝：流程 {}-版本 {}-key {}- 当前节点位置{}",
                        form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
                throw new NeoExecuteException("流程拒绝失败，当前节点不能拒绝");
            }

            cacheManager.setCache(cacheType, key, true);
            return terminateNode;
        }

        //终止节点
        if (NodeLocationType.TERMINATE.equals(current.getLocation())) {
            log.error("流程拒绝失败，终止节点不能再拒绝：流程 {}-版本 {}-key {}- 当前节点位置{}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程拒绝失败，终止节点不能再拒绝");
        }

        //发起、完成 节点
        String cacheType = CacheEnums.M_T_N.getType();
        String key = cacheManager.mergeKey(form.getProcessName(), form.getVersion().toString());
        NeoCacheManager.CacheValue<ModelNode> ct = cacheManager.getCache(cacheType, key, ModelNode.class);
        ModelNode terminateNode;
        if (ct.filter() || ct.value() != null) {
            terminateNode = ct.value();
        }else {
            terminateNode = modelNodeRepository.queryModelTerminateNode(form.getProcessName(), form.getVersion());
            cacheManager.setCache(cacheType, key, terminateNode);
        }

        if (terminateNode == null) {
            cacheManager.setCache(cacheType, key, false);
            log.error("流程拒绝失败，未找到终止节点：流程 {}-版本 {}-key {}- 当前节点位置{}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程拒绝失败，未找到终止节点");
        }

        return terminateNode;
    }

    /**
     * 获取时间间隔
     * @param duration Duration
     * @return xDxHxMxS，xHxMxS，xMxS，xS
     */
    private String getDuring(Duration duration) {
        StringBuilder sb = new StringBuilder();
        long value =duration.toDaysPart();
        if (value >= 1L) {
            sb.append(duration.toDays()).append(DURING_CHAR[0]);
        }
       value = duration.toHoursPart();
        if (value >= 1L) {
            sb.append(value).append(DURING_CHAR[1]);
        }
        value = duration.toMinutesPart();
        if (value >= 1L) {
            sb.append(value).append(DURING_CHAR[2]);
        }
        value = duration.toSecondsPart();
        sb.append(value).append(DURING_CHAR[3]);
        return sb.toString();
    }

    /**
     * 获取流程实例开始时间
     * @param form 表单
     * @param endTime 流程实例结束时间
     * @return xDxHxMxS，xHxMxS，xMxS，xS
     */
    private String getInstanceDuring(ExecuteForm form, LocalDateTime endTime) {
        String cacheType = CacheEnums.I_B_T.getType();
        String processName = form.getProcessName();
        Integer version = form.getVersion();
        String businessKey = form.getBusinessKey();
        NeoCacheManager.CacheValue<LocalDateTime> cache = cacheManager.getCache(cacheType, businessKey, LocalDateTime.class);
        LocalDateTime instanceBeginTime;
        if (cache.filter() || cache.value() != null) {
            instanceBeginTime = cache.value();
        }else {
            instanceBeginTime = instanceNodeRepository.queryInstanceBeginTime(processName, version, businessKey);
            cacheManager.setCache(cacheType, businessKey, instanceBeginTime);
        }

        if (instanceBeginTime == null) {
            log.error("未查询到流程开始时间：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    processName, version, businessKey, form.getNum());
            throw new NeoExecuteException("未查询到流程开始时间");
        }

        return getDuring(Duration.between(instanceBeginTime, endTime));
    }

    /**
     * 能否退回到发起人进行循环
     * @param form 表单
     * @return Boolean
     */
    private Boolean canCycle(ExecuteForm form) {
        String key = form.getBusinessKey();
        String cacheType = CacheEnums.F_I_C.getType();
        NeoCacheManager.CacheValue<Boolean> cache = cacheManager.getCache(cacheType, key, Boolean.class);
        if (cache.filter() || cache.value() != null) {
            return Objects.equals(cache.value(), true);
        }
        Boolean can = instanceNodeRepository.canCycle(form.getProcessName(), form.getVersion(), form.getBusinessKey());
        if (!can) {
            cacheManager.setCache(cacheType, key, false);
        }
        return can;
    }

    /**
     * 操作类型转成执行后的节点状态
     * @param operationType 操作类型
     * @return 节点状态
     */
    private Integer operateTypeToNodeStatus(Integer operationType) {
        if (InstanceOperationType.REJECTED.equals(operationType)) {
            return InstanceNodeStatus.REJECTED;
        }
        if (InstanceOperationType.FORWARD.equals(operationType)) {
            return InstanceNodeStatus.FORWARD;
        }
        if (InstanceOperationType.TERMINATED.equals(operationType)) {
            return InstanceNodeStatus.TERMINATED;
        }

        return InstanceNodeStatus.PASS;
    }

    /**
     * 获取更新的流程实例状态
     * @param current 当前流程实例节点
     * @param next 下一个实例节点
     * @return 流程实例状态
     */
    private Integer getFlowStatus(InstanceNode current, InstanceNode next) {
        if (next == null) {
          if (NodeLocationType.COMPLETE.equals(current.getLocation())) {
              return InstanceStatus.COMPLETE;
          }
          if (NodeLocationType.TERMINATE.equals(current.getLocation())) {
              return InstanceStatus.REJECTED;
          }
          return InstanceStatus.TERMINATED;
        }

        return InstanceStatus.PENDING;
    }

    /**
     * 构建下一个自动节点的表单
     * @param result 当前节点更新结果
     * @return 下一个自动节点的表单
     */
    private ExecuteForm autoNextForm(UpdateResult result) {
        ExecuteForm currentForm = result.form();
        int num = currentForm.getNum() + 1;
        long nodeId = result.next().getId();

        ExecuteForm form = new ExecuteForm();
        form.setProcessName(currentForm.getProcessName())
                .setBusinessKey(currentForm.getBusinessKey())
                .setVersion(currentForm.getVersion())
                .setNum(num)
                .setNodeId(nodeId)
                .setOperator(currentForm.getOperator())
                .setOperator(systemOperator)
                .setOperationType(InstanceOperationType.PASS);

        return form;
    }

    /**
     * 获取移植版本模型节点
     * @param form 表单
     */
    private NodeQueryDto<ModelNode> graftVersionModelNode(GraftForm form) {
        NeoCacheManager.CacheValue<NodeQueryDto> cacheValue;
        String cacheType;
        String cacheKey;
        NodeQueryDto<ModelNode> modelDto;
        String errorLog;
        //通过nodeUid移植
        String modeNodeUid = form.getGraftNodeUid();
        cacheType = CacheEnums.N_M_N.getType();
        cacheKey = cacheManager.mergeKey(form.getProcessName(), form.getGraftVersion().toString(), modeNodeUid);
        cacheValue = cacheManager.getCache(cacheType, cacheKey, NodeQueryDto.class);
        if (cacheValue.filter() || cacheValue.value() != null) {
            modelDto = (NodeQueryDto<ModelNode>) cacheValue.value();
        }else {
            modelDto = modelNodeRepository.queryModelNode(form.getProcessName(), form.getGraftVersion(), modeNodeUid);
            cacheManager.setCache(cacheType, cacheKey, modelDto);
        }
        errorLog = String.format("流程执行失败，未找到移植节点：流程 %s-版本 %s -key %s -当前节点位置 %s -移植版本 %s-移植节点uid %s",
                form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum(), form.getGraftVersion(), modeNodeUid);

        if (modelDto == null || StringUtils.isBlank(modelDto.getNodeJson())) {
            log.error(errorLog);
            throw new NeoExecuteException("流程执行失败，未找到移植节点");
        }

        modelDto.setNode(JacksonUtils.toObj(modelDto.getNodeJson(), ModelNode.class));

        return modelDto;
    }

    /**
     * 扫描自动节点
     * @param date 自动执行日期
     */
    public void autoScan(LocalDate date){
        date = date == null ? LocalDate.now() : date;
        log.info("开始扫描自动节点 {}", date);
        autoDeal(date);
    }

    /**
     * 扫描自动节点
     */
    @Scheduled(cron = "${neo.auto-corn:0 0 5 * * ?}")
    public void autoScan(){
        if (config.getScanAuto()) {
            LocalDate date = LocalDate.now();
            log.info("开始扫描自动节点 {}", date);
            autoDeal(date);
        }
    }

    /**
     * 扫描执行自动节点锁过期监控
     * @param getLock 是否获取锁
     * @param assignedPendingFuture 执行中的分配节点数量监控Future
     * @return Future
     */
    private Future<?> autoLockMonitor(AtomicBoolean getLock, AtomicReference<Future<?>> assignedPendingFuture) {

        Integer expired = config.getAutoLockExpired();

        if (expired != null && expired > 0) {
            return AUTO_LOCK_EXPIRED_EXECUTOR.schedule(() -> {
                if (getLock.getAndSet(false)) {
                    log.info("扫描执行自动节点超时，释放锁");
                    lockManager.releaseLock("ea", getLock.get(), LockEnums.AUTO_EXECUTE);
                    // 取消AUTO_LOCK_CHECK_EXECUTOR此次的任务
                    Future<?> future = assignedPendingFuture.get();
                    if (future != null && !future.isDone()) {
                        log.info("取消执行中的自动节点数量检测");
                        future.cancel(true);
                    }
                }
            }, expired, TimeUnit.SECONDS);
        }

        return null;
    }

    /**
     * 正在执行的分配节点数量监控
     * 采用这种方式是防止频繁的判断未执行的分配节点数量
     * @param getLock 是否获取锁
     * @param assignedFlag 是否开始分配
     * @param assignedPendingFuture 执行中的分配节点数量监控Future
     * @param autoLockFuture 锁过期监控Future
     * @return Future
     */
    private Future<?> assignedPendingMonitor(AtomicBoolean getLock, AtomicBoolean assignedFlag , AtomicReference<Future<?>> assignedPendingFuture, AtomicReference<Future<?>> autoLockFuture) {
        return AUTO_LOCK_CHECK_EXECUTOR.scheduleAtFixedRate(() -> {
            log.info("执行中的自动节点数量检测，是否开始分配 {}，当前数量 {}", assignedFlag.get(), ASSIGNED_PENDING_COUNT.get());
            if (getLock.get() && assignedFlag.get() && ASSIGNED_PENDING_COUNT.get() == 0) {
                log.info("扫描执行自动节点结束，释放锁");
                lockManager.releaseLock("ea", getLock.get(), LockEnums.AUTO_EXECUTE);
                // 取消AUTO_LOCK_EXPIRED_EXECUTOR此次的任务
                Future<?> future = autoLockFuture.get();
                if (future != null && !future.isDone()) {
                    log.info("取消锁过期检测");
                    future.cancel(true);
                }
                //取消自身的任务
                future = assignedPendingFuture.get();
                if (future != null) {
                    log.info("取消执行中的自动节点数量检测");
                    future.cancel(true);
                }
            }
        }, config.getAutoLockCheckPeriod(), config.getAutoLockCheckPeriod(), TimeUnit.SECONDS);
    }

    /**
     * 处理自动节点
     * @param date 自动执行日期
     */
    private void autoDeal(LocalDate date) {
        //获取锁
        AtomicBoolean getLock = new AtomicBoolean(lockManager.getLock("ea", LockEnums.AUTO_EXECUTE));
        ASSIGNED_PENDING_COUNT.set(0);
        //是否开始分配
        AtomicBoolean assignedFlag = new AtomicBoolean();
        AtomicReference<Future<?>> assignedPendingFuture = new AtomicReference<>();
        AtomicReference<Future<?>> autoLockFuture = new AtomicReference<>();
        try {
            //启动定时线程池任务
            assignedPendingFuture.set(assignedPendingMonitor(getLock, assignedFlag, assignedPendingFuture, autoLockFuture));
            autoLockFuture.set(autoLockMonitor(getLock, assignedPendingFuture));
            //扫描
            List<AutoNodeDto> autoNodes;
            if (config.getAutoType() == AutoType.TODAY) {
                autoNodes = instanceNodeRepository.queryAutoNodeToDay(date);
            } else {
                autoNodes = instanceNodeRepository.queryAutoNodeToDayAndBefore(date);
            }

            int size = autoNodes.size();
            if (size > 0) {
                //分配到autoNodeExecutor
                assigned(autoNodes, size, assignedFlag);
            }else {
                stopScheduledAndReleaseAutoLock(assignedPendingFuture, autoLockFuture, getLock);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            stopScheduledAndReleaseAutoLock(assignedPendingFuture, autoLockFuture, getLock);
        }

    }

    /**
     * 停止定时线程任务以及是否扫描执行自动节点的锁
     * @param assignedPendingFuture 执行中的分配节点数量监控Future
     * @param autoLockFuture 锁过期监控Future
     * @param getLock 是否获取锁
     */
    private void stopScheduledAndReleaseAutoLock(AtomicReference<Future<?>> assignedPendingFuture, AtomicReference<Future<?>> autoLockFuture, AtomicBoolean getLock) {
        Future<?> autoLock = autoLockFuture.get();
        Future<?> assignedPending = assignedPendingFuture.get();
        if (autoLock != null && !autoLock.isDone()) {
            autoLock.cancel(true);
        }
        if (assignedPending != null && !assignedPending.isDone()) {
            assignedPending.cancel(true);
        }
        lockManager.releaseLock("ea", getLock.get(), LockEnums.AUTO_EXECUTE);
    }

    /**
     * 分配自动节点
     * @param autoNodes 自动节点
     * @param size 节点数量
     * @param assignedFlag 是否开始分配
     */
    private void assigned(List<AutoNodeDto> autoNodes, Integer size, AtomicBoolean assignedFlag) {
        int assigned;
        if (size == 0) {
            return;
        }else if (size % config.getAutoAssigned() == 0) {
            assigned = size / config.getAutoAssigned();
        }else {
            assigned = size / config.getAutoAssigned() + 1;
        }

        //已分配的节点数量
        for (int i = 0; i < assigned; i++) {
            List<AutoNodeDto> subList;
            int start = i * config.getAutoAssigned();
            int end = (i + 1) * config.getAutoAssigned();
            if (end <= size) {
                subList = autoNodes.subList(start, end);
            } else {
                subList = autoNodes.subList(start, size);
            }

            autoNodeExecutor.execute(()-> {
                assignedFlag.compareAndSet(false, true);
                ASSIGNED_PENDING_COUNT.addAndGet(subList.size());
                for (AutoNodeDto autoNode : subList) {
                    autoExecute(autoNode);
                }
            });
        }
    }

    /**
     * 构建自动节点表单
     * @param autoNode 自动节点
     * @return ExecuteForm
     */
    private ExecuteForm autoForm(AutoNodeDto autoNode) {
        return new ExecuteForm()
                .setProcessName(autoNode.processName())
                .setVersion(autoNode.version())
                .setBusinessKey(autoNode.businessKey())
                .setNum(autoNode.location())
                .setNodeId(autoNode.nodeId())
                .setOperationMethod(autoNode.operationMethod())
                .setOperationType(InstanceOperationType.PASS)
                .setOperator(systemOperator);
    }

    /**
     * 构建当前自动实例节点
     * @param autoNodeDto 自动节点
     * @return InstanceNode
     */
    private InstanceNode autoCurrentNode(AutoNodeDto autoNodeDto) {
        InstanceNode current = new InstanceNode();
        current.setId(autoNodeDto.nodeId());
        current.setModelNodeUid(autoNodeDto.modelNodeUid());
        current.setStatus(InstanceNodeStatus.PASS);
        current.setOnlyPassExecute(true);
        current.setOperationBy(JacksonUtils.toJson(systemOperator));
        current.setDefaultPassCondition(autoNodeDto.defaultPassCondition());
        current.setLocation(autoNodeDto.location());
        current.setBeginTime(autoNodeDto.beginTime());
        current.setEndTime(LocalDateTime.now());
        current.setDuring(getDuring(Duration.between(current.getBeginTime(), current.getEndTime())));
        return current;
    }

    /**
     * 自动节点更新流程实例
     * @param autoNodeDto 自动节点
     * @return UpdateResult
     */
    private UpdateResult updateAfterAuto(AutoNodeDto autoNodeDto) {
        //获取锁
        boolean getLock = lockManager.getLock(autoNodeDto.businessKey(), LockEnums.FLOW_EXECUTE);
        String businessKey = autoNodeDto.businessKey();
        boolean autoNextRightNow = false;
        try {
            log.info("开始自动节点：流程{} -版本{}- businessKey{}- 当前节点位置{}",
                    autoNodeDto.processName(), autoNodeDto.version(), businessKey, autoNodeDto.num());
            ExecuteForm form = autoForm(autoNodeDto);
            InstanceNode current = autoCurrentNode(autoNodeDto);
            operateMethod(form, current,null,false);
            UpdateResult updateResult = updateFlowAfterPass(form, current, getLock);
            autoNextRightNow = updateResult.autoRightNow();
            log.info("完成自动节点：流程{} -版本{}- businessKey{}- 当前节点位置{}",
                    autoNodeDto.processName(), autoNodeDto.version(), businessKey, autoNodeDto.num());
            return updateResult;
        } catch (Exception e) {
            lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            getLock = false;
            throw e;
        } finally {
            if (!autoNextRightNow) {
                lockManager.releaseLock(businessKey, getLock, LockEnums.FLOW_EXECUTE);
            }
        }
    }

    /**
     * 执行自动节点
     * @param autoNodeDto 自动节点
     */
    private void autoExecute(AutoNodeDto autoNodeDto) {
        //当前自动节点
        UpdateResult updateResult;
        boolean[] result = new boolean[1];
        result[0] = true;
        try {
            updateResult = transactionTemplate.execute(status -> {
                try {
                    return updateAfterAuto(autoNodeDto);
                }catch (Exception e) {
                    status.setRollbackOnly();
                    result[0] = false;
                    throw e;
                }
            });

            rightNowNext(updateResult, autoNodeDto.businessKey());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result[0] = false;
        } finally {
            int i = ASSIGNED_PENDING_COUNT.decrementAndGet();
            log.info("自动节点已处理，结果：{}，[businessKey {}- num {}- nodeId {}]，剩余 {}", result[0], autoNodeDto.businessKey(), autoNodeDto.num(), autoNodeDto.nodeId(), i);
        }
    }

}
