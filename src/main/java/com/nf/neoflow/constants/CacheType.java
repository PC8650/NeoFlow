package com.nf.neoflow.constants;

import java.util.List;

/**
 * 缓存分类
 * @author PC8650
 */
public class CacheType {

    public static List<String> getAllType () {
        return new java.util.ArrayList<>(List.of(
                P_A_V_H,
                V_M,
                V_I_T,
                F_I_E,
                N_M_N
        ));
    }

    /**
     * 流程版本启用历史
     * 默认策略：流程名称
     * 自定义策略：p_a_v_h+分隔符+流程名称
     */
    public static final String P_A_V_H = "p_a_v_h";

    /**
     * 流程版本模型
     * 默认策略：流程名称+分隔符+版本号
     * 自定义策略：v_m+分隔符+流程名称+分隔符+版本号
     */
    public static final String V_M = "v_m";

    /**
     * 流程版本迭代树
     * 默认策略：流程名称
     * 自定义策略：v_i_t+分隔符+流程名称
     */
    public static final String V_I_T = "v_i_t";

    /**
     * 流程实例是否存在
     * 默认策略：业务key
     * 自定义策略：f_i_e+分隔符+业务key
     */
    public static final String F_I_E = "f_i_e";

    /**
     * 下一个模型节点
     * 默认策略：流程名称+分隔符+版本+分隔符+当前模型节点uid+分隔符+跳转条件
     * 自定义策略：n_m_n+分隔符+流程名称+分隔符+版本+分隔符+当前模型节点uid+分隔符+跳转条件
     */
    public static final String N_M_N = "n_m_n";

}
