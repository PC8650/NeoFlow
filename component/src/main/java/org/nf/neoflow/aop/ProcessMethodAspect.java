package org.nf.neoflow.aop;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.nf.neoflow.constants.InstanceOperationType;
import org.nf.neoflow.constants.NodeLocationType;
import org.nf.neoflow.dto.execute.ExecuteForm;
import org.nf.neoflow.exception.NeoExecuteException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 环绕处理流程方法，对部分参数进行前后一致性校验和非空校验
 * 将方法异常控制在业务服务内
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ProcessMethodAspect {

    @SneakyThrows
    @Around("execution(* org.nf.neoflow.component.BusinessOperatorManager.operate(..))")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteForm around(ProceedingJoinPoint point) {
        log.info("记录流程参数");
        ExecuteForm form = getForm(point);
        //记录关键数据
        String businessKey = form.getBusinessKey();
        String processName = form.getProcessName();
        Integer version = form.getVersion();
        Long nodeId = form.getNodeId();
        Integer num = form.getNum();
        Integer condition = form.getCondition();
        boolean conditionByMethod = form.getConditionByMethod();

        Integer location = form.getLocation();
        Integer defaultPassCondition = form.getDefaultPassCondition();

        log.info("执行业务方法");
        //执行方法
        form = (ExecuteForm) point.proceed();

        log.info("流程参数非空和一致性校验");
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
                !Objects.equals(form.getNum(), num) ||
                !Objects.equals(form.getLocation(), location)
        ) {
            log.error("流程执行失败，关键数据不一致：流程 {}-版本 {}-key {}-当前节点位置 {}", processName, version, businessKey, num);
            throw new NeoExecuteException("流程执行失败，节点方法后关键数据变更");
        }

        //跳转条件不依赖方法结果，设置方法执行前的条件，防止被修改
        if (!conditionByMethod) form.setCondition(condition);

        //发起、通过 必须有跳转条件
        if (form.getOperationType() < InstanceOperationType.REJECTED && location <= NodeLocationType.MIDDLE) {
            if (form.getCondition() == null) {
                if (defaultPassCondition == null) {
                    log.error("流程执行失败，缺失跳转条件：流程 {}-版本 {}-key {}-当前节点位置 {}",
                            form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
                    throw new NeoExecuteException("流程执行失败，缺失跳转条件");
                }
                form.setCondition(defaultPassCondition);
            }
        }

        return form;
    }

    /**
     * 获取 ExecuteForm
     * @param point 切点方法
     * @return ExecuteForm
     */
    private ExecuteForm getForm(ProceedingJoinPoint point) {
        Object[] args = point.getArgs();
        return (ExecuteForm) args[0];
    }

}
