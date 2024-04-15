package com.nf.neoflow.interfaces;

import com.nf.neoflow.dto.user.UserBaseInfo;

import java.util.List;

/**
 * 用户选择
 * 需要实现接口，并重写服务方法，手动注入spring容器
 * @author PC8650
 */
public interface UserChoose {

    /**
     * 用于流程的 "创建"、"修改"、"发起"、"审批" 操作中获取当前用户信息
     * 该方法没有入参，意味着需能从RequestContextHolder、ThreadLocal等 获取当前用户信息
     * @return UserBaseInfo
     */
    UserBaseInfo getUser();

    /**
     * 用于获取流程实例中下一节点的候选人
     * @param obj 候选人策略信息
     * @return List<UserBaseInfo>
     */
    List<UserBaseInfo> getCandidateUsers(Object obj);

    /**
     * 定义流程模型时，获取候选人列表
     * @return List<Object>
     */
    List<Object> getCandidateList();
}
