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
                V_I_T
        ));
    }

    /**
     * 流程版本启用历史
     */
    public static final String P_A_V_H = "p_a_v_h";

    /**
     * 流程版本模型
     */
    public static final String V_M = "v_m";

    /**
     * 流程版本迭代树
     */
    public static final String V_I_T = "v_i_t";



}
