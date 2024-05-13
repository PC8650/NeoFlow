package com.nf.neoflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存类型枚举
 * @author PC8650
 */
@Getter
@AllArgsConstructor
public enum CacheEnums {

    A_P_N("a_p_n", "所有流程名称", "all", "a_p_n+分隔符+all"),
    P_A_V_H("p_a_v_h", "流程版本启用历史", "流程名称", "p_a_v_h+分隔符+流程名称"),
    V_M("v_m", "流程版本模型", "流程名称+分隔符+版本号", "v_m+分隔符+流程名称+分隔符+版本号"),
    V_I_T("v_i_t" ,"流程版本迭代树", "流程名称", "v_i_t+分隔符+流程名称"),
    F_I_E("f_i_e" ,"流程实例是否存在", "业务key", "f_i_e+分隔符+业务key"),
    N_M_N("n_m_n" ,"下一个模型节点", "流程名称+分隔符+版本+分隔符+当前模型节点uid+分隔符+跳转条件", "n_m_n+分隔符+流程名称+分隔符+版本+分隔符+当前模型节点uid+分隔符+跳转条件"),
    M_C_T("m_c_t" ,"中间节点能否拒绝", "流程名称+分隔符+版本+分隔符+当前节点对应模型节点uid", "m_c_t+分隔符+流程名称+分隔符+版本+分隔符+当前节点对应模型节点uid"),
    M_T_N("m_t_n" ,"终止节点模型", "流程名称+分隔符+版本", "m_t_n+分隔符+流程名称+分隔符+版本"),
    F_I_C("f_i_c" ,"流程实例能否退回", "业务key", "f_i_c+分隔符+业务key"),
    I_I_N("i_i_n" ,"流程实例发起节点", "业务key", "i_i_n+分隔符+业务key"),
    I_B_T("i_b_t","流程实例开始时间","业务key","i_b_t+分隔符+业务key"),
    I_O_H("i_o_h","流程实例操作历史","业务key 或 业务key+分隔符+节点位置","i_o_h+分隔符+业务key 或 i_o_h+分隔符+业务key+分隔符+节点位置"),
    C_S("c_s", "缓存统计", "all", "");

    /**
     * 缓存类型
     */
    private final String type;
    /**
     * 信息
     */
    private final String info;
    /**
     * 默认策略规则
     */
    private final String defaultRule;
    /**
     * 自定义策略规则
     */
    private final String customRule;

    public static List<String> filterStatistics() {
        List<String> type = new ArrayList<>();
        for (CacheEnums value : CacheEnums.values()) {
            if (!value.equals(CacheEnums.C_S)) {
                type.add(value.getType());
            }
        }
        return type;
    }

    public static CacheEnums getByType(String type) {
        for (CacheEnums value : CacheEnums.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }

}
