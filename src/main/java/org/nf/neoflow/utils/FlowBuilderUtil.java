package org.nf.neoflow.utils;

import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.constants.NodeLocationType;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.dto.version.ModelNodeDto;
import org.nf.neoflow.dto.version.ProcessNodeEdge;
import org.nf.neoflow.dto.version.VersionModelCreateForm;
import org.nf.neoflow.exception.NeoProcessException;
import org.nf.neoflow.utils.JacksonUtils;
import org.nf.neoflow.utils.ModelCheckUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程Builder，用于后端测试生成流程模型 json
 * @author PC8650
 */
public class FlowBuilderUtil {

    private FlowBuilderUtil(){}

    private final static String FF = "first";

    private final static String SPLIT = ",";

    /**
     * 获取Builder
     * @param processName 流程名称
     * @param iterateFrom 迭代自版本
     * @return Builder
     */
    public static Builder of (String processName, Integer iterateFrom){
        return new Builder(processName, iterateFrom);
    }

    /**
     * 获取Builder
     * @param processName 流程名称
     * @return Builder
     */
    public static Builder of (String processName){
        return of(processName, null);
    }

    /**
     * 构建器
     */
    public static class Builder {

        private Builder(String processName, Integer iterateFrom) {
            if (StringUtils.isBlank(processName)) {
                throw new NeoProcessException("流程名称不能为空");
            }
            this.processName = processName;
            this.iterateFrom = iterateFrom;
            this.initiatorFlag = 0;
            //发起节点到第一个节点只能有一条路径
            this.pathMap = new HashMap<>(){{put(0, new ArrayList<>(1));}};
            this.conditionNodeMap = new HashMap<>();
        }

        /**
         * 流程名称
         */
        private final String processName;

        /**
         * 迭代自版本
         */
        private final Integer iterateFrom;

        /**
         * 退回次数上限
         */
        private  int cycle;

        /**
         * 终止方法
         */
        private String terminatedMethod;

        /**
         * 由发起人操作的节点操作类型标识，默认取0
         */
        private int initiatorFlag;

        /**
         * 创建用户
         */
        private UserBaseInfo user;

        /**
         * 发起节点
         */
        private ModelNodeDto startNode;

        /**
         * 完成节点
         */
        private ModelNodeDto completeNode;

        /**
         * 终止节点
         */
        private ModelNodeDto terminateNode;

        /**
         * 流程路径
         * Key: 起点标识长度
         * Value: 路径集合
         */
        private final Map<Integer, List<Path>> pathMap;

        /**
         * 条件节点
         * Key: 从发起节点到value的条件跳转
         * Value: 节点Uid
         */
        private final Map<String, String> conditionNodeMap;

        /**
         * 当前路径
         */
        private Path currentPath;

        /**
         * 设置退回次数上限
         * @param cycle 退回次数上限
         * @return Builder
         */
        public Builder cycle(int cycle) {
            this.cycle = cycle;
            return this;
        }

        /**
         * 设置终止方法
         * @param terminatedMethod 终止方法
         * @return Builder
         */
        public Builder terminatedMethod(String terminatedMethod) {
            this.terminatedMethod = terminatedMethod;
            return this;
        }

        /**
         * 设置发起人操作的节点操作类型标识
         * @param initiatorFlag 发起人标识
         * @return Builder
         */
        public Builder initiatorFlag(int initiatorFlag) {
            this.initiatorFlag = initiatorFlag;
            return this;
        }

        /**
         * 设置创建人
         * @param createBy 创建人标识
         * @param createByName 创建人名称
         * @return Builder
         */
        public Builder createBy(String createBy, String createByName) {
            user = new UserBaseInfo(createBy, createByName);
            return this;
        }

        /**
         * 设置创建人
         * @param user 创建人
         * @return Builder
         */
        public Builder createBy(UserBaseInfo user) {
           this.user = user;
            return this;
        }

