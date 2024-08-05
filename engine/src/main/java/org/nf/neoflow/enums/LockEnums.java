package org.nf.neoflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 锁枚举
 * @author PC8650
 */
@Getter
@AllArgsConstructor
public enum LockEnums {

    PROCESS_STATUS("process_status","变更流程状态","流程状态变更中，请稍后重试","用于更改流程启用状态、更改流程启用版本，建议流程名称为key"),
    VERSION_CREATE("version_create","创建流程模型","流程模型创建中，请稍后重试","用于创建版本，建议流程名称为key"),
    FLOW_EXECUTE("flow_execute","流程执行","流程执行中，请稍后重试","用于流程执行，建议businessKey为key"),
    AUTO_EXECUTE("auto_execute","扫描执行自动节点","扫描执行自动节点中，请稍后重试","用于定时任务扫描执行自动节点，防止集群环境或人为触发大规模全库查询，key统一任意字符。控制范围仅为统计扫描的节点数，不包含节点后的自动节");


    /**
     * 锁名称
     */
    private final String name;
    /**
     * 锁行日志信息
     */
    private final String msg;
    /**
     * 锁错误信息
     */
    private final String errorMsg;
    /**
     * 描述
     */
    private final String desc;

    /**
     * 获取所有锁名称
     * @return lockNames
     */
    public static List<String> allLockNames() {
        List<String> lockNames = new ArrayList<>();
        for (LockEnums value : LockEnums.values()) {
            lockNames.add(value.name);
        }
        return lockNames;
    }

}
