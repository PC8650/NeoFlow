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
     * 可根据 {@link com.nf.neoflow.config.NeoFlowConfig#baseUserChoose NeoFlowConfig.baseUserChoose} 选择是否实现
     * @return UserBaseInfo
     */
    UserBaseInfo getUser();

    /**
     * 根据模型节点获取实际候选人
     * 需发起人操作类型 {@link com.nf.neoflow.config.NeoFlowConfig#initiatorFlag NeoFlowConfig.initiatorFlag} 的节点不会调用此方法
     * @param operationType 节点操作类型
     * @param modelCandidateInfo 模型节点候选人信息，根据设置可能为null
     * @return List<UserBaseInfo>
     */
    List<UserBaseInfo> getCandidateUsers(Integer operationType, List<UserBaseInfo> modelCandidateInfo);

    /**
     * 根据节点操作类型校验候选人
     * @param operationType 节点操作类型
     * @param operationUser 当前操作用户
     * @param candidate 候选人列表，根据设置可能为null
     * @return Boolean 校验结果
     */
    Boolean checkCandidateUser(Integer operationType, UserBaseInfo operationUser, List<UserBaseInfo> candidate);

    /**
     * 待办列表获取候选人范围
     * 结合当前用户和实际业务涉及到的节点操作类型，获取在所有节点操作类型范围内的候选信息
     * 假设业务中定义了[用户、部门]两种节点操作类型，且节点候选人直接设置的为 用户信息、部门信息
     * 则需要返回 [当前用户信息，当前用户所属部门信息]
     * @param currentUser 当前用户
     * @return List<UserBaseInfo> 例如[当前用户信息，当前用户所属部门信息 ...]
     */
    List<UserBaseInfo> getCandidateRange(UserBaseInfo currentUser);

    /**
     * 定义流程模型时，获取候选人列表
     * @return List<Object>
     */
    List<Object> getCandidateList();
}
