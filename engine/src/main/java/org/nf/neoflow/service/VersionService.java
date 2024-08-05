package org.nf.neoflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nf.neoflow.component.BaseUserChoose;
import org.nf.neoflow.component.NeoCacheManager;
import org.nf.neoflow.component.NeoLockManager;
import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.dto.version.*;
import org.nf.neoflow.enums.CacheEnums;
import org.nf.neoflow.enums.LockEnums;
import org.nf.neoflow.exception.NeoProcessException;
import org.nf.neoflow.repository.VersionRepository;
import org.nf.neoflow.utils.ModelCheckUtils;
import org.nf.neoflow.utils.NodeTemplateUtils;
import org.nf.neoflow.utils.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 版本服务
 * @author PC8650
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepository versionRepository;
    private final BaseUserChoose userChoose;
    private final NeoLockManager lockManager;
    private final NeoCacheManager cacheManager;
    private final NeoFlowConfig config;


    /**
     * 查询流程版本列表
     * @param form 查询表单
     * @return Page<VersionListDto>
     */
    public Page<VersionListDto> versionList(VersionListQueryForm form) {
        Pageable pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "createTime", form.getDesc());
        return versionRepository.queryVersionList(form.getProcessName(), pageable);
    }

    /**
     * 版本视图
     * @param form 查询表单
     * @return VersionModelViewDto
     */
    public VersionModelViewDto versionView(VersionViewQueryForm form) {
        String processName = form.processName();
        Integer version = form.version();
        VersionModelViewDto dto;

        //版本为空，返回组件模型
        if (version == null) {
            dto = new VersionModelViewDto();
            dto.setProcessName(processName);
            dto.setComponentModel(componentModelInit());
            return dto;
        }

        //版本不为空，查询版本视图
        String cacheKey = cacheManager.mergeKey(processName, version.toString());
        String cacheType = CacheEnums.V_M.getType();
        NeoCacheManager.CacheValue<VersionModelViewDto> value =cacheManager.getCache(cacheType, cacheKey, VersionModelViewDto.class);
        if (value.filter() || value.value() != null) {
            dto = value.value();
        }else {
            dto = versionRepository.queryVersionView(processName, version);
        }
        if (dto == null) {
            cacheManager.setCache(cacheType, cacheKey, null);
            throw new NeoProcessException("流程版本模型不存在");
        }
        dto.setComponentModel(componentModelInit());
        cacheManager.setCache(cacheType, cacheKey, dto);
        return dto;
    }

    /**
     * 版本迭代树
     * @param form 查询表单
     */
    public Object versionIterateTree(IterateTreeQueryForm form) {
        String cacheKey = cacheManager.mergeKey(form.getProcessName(), form.getType().toString());
        String cacheType = CacheEnums.V_I_T.getType();
        NeoCacheManager.CacheValue<Object> value = cacheManager.getCache(cacheType, cacheKey, Object.class);
        if (value.filter() || value.value() != null) {
            return value.value();
        }

        Object result;
        switch (form.getType()) {
            case 1 -> result = versionRepository.queryVersionIterateTree(form.getProcessName());
            case 2 -> result = treeNested(versionRepository.queryVersionIterateTreeNested(form.getProcessName()));
            case 3 -> result = versionRepository.queryVersionIterateTreeGraphic(form.getProcessName());
            default -> throw new NeoProcessException("参数错误");
        }

        cacheManager.setCache(cacheType, cacheKey, result);
        return result;
    }

    /**
     * 定义流程模型时，获取候选人选择列表
     * @return 返回所有业务涉及到的可选候选人信息
     */
    public Object getCandidateList() {
        return userChoose.getCandidateList();
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
            ModelCheckUtils.validateModel(config.getInitiatorFlag(), form.getNodes(), form.getEdges(), nodes, edges);
            //创建模型
            version = versionRepository.createVersion(nodes, edges,
                    form.getProcessName(), form.getIterateFrom(), form.getCycle(), form.getTerminatedMethod(),
                    form.getCreateBy(), form.getCreateByName(), LocalDateTime.now()
            );

            //删除迭代树缓存
            deleteIterateTreeCache(form.getProcessName());
        } finally {
            lockManager.releaseLock(form.getProcessName(), getLock, lockEnum);
        }

        log.info("{} 创建新版本成功，版本号{}", form.getProcessName(), version);
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
                .collect(Collectors.toMap(IterateTreeNode::version, Function.identity()));

        //返回的结果，顶层node列表
        List<IterateTreeNode> topNodes = new ArrayList<>();

        //循环填充迭代
        for (IterateTreeNode node : treeNodes) {
            if (node.top()) {
                topNodes.add(node);
            }
            if (CollectionUtils.isEmpty(node.iterateVersion())) {
                continue;
            }
            for (Integer version : node.iterateVersion()) {
                node.iterate().add(map.get(version));
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
        cacheManager.deleteCache(CacheEnums.V_I_T.getType(), cacheKeys);
    }

    /**
     * 组件模型初始化
     * @return Map<String, Object>
     */
    private Map<String, Object> componentModelInit() {
        Map<String, Object> map = NodeTemplateUtils.templateInit();
        map.put("条件关系",new ProcessNodeEdge());
        return map;
    }

}