        /**
         * 设置发起节点
         * @param startNode startNode
         * @return Builder
         */
        public Builder startNode(ModelNodeDto startNode) {
            if (this.startNode != null) {
                throw new NeoProcessException("发起节点只能设置一次");
            }
            this.startNode = startNode;
            return this;
        }

        /**
         * 设置完成节点
         * @param completeNode completeNode
         * @return Builder
         */
        public Builder completeNode(ModelNodeDto completeNode) {
            if (this.completeNode != null) {
                throw new NeoProcessException("完成节点只能设置一次");
            }
            this.completeNode = completeNode;
            return this;
        }

        /**
         * 设置终止节点
         * @param terminateNode terminateNode
         * @return Builder
         */
        public Builder terminateNode(ModelNodeDto terminateNode) {
            if (this.terminateNode != null) {
                throw new NeoProcessException("终止节点只能设置一次");
            }
            this.terminateNode = terminateNode;
            return this;
        }

        /**
         * 设置一条新路径
         * @param startFlag 起点标识 。从发起节点开始，依次根据跳转条件找到新路径的起始节点
         * @return Builder
         */
        public Path newPath(int... startFlag) {
            if (currentPath != null && !currentPath.isComplete) {
                throw new NeoProcessException("当前路径未完成");
            }

            int length;
            if (startFlag == null || (length = startFlag.length) == 0) {
                currentPath = new Path(this);
                List<Path> paths = pathMap.get(0);
                if (!paths.isEmpty()) {
                    throw new NeoProcessException("由发起节点开始的路径已存在");
                }
                paths.add(currentPath);
                return currentPath;
            }

            String startFlagStr = Arrays.stream(startFlag).mapToObj(String::valueOf).collect(Collectors.joining(SPLIT));
            currentPath = new Path(startFlagStr,this);
            if (pathMap.containsKey(length)) {
                pathMap.get(length).add(currentPath);
            } else {
                pathMap.put(length, new ArrayList<>(){{add(currentPath);}});
            }
            return currentPath;
        }

        /**
         * 设置一条新路径，发起节点作为起始节点
         * @return Builder
         */
        public Path newPath() {
            return newPath(null);
        }

        /**
         * 预检查
         */
        private void buildPreCheck() {
            StringBuilder builder = new StringBuilder();
            if (startNode == null) {
                builder.append("未设置发起节点\n");
            }
            if (completeNode == null) {
                builder.append("未设置完成节点\n");
            }
            if (currentPath == null) {
                builder.append("未设置路径\n");
            } else if (!currentPath.isComplete) {
                builder.append("存在未完成的路径\n");
            }

            if (!builder.isEmpty()) {
                throw new NeoProcessException(builder.toString());
            }

        }

        /**
         * 构建创建流程模型接口参数json
         * @return json
         */
        public String build() {
            buildPreCheck();

            Set<ModelNodeDto> nodeSet = new HashSet<>(){{
                add(startNode);
                add(completeNode);
                if (terminateNode != null) {add(terminateNode);}
            }};
            Set<ProcessNodeEdge> edgeSet = new HashSet<>();

            pathMap.keySet().stream().sorted(Comparator.naturalOrder())
                    .forEach(k -> pathMap.get(k)
                            .forEach(path -> getNodeAndEdge(path, nodeSet, edgeSet))
                    );

            ModelCheckUtils.validateModel(0,nodeSet, edgeSet);

            VersionModelCreateForm form = new VersionModelCreateForm();
            form.setNodes(nodeSet);
            form.setEdges(edgeSet);
            form.setCycle(cycle);
            form.setIterateFrom(iterateFrom);
            form.setProcessName(processName);
            form.setTerminatedMethod(terminatedMethod);
            if (user != null) {
                form.setCreateBy(user.getId());
                form.setCreateByName(user.getName());
            }

            return JacksonUtils.toJson(form);
        }

