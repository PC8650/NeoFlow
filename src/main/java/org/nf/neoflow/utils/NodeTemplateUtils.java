package org.nf.neoflow.utils;

import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.constants.NodeLocationType;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.dto.version.ModelNodeDto;

import java.util.*;

/**
 * 节点模版
 * @author PC8650
 */
public class NodeTemplateUtils {

    private NodeTemplateUtils(){}

    private static final String startName = "发起";
    private static final String completeName = "完成";
    private static final String terminateName = "终止";

    /**
     * 基础初始化
     * @param nodeLocationType 节点位置
     * @param name 节点名称
     * @param uid 节点uid
     * @param operationCandidate 候选人
     * @return ModelNodeDto
     */
    private static ModelNodeDto baseInit(Integer nodeLocationType, String name, String uid, List<UserBaseInfo> operationCandidate) {
        return new ModelNodeDto()
                .setLocation(nodeLocationType)
                .setAutoInterval(0)
                .setOperationCandidateInfo(operationCandidate)
                .setNodeUid(uid)
                .setName(name);
    }

    /**
     * 模版初始化
     * @return 包含模版节点的map
     */
    public static Map<String, Object> templateInit() {
        List<UserBaseInfo> operationCandidate = new ArrayList<>(0);
        //为边条件模型预留容量
        return new HashMap<String,Object>(5) {{
            put("发起节点", startInit(startName, null, operationCandidate));
            put("中间节点", middleInit(null,null, operationCandidate));
            put("完成节点", completeInit(completeName, null, operationCandidate));
            put("终止节点", terminateInit(terminateName, null, operationCandidate));
        }};
    }

    /**
     * 发起节点初始化，指定候选人
     * @param operationCandidate 候选人集合
     * @return 发起节点模版
     */
    public static ModelNodeDto startInit(List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.INITIATE, startName, UUID.randomUUID().toString(), operationCandidate);
    }

    /**
     * 发起节点初始化，指定节点名称和uid
     * @param name 节点名称
     * @param uid 节点uid
     * @return 发起节点模版
     */
    public static ModelNodeDto startInit(String name, String uid) {
        return baseInit(NodeLocationType.INITIATE,
                StringUtils.isBlank(name) ? startName : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                new ArrayList<>());
    }

    /**
     * 发起节点初始化，指定节点名称，uid，候选人
     * @param name 节点名称
     * @param uid 节点uid
     * @param operationCandidate 候选人集合
     * @return 发起节点模版
     */
    public static ModelNodeDto startInit(String name, String uid, List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.INITIATE,
                StringUtils.isBlank(name) ? startName : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                operationCandidate);
    }

    /**
     * 中间节点初始化，指定候选人
     * @param operationCandidate 候选人集合
     * @return 中间节点模版
     */
    public static ModelNodeDto middleInit(List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.MIDDLE, null, UUID.randomUUID().toString(), operationCandidate)
                .setAutoInterval(null);
    }

    /**
     * 中间节点初始化，指定节点名称和uid
     * @param name 节点名称
     * @param uid 节点uid
     * @return 中间节点模版
     */
    public static ModelNodeDto middleInit(String name, String uid) {
        return baseInit(NodeLocationType.MIDDLE,
                StringUtils.isBlank(name) ? null : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                new ArrayList<>())
                .setAutoInterval(null);
    }

    /**
     * 中间节点初始化，指定节点名称，uid，候选人
     * @param name 节点名称
     * @param uid 节点uid
     * @param operationCandidate 候选人集合
     * @return 中间节点模版
     */
    public static ModelNodeDto middleInit(String name, String uid, List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.MIDDLE,
                StringUtils.isBlank(name) ? null : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                operationCandidate)
                .setAutoInterval(null);
    }


    /**
     * 完成节点初始化，指定候选人
     * @param operationCandidate 候选人集合
     * @return 完成节点模版
     */
    public static ModelNodeDto completeInit(List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.COMPLETE, completeName, UUID.randomUUID().toString(), operationCandidate);
    }

    /**
     * 完成节点初始化，指定节点名称和uid
     * @param name 节点名称
     * @param uid 节点uid
     * @return 完成节点模版
     */
    public static ModelNodeDto completeInit(String name, String uid) {
        return baseInit(NodeLocationType.COMPLETE,
                StringUtils.isBlank(name) ? completeName : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                new ArrayList<>());
    }

    /**
     * 完成节点初始化，指定节点名称，uid，候选人
     * @param name 节点名称
     * @param uid 节点uid
     * @param operationCandidate 候选人集合
     * @return 完成节点模版
     */
    public static ModelNodeDto completeInit(String name, String uid, List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.COMPLETE,
                StringUtils.isBlank(name) ? completeName : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                operationCandidate);
    }

    /**
     * 终止节点初始化，指定候选人
     * @param operationCandidate 候选人集合
     * @return 终止节点模版
     */
    public static ModelNodeDto terminateInit(List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.COMPLETE, terminateName, UUID.randomUUID().toString(), operationCandidate);
    }

    /**
     * 终止节点初始化，指定节点名称和uid
     * @param name 节点名称
     * @param uid 节点uid
     * @return 终止节点模版
     */
    public static ModelNodeDto terminateInit(String name, String uid) {
        return baseInit(NodeLocationType.TERMINATE,
                StringUtils.isBlank(name) ? terminateName : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                new ArrayList<>());
    }

    /**
     * 终止节点初始化，指定节点名称，uid，候选人
     * @param name 节点名称
     * @param uid 节点uid
     * @param operationCandidate 候选人集合
     * @return 终止节点模版
     */
    public static ModelNodeDto terminateInit(String name, String uid, List<UserBaseInfo> operationCandidate) {
        return baseInit(NodeLocationType.TERMINATE,
                StringUtils.isBlank(name) ? terminateName : name,
                StringUtils.isBlank(uid) ? UUID.randomUUID().toString() : uid,
                operationCandidate);
    }

}
