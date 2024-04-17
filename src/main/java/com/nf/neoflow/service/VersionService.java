package com.nf.neoflow.service;

import com.nf.neoflow.component.BaseUserChoose;
import com.nf.neoflow.component.NeoCacheManager;
import com.nf.neoflow.component.NeoLockManager;
import com.nf.neoflow.constants.CacheType;
import com.nf.neoflow.constants.NodeLocationType;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.dto.version.*;
import com.nf.neoflow.enums.LockEnums;
import com.nf.neoflow.exception.NeoProcessException;
import com.nf.neoflow.repository.VersionRepository;
import com.nf.neoflow.utils.JacksonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepository versionRepository;
    private final BaseUserChoose userChoose;
    private final NeoLockManager lockManager;
    private final NeoCacheManager cacheManager;


    /**
     * 查询流程版本列表
     * @param form 查询表单
     * @return
     */
    public Page<VersionListDto> versionList(VersionListQueryForm form) {
        Sort sort;
        if (form.getDesc()) {
            sort = Sort.by(Sort.Direction.DESC,"createTime");
        }else {
            sort = Sort.by(Sort.Direction.ASC,"createTime");
        }
        Pageable pageable = PageRequest.of(form.getPageNumber()-1, form.getPageSize(), sort);
        return versionRepository.queryVersionList(form.getProcessName(), pageable.getOffset(), form.getPageSize(), pageable);
    }

    /**
     * 版本视图
     * @param form 查询表单
     * @return VersionModelViewDto
     */
    public VersionModelViewDto versionView(VersionViewQueryForm form) {
        String cacheKey = cacheManager.mergeKey(form.getProcessName(), form.getVersion().toString());

        NeoCacheManager.CacheValue<VersionModelViewDto> value =cacheManager.getCache(CacheType.V_M, cacheKey, VersionModelViewDto.class);
        if (value.filter()) {
            throw new NeoProcessException("流程版本模型不存在");
        }else if (value.value() != null) {
            return value.value();
        }

        VersionModelViewDto dto;
        if (form.getVersion() != null) {
            dto = versionRepository.queryVersionView(form.getProcessName(), form.getVersion());
            if (dto == null) {
                cacheManager.setCache(CacheType.V_M, cacheKey, null);
                throw new NeoProcessException("流程版本模型不存在");
            }
        }else {
            dto = new VersionModelViewDto();
            dto.setProcessName(form.getProcessName());
        }
        dto.setComponentModel(componentModelInit());
        cacheManager.setCache(CacheType.V_M, cacheKey, dto);
        return dto;
    }

    /**
     * 版本迭代树
     * @param form 查询表单
     */
    public Object versionIterateTree(IterateTreeQueryForm form) {
        String cacheKey = cacheManager.mergeKey(form.getProcessName(), form.getType().toString());

        NeoCacheManager.CacheValue<Object> value = cacheManager.getCache(CacheType.V_I_T, cacheKey, Object.class);
        if (value.filter() || value.value() != null) {
            return value.value();
        }

        Object result;
        switch (form.getType()) {
            case 1 -> {
                result = versionRepository.queryVersionIterateTree(form.getProcessName());
            }
            case 2 -> {
                result = versionRepository.queryVersionIterateTreeNested(form.getProcessName());
            }
            case 3 -> {
                result = versionRepository.queryVersionIterateTreeGraphic(form.getProcessName());
            }
            default -> throw new NeoProcessException("参数错误");
        }

        cacheManager.setCache(CacheType.V_I_T, cacheKey, result);
        return result;
    }

    /**
     * 创建版本
     * @param form 创建版本表单
     */
    public void createVersion(VersionModelCreateForm form) {
        log.info("{}创建新版本", form.getProcessName());
        int version;
        LockEnums lockEnum = LockEnums.VERSION_CREATE;
        boolean getLock = false;
        try {
            // 加锁，避免版本号并发重复
            getLock = lockManager.getLock(form.getProcessName(), lockEnum);
            //获取用户信息
            UserBaseInfo user = userChoose.user(form.getCreateBy(), form.getCreateByName());
            if (user != null) {
                form.setCreateBy(user.getId());
                form.setCreateByName(user.getName());
            }
            //判断流程是否存在
            String exists = versionRepository.existsProcessAndIterateFrom(form.getProcessName(), form.getIterateFrom());
            if (exists != null) {
                throw new NeoProcessException(exists);
            }
            //由于Neo4jRepository不能适配转换复杂对象，这里将对象转成map
            Set<Map<String,Object>> nodes = new HashSet<>();
            Set<Map<String,Object>> edges = new HashSet<>();
            //校验模型参数
            validateModel(form.getNodes(), form.getEdges(), nodes, edges);
            //创建模型
            version = versionRepository.createVersion(nodes, edges,
                    form.getProcessName(), form.getIterateFrom(), form.getCycle(), form.getCreateBy(), form.getCreateByName(),
                    LocalDateTime.now()
            );

            //删除迭代树缓存
            deleteIterateTreeCache(form.getProcessName());
        } finally {
            lockManager.releaseLock(form.getProcessName(), getLock, lockEnum);
        }

        log.info("{}创建新版本成功，版本号{}", form.getProcessName(), version);
    }

    /**
     * 校验要创建的model数据
     * @param nodes 节点
     * @param edges 边
     * @param nodesSet nodes map集合
     * @param edgesSet edges map集合
     */
    private void validateModel(Set<ModelNodeDto> nodes, Set<ProcessNodeEdge> edges,
                               Set<Map<String,Object>> nodesSet, Set<Map<String,Object>> edgesSet){
        Map<String,ModelNodeDto> nodeMap = new HashMap<>(nodes.size());
        validateNodes(nodes, nodeMap, nodesSet);
        validateEdges(edges, nodeMap, edgesSet);
    }

    /**
     * 校验节点
     * @param nodes 节点
     * @param nodeMap 节点map
     */
    private void validateNodes(Set<ModelNodeDto> nodes, Map<String,ModelNodeDto> nodeMap, Set<Map<String,Object>> nodesSet){
        int startCount = 0, completeCount = 0, terminateCount = 0;
        Map<String,Object> dtoMap;
        for (ModelNodeDto node : nodes) {
            //自检基本属性
            node.check();

            //特殊节点数量
            if (Objects.equals(node.getLocation(), NodeLocationType.BEGIN)) {
                startCount++;
            } else if (Objects.equals(node.getLocation(), NodeLocationType.COMPLETE)) {
                completeCount++;
            } else if (Objects.equals(node.getLocation(), NodeLocationType.TERMINATE)) {
                terminateCount++;
            }

            nodeMap.put(node.getNodeUid(), node);
            dtoMap = JacksonUtils.objToMap(node);
            if (!CollectionUtils.isEmpty(node.getOperationCandidateInfo())) {
                String candidate = JacksonUtils.toJson(node.getOperationCandidateInfo());
                dtoMap.put("operationCandidate", candidate);
                dtoMap.remove("operationCandidateInfo");
            }
            nodesSet.add(dtoMap);
        }

        if (startCount != 1 || completeCount != 1 || terminateCount != 1) {
            throw new NeoProcessException("开始节点、完成节点、终止节点有且只能存在一个");
        }
    }

    /**
     * 校验边
     * @param edges 边
     * @param nodeMap 节点map
     */
    private void validateEdges(Set<ProcessNodeEdge> edges, Map<String,ModelNodeDto> nodeMap, Set<Map<String,Object>> edgesSet){
        ModelNodeDto startNode;
        ModelNodeDto endNode;
        Set<String> includeNodes = new HashSet<>(nodeMap.size());
        for (ProcessNodeEdge edge : edges) {
            //校验空值
            if (StringUtils.isBlank(edge.getStartNode()) || StringUtils.isBlank(edge.getEndNode())) {
                throw new NeoProcessException("边端点不能为空");
            }
            if (edge.getCondition() == null) {
                throw new NeoProcessException("边条件不能为空");
            }
            if (!nodeMap.containsKey(edge.getStartNode()) || !nodeMap.containsKey(edge.getEndNode())) {
                throw new NeoProcessException(String.format("边端点不存在 (%s)-->(%s)", edge.getStartNode(), edge.getEndNode()));
            }

            //校验结构
            startNode = nodeMap.get(edge.getStartNode());
            endNode = nodeMap.get(edge.getEndNode());
            if (Objects.equals(endNode.getLocation(), NodeLocationType.BEGIN)) {
                throw new NeoProcessException("不能指向开始节点");
            }
            if (Objects.equals(startNode.getLocation(), NodeLocationType.COMPLETE)
                    || Objects.equals(startNode.getLocation(), NodeLocationType.TERMINATE)) {
                throw new NeoProcessException("完成、终止 节点不能有后续");
            }

            includeNodes.add(edge.getStartNode());
            includeNodes.add(edge.getEndNode());

            Map<String,Object> edgeMap = JacksonUtils.objToMap(edge);
            edgesSet.add(edgeMap);
        }

        if (includeNodes.size() != nodeMap.size()) {
            throw new NeoProcessException("存在节点未包含在边中");
        }
    }

    /**
     * 处理迭代树，封装嵌套
     * @return List<IterateTreeNode>
     */
    private List<IterateTreeNode> treeNested(List<IterateTreeNode> treeNodes) {
        if (CollectionUtils.isEmpty(treeNodes)) {
            return new ArrayList<>(0);
        }

        //转成map，用于遍历时被调用更新迭代列表
        Map<Integer, IterateTreeNode> map = treeNodes.stream()
                .collect(Collectors.toMap(IterateTreeNode::getVersion, Function.identity()));

        //返回的结果，顶层node列表
        List<IterateTreeNode> topNodes = new ArrayList<>();

        //循环填充迭代
        for (IterateTreeNode node : treeNodes) {
            if (node.getTop()) {
                topNodes.add(node);
            }
            if (CollectionUtils.isEmpty(node.getIterateVersion())) {
                continue;
            }
            for (Integer version : node.getIterateVersion()) {
                node.getIterate().add(map.get(version));
            }
        }

        return topNodes;
    }

    /**
     * 删除迭代树缓存
     * @param ProcessName 缓存名称
     */
    private void deleteIterateTreeCache(String ProcessName) {
        List<String> cacheKeys = new ArrayList<>(){{
            add(cacheManager.mergeKey(ProcessName, "1"));
            add(cacheManager.mergeKey(ProcessName, "2"));
            add(cacheManager.mergeKey(ProcessName, "3"));
        }};
        cacheManager.deleteCache(CacheType.V_I_T, cacheKeys);
    }

    /**
     * 组件模型初始化
     * @return Map<String, Object>
     */
    private Map<String, Object> componentModelInit() {
        Map<String, Object> map = new HashMap<>(5);
        List<UserBaseInfo> operationCandidate = new ArrayList<>(0);
        map.put("发起节点",startInit(operationCandidate));
        map.put("中间节点",middleInit(operationCandidate));
        map.put("完成节点",completeInit(operationCandidate));
        map.put("终止节点",terminateInit(operationCandidate));
        map.put("条件关系",new ProcessNodeEdge());

        return map;
    }

    private ModelNodeDto startInit(List<UserBaseInfo> operationCandidate) {
        ModelNodeDto modelNodeDto = new ModelNodeDto();
        modelNodeDto.setLocation(1);
        modelNodeDto.setAutoInterval(0);
        modelNodeDto.setOperationCandidateInfo(operationCandidate);
        modelNodeDto.setName("发起");
        return modelNodeDto;
    }

    private ModelNodeDto middleInit(List<UserBaseInfo> operationCandidate) {
        ModelNodeDto modelNodeDto = new ModelNodeDto();
        modelNodeDto.setLocation(2);
        modelNodeDto.setOperationCandidateInfo(operationCandidate);
        return modelNodeDto;
    }

    private  ModelNodeDto completeInit(List<UserBaseInfo> operationCandidate) {
        ModelNodeDto modelNodeDto = new ModelNodeDto();
        modelNodeDto.setLocation(3);
        modelNodeDto.setAutoInterval(0);
        modelNodeDto.setOperationCandidateInfo(operationCandidate);
        modelNodeDto.setName("完成");
        return modelNodeDto;
    }

    private  ModelNodeDto terminateInit(List<UserBaseInfo> operationCandidate) {
        ModelNodeDto modelNodeDto = new ModelNodeDto();
        modelNodeDto.setLocation(4);
        modelNodeDto.setAutoInterval(0);
        modelNodeDto.setOperationCandidateInfo(operationCandidate);
        modelNodeDto.setName("终止");
        return modelNodeDto;
    }

}
