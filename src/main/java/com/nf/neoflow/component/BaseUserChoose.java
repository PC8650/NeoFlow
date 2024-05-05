package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.exception.NeoUserException;
import com.nf.neoflow.interfaces.UserChoose;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 获取用户
 * @author PC8650
 */
@Data
@Component
public class BaseUserChoose {

    private final NeoFlowConfig config;

    @Autowired
    @Lazy
    private UserChoose userChooseService;

    /**
     * 根据模型节点获取实际候选人
     * 需发起人操作类型 {@link com.nf.neoflow.config.NeoFlowConfig#initiatorFlag NeoFlowConfig.initiatorFlag} 的节点不会调用此方法
     * @param operationType 节点操作类型
     * @param modelCandidateInfo 模型节点候选人信息
     * @return List<UserBaseInfo>
     */
    public List<UserBaseInfo> getCandidateUsers(Integer operationType, List<UserBaseInfo> modelCandidateInfo) {
        return userChooseService.getCandidateUsers(operationType, modelCandidateInfo);
    }

    /**
     * 根据节点操作类型校验候选人
     * @param operationType 节点操作类型
     * @param operationUser 当前操作用户
     * @param candidate 候选人列表，根据设置可能为null
     * @return Boolean 校验结果
     */
    public Boolean checkCandidateUser(Integer operationType, UserBaseInfo operationUser, List<UserBaseInfo> candidate) {
        return userChooseService.checkCandidateUser(operationType, operationUser, candidate);
    }

    /**
     * 获取或校验当前用户信息
     * @param userBaseInfo 当前用户信息
     * @return UserBaseInfo
     */
    public UserBaseInfo user(UserBaseInfo userBaseInfo) {
        if (config.getBaseUserChoose()) {
            return getUser();
        }

        if (userBaseInfo == null
                || StringUtils.isBlank(userBaseInfo.getId())
                || StringUtils.isBlank(userBaseInfo.getName())
        ) {
            throw new NeoUserException("用户信息缺失");
        }
        return userBaseInfo;
    }

    /**
     * 获取或校验当前用户信息
     * @param params 当前用户信息 id or name
     * @return UserBaseInfo
     */
    public UserBaseInfo user(String... params) {
        if (config.getBaseUserChoose()) {
            return getUser();
        }

        if (params.length == 2) {
            check(params[0], params[1]);
        } else if (params.length == 1) {
            check(params[0]);
        }else {
            throw new NeoUserException("参数错误");
        }
        return null;
    }

    /**
     * 获取用户
     * @return 用户信息
     */
    private UserBaseInfo getUser() {
        UserBaseInfo user = userChooseService.getUser();
        if (Objects.isNull(user)) {
            throw new NeoUserException("未获取到用户信息");
        }
        if (StringUtils.isBlank(user.getId()) || StringUtils.isBlank(user.getName())) {
            throw new NeoUserException("用户信息不完整");
        }
        return user;
    }

    private void check(String param) {
        if (StringUtils.isBlank(param)) {
            throw new NeoUserException("用户信息缺失");
        }
    }

    private void check(String param1, String param2) {
        if (StringUtils.isBlank(param1) || StringUtils.isBlank(param2)) {
            throw new NeoUserException("用户信息缺失");
        }
    }

}
