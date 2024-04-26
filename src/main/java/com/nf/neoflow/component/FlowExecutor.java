package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.constants.*;
import com.nf.neoflow.dto.execute.ExecuteForm;
import com.nf.neoflow.dto.execute.NodeQueryDto;
import com.nf.neoflow.dto.execute.UpdateResult;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.enums.LockEnums;
import com.nf.neoflow.exception.NeoExecuteException;
import com.nf.neoflow.exception.NeoFlowConfigException;
import com.nf.neoflow.models.InstanceNode;
import com.nf.neoflow.models.ModelNode;
import com.nf.neoflow.repository.InstanceNodeRepository;
import com.nf.neoflow.repository.ModelNodeRepository;
import com.nf.neoflow.utils.JacksonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
            InstanceOperationType.REJECTED, this::reject
    );

    private ThreadPoolExecutor autoNodeExecutor;

    @PostConstruct
    private void autoNodeExecutorInit() {
        BlockingQueue queue;
        switch (config.getQueueType().toLowerCase()) {
            case "array" -> queue = new java.util.concurrent.ArrayBlockingQueue<>(config.getQueueCapacity());
            case "linked" -> queue = new java.util.concurrent.LinkedBlockingQueue<>(config.getQueueCapacity());
            case "synchronous" -> queue = new java.util.concurrent.SynchronousQueue<>(false);
            case "priority" -> queue = new java.util.concurrent.PriorityBlockingQueue<>(config.getQueueCapacity());
            default -> throw new NeoFlowConfigException(String.format("队列类型错误，%s", config.getQueueType()));
        }

        autoNodeExecutor = new ThreadPoolExecutor(
                config.getCorePoolSize(),
                config.getMaxPoolSize(),
                config.getKeepAliveTime(), TimeUnit.SECONDS,
                queue
        );

        switch (config.getRejectionPolicy().toLowerCase()) {
            case "abort" -> autoNodeExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
            case "caller-runs" -> autoNodeExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            case "discard" -> autoNodeExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
            case "discard-oldest" -> autoNodeExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
            default -> throw new NeoFlowConfigException(String.format("拒绝策略类型错误，%s", config.getRejectionPolicy()));
        }
    }

    /**
     * 执行器
     * @param form 表单
     * @param getLockByLast 是否在上个节点获取锁
     */
    public void executor(ExecuteForm form, Boolean getLockByLast) {
        int operationType = form.getOperationType();
        BiFunction<ExecuteForm, Boolean, UpdateResult> function = excuteMap.get(operationType);
        if (function == null) {
            log.error("错误的操作类型-{}", operationType);
            throw new NeoExecuteException("错误的操作类型");
        }

        //执行当前节点
        UpdateResult updateResult = transactionTemplate.execute(status -> {
            try {
                return function.apply(form, getLockByLast);
            }catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });

        //按需自动执行下一节点
        if (updateResult.autoRigNow()) {
            ExecuteForm nextForm = autoNextForm(updateResult);
            autoNodeExecutor.execute(() -> {
                transactionTemplate.execute(status -> {
                    try {
                        executor(nextForm, updateResult.getLock());
                    }catch (Exception e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                    return null;
                });
            });
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
                getLock = getLockByLast;
            }

            //获取/校验当前用户信息
            form.setOperator(userChoose.user(form.getOperator())).setNum(1);
            //获取实例节点
            NodeQueryDto<InstanceNode> dto = getInstanceNode(form);
            //执行
            operateMethod(form, dto.getNode());
            //若发起时未加锁，此时获得businessKey后加锁
            if (!getLock) {
                getLock = lockManager.getLock(form.getBusinessKey(), LockEnums.FLOW_EXECUTE);
            }
            //判断businessKey是否已存在
            canInitiate(form.getBusinessKey());
            //更新流程
            UpdateResult ur = updateFlowAfterPass(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRigNow();

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

            //获取/校验当前用户信息
            form.setOperator(userChoose.user(form.getOperator()));
            //获取实例节点
            NodeQueryDto<InstanceNode> dto = getInstanceNode(form);
            //执行
            operateMethod(form, dto.getNode());
            //更新流程
            UpdateResult ur = updateFlowAfterPass(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRigNow();

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

            //获取/校验当前用户信息
            form.setOperator(userChoose.user(form.getOperator()));
            //获取实例节点
            NodeQueryDto<InstanceNode> dto = getInstanceNode(form);
            //执行
            operateMethod(form, dto.getNode());

            //更新流程
            UpdateResult ur = updateFlowAfterReject(form, dto.getNode(), getLock);
            autoNextRightNow = ur.autoRigNow();

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
     * 执行节点方法
     * @param form 表单
     * @param current 当前实例节点
     */
    private void operateMethod(ExecuteForm form, InstanceNode current) {
        //判断候选人身份
        inCandidate(form, current);

        log.info("流程操作类型：{}", form.getOperationType());

        if (needOperateMethod(form.getOperationType(), current.getOnlyPassExecute())) {
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
        }

        //发起、通过 必须有跳转条件
        if (form.getOperationType() < InstanceOperationType.REJECTED && current.getLocation() <= NodeLocationType.MIDDLE && form.getCondition() == null) {
            log.error("流程执行失败，缺失跳转条件：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程执行失败，缺失跳转条件");
        }
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
    public UpdateResult updateInstance(InstanceNode current, InstanceNode next, ExecuteForm form,
                                       Boolean autoNextRightNow, Boolean getLock) {
        Map<String, Object> cMap = JacksonUtils.objToMap(current);
        Map<String, Object> nMap = JacksonUtils.objToMap(next);
        Integer flowStatus = getFlowStatus(current, next);
        Long nextId;

        log.info("更新流程状态：流程 {}-版本 {}-key {}", form.getProcessName(), form.getVersion(), form.getBusinessKey());
        if (form.getNum() < 5) {
            nextId = instanceNodeRepository.updateFlowInstance(form.getProcessName(), form.getVersion(),
                    form.getNodeId(), form.getBusinessKey(), form.getCondition(), flowStatus, cMap, nMap);
        } else {
            nextId = instanceNodeRepository.updateFlowInstanceTooLong(form.getProcessName(), form.getVersion(),
                    form.getNodeId(), form.getNum(), form.getBusinessKey(), form.getCondition(), flowStatus, cMap, nMap);
        }
        log.info("流程状态更新：流程 {}-版本 {}-key {}", form.getProcessName(), form.getVersion(), form.getBusinessKey());

        if (next != null && nextId != null) {
            next.setId(nextId);
            return new UpdateResult(form, next, autoNextRightNow, getLock);
        } else {
            return new UpdateResult(form, null, false, getLock);
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
        current.setDuring(Duration.between(current.getBeginTime(), current.getEndTime()).getSeconds());

        //查询下一模型节点
        InstanceNode next = null;
        boolean autoNextRightNow = false;
        if (current.getLocation() <= NodeLocationType.MIDDLE) {
            ModelNode modelNode = findNextModelNode(form, current.getModelNodeUid());
            autoNextRightNow = Objects.equals(modelNode.getAutoInterval(), 0);
            next = constructInstanceNode(modelNode);
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
        //拒绝时带有跳转条件，且下一个节点不为终止节点，执行通过逻辑
        ModelNode terminateNode = null;
        if (form.getCondition() != null) {
            terminateNode = findNextModelNode(form, current.getModelNodeUid());
            if (!Objects.equals(terminateNode.getLocation(), NodeLocationType.TERMINATE)) {
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
        current.setDuring(Duration.between(current.getBeginTime(), current.getEndTime()).getSeconds());
        //构建下一个实例节点
        InstanceNode next;
        boolean autoNextRightNow;
        if (canCycle(form)) {
            //获取开始节点实例，退回发起人
            next = getInstanceInitiateNodeToRegression(form);
            autoNextRightNow = false;
        }else {
            //结束流程
            next = constructInstanceNode(terminateNode);
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
        return null;
    }

    /**
     * 终止后更新流程
     * @param form 表单
     * @param current 当前实例节点
     * @param getLock 当前是否加锁，用于初始化UpdateResult，在需要立即执行下一步时，告知下一步是否需要获取锁
     * @return UpdateResult
     */
    private UpdateResult updateFlowAfterTerminate(ExecuteForm form, InstanceNode current, Boolean getLock) {
        return null;
    }

    /**
     * 能否发起流程（businessKey去重）
     * @param businessKey 业务key
     */
    private void canInitiate(String businessKey) {
        Boolean exist;
        //缓存
        NeoCacheManager.CacheValue<Boolean> cache = cacheManager.getCache(CacheType.F_I_E, businessKey, Boolean.class);
        if (cache.filter() || cache.value() != null) {
            exist = Objects.equals(cache.value(), true);
        } else {
            exist = instanceNodeRepository.instanceIsExists(businessKey);
        }

        if (exist) {
            //存在才加入缓存
            cacheManager.setCache(CacheType.F_I_E, businessKey, exist);
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
        if (Objects.equals(form.getOperationType(), InstanceOperationType.INITIATE)) {
            NodeQueryDto<ModelNode> modelDto = findActiveVersionModelFirstNode(form.getProcessName());
            currentNode = constructInstanceNode(JacksonUtils.toObj(modelDto.getNodeJson(), ModelNode.class));
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
    public NodeQueryDto<InstanceNode> queryCurrentInstanceNode(ExecuteForm form) {
        NodeQueryDto<InstanceNode> dto;
        if (form.getNum() < 5) {
            dto = instanceNodeRepository.queryCurrentInstanceNode(form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNodeId());
        }else {
            dto = instanceNodeRepository.queryCurrentInstanceNodeTooLong(form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNodeId(), form.getNum());
        }

        //查询结果校验
        if (dto == null || StringUtils.isBlank(dto.getNodeJson())) {
            log.error("流程执行失败，未找到当前实例节点：流程 {}-位置 {}-nodeId{}", form.getProcessName(), form.getNum(), form.getNodeId());
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
        if (!Objects.equals(dto.getNode().getStatus(), InstanceNodeStatus.PENDING)) {
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
     * @return InstanceNode
     */
    private InstanceNode constructInstanceNode(ModelNode modelNode) {
        //复制关键属性
        InstanceNode instanceNode = new InstanceNode();
        BeanUtils.copyProperties(modelNode, instanceNode,
                modelNode.ignoreCopyPropertyList());
        instanceNode.setModelNodeUid(modelNode.getNodeUid());

        //设置自动执行日期
        Integer autoInterval = modelNode.getAutoInterval();
        if (autoInterval != null && autoInterval > 0) {
            instanceNode.setAutoTime(LocalDate.now().plusDays(autoInterval));
        }

        //设置候选人
        if (StringUtils.isNotBlank(modelNode.getOperationCandidate())) {
            List<UserBaseInfo> candidates = (List<UserBaseInfo>) JacksonUtils.toObj(modelNode.getOperationCandidate(), List.class, UserBaseInfo.class);
            candidates = userChoose.getCandidateUsers(modelNode.getOperationType(), candidates);
            instanceNode.setOperationCandidate(JacksonUtils.toJson(candidates));
        }

        //方法执行条件
        instanceNode.setOnlyPassExecute(modelNode.getOnlyPassExecute());

        //设置状态
        instanceNode.setStatus(InstanceNodeStatus.PENDING);

        //设置时间
        instanceNode.setBeginTime(LocalDateTime.now());


        return instanceNode;
    }

    /**
     * 校验当前用户是否在候选人范围
     * @param form 表单
     * @param instanceNode 实例节点
     */
    private void inCandidate(ExecuteForm form, InstanceNode instanceNode) {
        if (instanceNode.getAutoTime() != null) {
            return;
        }
        UserBaseInfo user = form.getOperator();
        List<UserBaseInfo> candidate = (List<UserBaseInfo>) JacksonUtils.toObj(instanceNode.getOperationCandidate(), List.class, UserBaseInfo.class);
        if (!CollectionUtils.isEmpty(candidate)
                && candidate.stream().noneMatch(x -> x.equals(user))) {
            log.error("流程执行失败，操作人不在候选人中：流程 {}-版本 {}-key {}-当前位置{}-当前用户{}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum(), user);
            throw new NeoExecuteException("流程执行失败，不在候选人中");
        }
    }

    /**
     * 判断是否需要执行节点方法
     * @param operationType 执行类型
     * @param onlyPassExecute 是否只通过才执行方法
     * @return Boolean
     */
    private Boolean needOperateMethod(Integer operationType, Boolean onlyPassExecute) {
        if (config.getIndependence()) {
            log.info("独立部署，跳过流程方法");
            return false;
        }
        return (onlyPassExecute && operationType < InstanceOperationType.REJECTED)
                || (!onlyPassExecute && operationType < InstanceOperationType.FORWARD);
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
        Integer num = form.getNum();
        Integer condition = form.getCondition();
        String conditionKey = condition == null ? "null" : condition.toString();

        String key = cacheManager.mergeKey(processName, version.toString(), nodeUid, conditionKey);
        NeoCacheManager.CacheValue<ModelNode> cache = cacheManager.getCache(CacheType.N_M_N, key, ModelNode.class);
        ModelNode modelNode;
        if (cache.filter() || cache.value() != null) {
            modelNode = cache.value();
        } else {
            modelNode = modelNodeRepository.queryNextModelNode(processName, version, nodeUid, condition);
            cacheManager.setCache(CacheType.N_M_N, key, modelNode);
        }

        if (modelNode == null) {
            log.error("流程执行失败，未找到下一节点：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    processName, version, form.getBusinessKey(), num);
            throw new NeoExecuteException("流程执行失败，未找到下一节点");
        }

        return modelNode;
    }

    /**
     * 查询当前流程实例发起节点
     * @param form 表单
     * @return InstanceNode
     */
    private InstanceNode getInstanceInitiateNodeToRegression(ExecuteForm form) {
        InstanceNode initiateNode;
        NeoCacheManager.CacheValue<InstanceNode> cache = cacheManager.getCache(CacheType.I_I_N, form.getBusinessKey(), InstanceNode.class);
        if (cache.filter() || cache.value() != null) {
            initiateNode =  cache.value();
        } else {
            initiateNode = instanceNodeRepository.queryInstanceInitiateNode(form.getProcessName(), form.getVersion(), form.getBusinessKey());
            cacheManager.setCache(CacheType.I_I_N, form.getBusinessKey(), initiateNode);
        }

        if (initiateNode == null) {
            log.error("流程拒绝失败，退回时未找到当前流程实例发起节点：{}", form.getBusinessKey());
            throw new NeoExecuteException("流程拒绝失败，退回时未找到当前流程实例发起节点");
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
            String key = cacheManager.mergeKey(form.getProcessName(), form.getVersion().toString(), current.getModelNodeUid());
            NeoCacheManager.CacheValue<Boolean> cache = cacheManager.getCache(CacheType.M_C_T, key, Boolean.class);
            if (cache.filter() || cache.value() != null) {
                if (Objects.equals(cache.value(), true)) {
                    log.error("流程拒绝失败，当前节点不能拒绝：流程 {}-版本 {}-key {}- 当前节点位置{}",
                            form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
                    throw new NeoExecuteException("流程拒绝失败，当前节点不能拒绝");
                }
            }
            //缓存-模型终止节点
            key = cacheManager.mergeKey(form.getProcessName(), form.getVersion().toString());
            NeoCacheManager.CacheValue<ModelNode> ct = cacheManager.getCache(CacheType.M_T_N, key, ModelNode.class);
            ModelNode terminateNode;
            if (ct.filter() || ct.value() != null) {
                terminateNode = ct.value();
            }else {
                terminateNode = modelNodeRepository.MiddleNodeCanReject(form.getProcessName(), form.getVersion(), current.getModelNodeUid());
                cacheManager.setCache(CacheType.M_T_N, key, terminateNode);
            }
            //当前节点没与终止节点相连
            if (terminateNode == null) {
                cacheManager.setCache(CacheType.M_C_T, key, false);
                log.error("流程拒绝失败，当前节点不能拒绝：流程 {}-版本 {}-key {}- 当前节点位置{}",
                        form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
                throw new NeoExecuteException("流程拒绝失败，当前节点不能拒绝");
            }

            cacheManager.setCache(CacheType.M_C_T, key, true);
            return terminateNode;
        }

        //终止节点
        if (NodeLocationType.TERMINATE.equals(current.getLocation())) {
            log.error("流程拒绝失败，终止节点不能再拒绝：流程 {}-版本 {}-key {}- 当前节点位置{}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程拒绝失败，终止节点不能再拒绝");
        }

        //发起、完成 节点
        String key = cacheManager.mergeKey(form.getProcessName(), form.getVersion().toString());
        NeoCacheManager.CacheValue<ModelNode> ct = cacheManager.getCache(CacheType.M_T_N, key, ModelNode.class);
        ModelNode terminateNode;
        if (ct.filter() || ct.value() != null) {
            terminateNode = ct.value();
        }else {
            terminateNode = modelNodeRepository.queryModelTerminateNode(form.getProcessName(), form.getVersion());
            cacheManager.setCache(CacheType.M_T_N, key, terminateNode);
        }

        if (terminateNode == null) {
            cacheManager.setCache(CacheType.M_C_T, key, false);
            log.error("流程拒绝失败，未找到终止节点：流程 {}-版本 {}-key {}- 当前节点位置{}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程拒绝失败，未找到终止节点");
        }

        return terminateNode;

    }

    /**
     * 能否退回到发起人进行循环
     * @param form 表单
     * @return Boolean
     */
    private Boolean canCycle(ExecuteForm form) {
        String key = form.getBusinessKey();
        NeoCacheManager.CacheValue<Boolean> cache = cacheManager.getCache(CacheType.F_I_C, key, Boolean.class);
        if (cache.filter() || cache.value() != null) {
            return Objects.equals(cache.value(), true);
        }
        Boolean can = instanceNodeRepository.canCycle(form.getProcessName(), form.getVersion(), form.getBusinessKey());
        if (!can) {
            cacheManager.setCache(CacheType.F_I_C, key, false);
        }
        return can;
    }

    /**
     * 操作类型转成执行后的节点状态
     * @param operationType 操作类型
     * @return 节点状态
     */
    private Integer operateTypeToNodeStatus(Integer operationType) {
        if (Objects.equals(operationType, InstanceOperationType.REJECTED)) {
            return InstanceNodeStatus.REJECTED;
        }
        if (Objects.equals(operationType, InstanceOperationType.FORWARD)) {
            return InstanceNodeStatus.FORWARD;
        }
        if (Objects.equals(operationType, InstanceOperationType.TERMINATED)) {
            return InstanceNodeStatus.TERMINATED;
        }

        return InstanceNodeStatus.PASS;
    }

    /**
     * 获取更新的流程实例状态
     * @param current 当前流程实例节点
     * @param next 下一个模型节点
     * @return 流程实例状态
     */
    private Integer getFlowStatus(InstanceNode current, InstanceNode next) {
        if (next == null) {
          if (Objects.equals(current.getLocation(), NodeLocationType.COMPLETE)) {
              return InstanceStatus.COMPLETE;
          }
          if (Objects.equals(current.getLocation(), NodeLocationType.TERMINATE)) {
              return InstanceStatus.REJECTED;
          }
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

        UserBaseInfo user = new UserBaseInfo();
        user.setId(config.getAutoId());
        user.setName(config.getAutoName());

        ExecuteForm form = new ExecuteForm();
        form.setProcessName(currentForm.getProcessName())
                .setBusinessKey(currentForm.getBusinessKey())
                .setVersion(currentForm.getVersion())
                .setNum(num)
                .setNodeId(nodeId)
                .setOperator(currentForm.getOperator())
                .setOperator(user)
                .setOperationType(InstanceOperationType.PASS);

        return form;
    }

}