        /**
         * 收集节点和边
         * @param path 路径
         * @param nodeSet 节点收集set
         * @param edgeSet 边收集set
         */
        private void getNodeAndEdge(Path path, Set<ModelNodeDto> nodeSet, Set<ProcessNodeEdge> edgeSet) {
            String startFlagStr = path.startFlag;
            String lastUid;
            if (FF.equals(startFlagStr)) {
                lastUid = startNode.getNodeUid();
            }else {
                lastUid = conditionNodeMap.get(startFlagStr);
                if (lastUid == null) {
                    System.out.println(path.startFlag);
                }
            }

            int size = path.nodes.size();

            for (int i = 0; i < size; i++) {
                Node node = path.nodes.get(i);
                nodeSet.add(node.node);
                edgeSet.add(new ProcessNodeEdge(lastUid, node.node.getNodeUid(), node.condition));
                if (node.reject) {
                    if (terminateNode == null) {
                        throw new NeoProcessException("未设置终止节点");
                    }
                    edgeSet.add(new ProcessNodeEdge(node.node.getNodeUid(), terminateNode.getNodeUid(), node.rejectCondition));
                }
                lastUid = node.node.getNodeUid();

                if (i == size - 1) {
                    edgeSet.add(new ProcessNodeEdge(lastUid, completeNode.getNodeUid(), path.completeCondition));
                }
            }
        }

    }

    /**
     * 路径
     */
    public static class Path {

        private Path(Builder builder) {
            this(FF, builder);
        }

        private Path(String startFlag, Builder builder) {
            this.builder = builder;
            if (FF.equals(startFlag)) {
                this.record = new StringBuilder(startFlag);
                this.startFlag = startFlag;
            } else {
                this.record = new StringBuilder(FF).append(SPLIT).append(startFlag);
                this.startFlag =  record.toString();
            }
            this.nodes = new ArrayList<>();
            this.isComplete = false;
        }

        /**
         * 当前构建器
         */
        private final Builder builder;

        /**
         * 路径起始标识，从发起节点往后的跳转条件，定位路径起始节点
         */
        private final String startFlag;

        /**
         * 路径记录，添加节点时，在startFlag的基础上添加跳转条件
         */
        private final StringBuilder record;

        /**
         * 路径节点列表
         */
        private final List<Node> nodes;

        /**
         * 路径是否完成
         */
        private boolean isComplete;

        /**
         * 路径末尾节点到完成节点的条件
         */
        private int completeCondition;

        /**
         * 添加节点
         * @param condition 跳转条件
         * @param reject 能否驳回
         * @param rejectCondition 跳转至终止节点的条件
         *  @param node 节点
         * @return Builder
         */
        public Path addNode(int condition, boolean reject, int rejectCondition, ModelNodeDto node) {
            if (node == null) {
                throw new NeoProcessException("节点不能为空");
            }
            if (!NodeLocationType.MIDDLE.equals(node.getLocation())) {
                throw new NeoProcessException("只能添加中间节点");
            }
            nodes.add(new Node(node, condition, reject, rejectCondition));
            record.append(SPLIT).append(condition);
            builder.conditionNodeMap.put(record.toString(), node.getNodeUid());
            return this;
        }

        /**
         * 添加节点，不能驳回
         * @param condition 跳转条件
         * @param node 节点
         * @return Builder
         */
        public Path addNode( int condition, ModelNodeDto node) {
            return addNode(condition, false, 0, node);
        }

        /**
         * 完成路径
         * @param condition 跳转至完成节点的条件
         * @return Builder
         */
        public Builder complete(int condition) {
            if (nodes.isEmpty()) {
                throw new NeoProcessException("路径未设置节点");
            }
            isComplete = true;
            completeCondition = condition;
            return builder;
        }

    }

    /**
     * 节点
     */
    private static class Node {

        private Node(ModelNodeDto node, int condition, boolean reject, int rejectCondition){
            this.node = node;
            this.condition = condition;
            this.reject = reject;
            this.rejectCondition = rejectCondition;
        }

        /**
         * 流程模型节点
         */
       private final ModelNodeDto node;

        /**
         * 上一个节点到当前节点的条件
         */
       private final int condition;

        /**
         * 能否驳回
         */
       private final boolean reject;

        /**
         * 到终止节点的条件
         */
       private final int rejectCondition;
    }

}
