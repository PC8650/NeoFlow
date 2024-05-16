package org.nf.neoflow.component;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.constants.QueryForOperatorType;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.exception.NeoUserException;
import org.nf.neoflow.interfaces.UserChoose;
import org.nf.neoflow.utils.JacksonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 获取用户
 * @author PC8650
 */
@Component
@RequiredArgsConstructor
public class BaseUserChoose {

    private final NeoFlowConfig config;

    @Autowired
    @Lazy
    private UserChoose userChoose;

    private final String luceneTemplate = "\\{\"id\"\\:\"%s\" AND \"name\"\\:\"%s\"\\}";
    private final String luceneSpecialReplaceRegex = "([+\\-!(){}\\[\\]^\"~*?:\\\\/]|&&|\\|\\|)";

    /**
     * 定义流程模型时，获取候选人选择列表
     * @return 返回所有业务涉及到的可选候选人信息
     */
    public Object getCandidateList() {
        return userChoose.getCandidateList();
    }

    /**
     * 根据模型节点获取实际候选人
     * 需发起人操作类型 {@link NeoFlowConfig#initiatorFlag NeoFlowConfig.initiatorFlag} 的节点不会调用此方法
     * @param operationType 节点操作类型
     * @param modelCandidateInfo 模型节点候选人信息
     * @return List<UserBaseInfo>
     */
    public List<UserBaseInfo> getCandidateUsers(Integer operationType, List<UserBaseInfo> modelCandidateInfo) {
        return userChoose.getCandidateUsers(operationType, modelCandidateInfo);
    }

    /**
     * 根据节点操作类型校验候选人
     * @param operationType 节点操作类型
     * @param operationUser 当前操作用户
     * @param candidate 候选人列表，根据设置可能为null
     * @return Boolean 校验结果
     */
    public Boolean checkCandidateUser(Integer operationType, UserBaseInfo operationUser, List<UserBaseInfo> candidate) {
        return userChoose.checkCandidateUser(operationType, operationUser, candidate);
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
     * 获取当前用户涉及候选范围
     * @param userId 用户id
     * @param username 用户名
     * @param queryType 查询类型：1-发起，2-待办，3-已办
     * @return 符合lucene查询语法的String / 当前用户信息json
     */
    public String getCandidateRange(String userId, String username, Integer queryType) {
        //校验获取当前用户
        UserBaseInfo currentUser = user(userId, username);
        if (currentUser == null) {
            currentUser = new UserBaseInfo(userId, username);
        }

        //待办，获取当前用户涉及的候选人范围
        if (QueryForOperatorType.PENDING.equals(queryType)) {
            List<UserBaseInfo> candidateRange = userChoose.getCandidateRange(currentUser);
            if (CollectionUtils.isEmpty(candidateRange)) {
                throw new NeoUserException("当前用户信息没有涉及的候选人范围");
            }
            //转换成符合lucene查询语法的String
            return candidateRange.stream()
                    .map(userBaseInfo -> String.format(luceneTemplate,
                            userBaseInfo.getId().replaceAll(luceneSpecialReplaceRegex,"\\\\$1"),
                            userBaseInfo.getName().replaceAll(luceneSpecialReplaceRegex,"\\\\$1")))
                    .collect(Collectors.joining(" OR "));
        }

        //其他，当前用户信息json
        return JacksonUtils.toJson(currentUser);
    }

    /**
     * 获取用户
     * @return 用户信息
     */
    private UserBaseInfo getUser() {
        UserBaseInfo user = userChoose.getUser();
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
