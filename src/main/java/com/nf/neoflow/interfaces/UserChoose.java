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
     * 根据模型节点获取实际候选人
     * @param operationType 节点操作类型
     * @param modelCandidateInfo 模型节点候选人信息，根据设置可能为null
     * @return List<UserBaseInfo>
     */
    List<UserBaseInfo> getCandidateUsers(Integer operationType, List<UserBaseInfo> modelCandidateInfo);

    /**
     * 定义流程模型时，获取候选人列表
     * @return List<Object>
     */
    List<Object> getCandidateList();
}
