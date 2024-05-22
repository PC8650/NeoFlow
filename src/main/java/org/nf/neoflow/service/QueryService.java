package org.nf.neoflow.service;

import lombok.AllArgsConstructor;
import org.nf.neoflow.component.BaseUserChoose;
import org.nf.neoflow.component.NeoCacheManager;
import org.nf.neoflow.constants.QueryForOperatorType;
import org.nf.neoflow.dto.query.*;
import org.nf.neoflow.enums.CacheEnums;
import org.nf.neoflow.repository.QueryRepository;
import org.nf.neoflow.utils.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询服务
 */
@Service
@AllArgsConstructor
public class QueryService {

    private final BaseUserChoose userChoose;
    private final NeoCacheManager cacheManager;
    private final QueryRepository queryRepository;

    /**
     * 查询当前用户在各流程的待办数量
     * @param form 查询表单
     */
    public List<OperatorOfPendingDto> queryForOperatorOfPending(OperatorOfPendingForm form){
        String query = userChoose.getCandidateRange(form.userId(), form.username(), QueryForOperatorType.PENDING);
        return queryRepository.queryForOperatorOfPending(form.name(), form.version(), query);
    }

    /**
     * 查询当前用户 发起 / 待办 / 已办 列表
     * @param form 表单
     * @return Page<QueryForOperatorDto>
     */
    public Page<QueryForOperatorDto> queryForOperator(QueryForOperatorForm form) {
        Integer queryType = form.getType();
        String query = userChoose.getCandidateRange(form.getUserId(), form.getUsername(), queryType);

        Pageable pageable;

        if (QueryForOperatorType.PENDING.equals(queryType)) {
            pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "updateTime", form.getDesc());
            return queryRepository.queryForOperatorPending(form.getName(), form.getVersion(), form.getBusinessKey(), query, pageable);
        } else if (QueryForOperatorType.INITIATE.equals(queryType)) {
            pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "initiateTime", form.getDesc());
            return queryRepository.queryForOperatorInitiate(form.getName(), form.getVersion(), form.getBusinessKey(), query, form.getInstanceStatus(), pageable);
        }

        pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "doneTime", form.getDesc());
        return queryRepository.queryForOperatorDone(form.getName(), form.getVersion(), form.getBusinessKey(), query, form.getNodeStatus(), pageable);
    }

    /**
     * 通过节点 名称/身份 查询节点状态
     * 只能查询在候选人范围内的节点
     * @param form 表单
     */
    public Page<QueryOfNodeIdentityDto> queryOfNodeIdentity(QueryOfNodeIdentityForm form) {
        String query = userChoose.getCandidateRange(form.getUserId(), form.getUsername(), QueryForOperatorType.PENDING);
        Pageable pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "endTime", form.getDesc());

        if (form.getQueryType() == 1) {
            return queryRepository.queryOfNodeIdentityPending(form.getName(), form.getVersion(), form.getBusinessKey(),
                    form.getNodeName(), form.getNodeIdentity(), query, pageable);
        }

        return queryRepository.queryOfNodeIdentityDone(form.getName(), form.getVersion(), form.getBusinessKey(),
                form.getNodeName(), form.getNodeIdentity(), form.getNodeStatus(), query, pageable);
    }

    /**
     * 查询流程实例操作历史
     * @param businessKey 业务key
     * @param num 截至到 第 num 个节点
     * @return List<OperationHistoryDto>
     */
    public List<OperationHistoryDto> queryInstanceOperationHistory(String businessKey, Integer num) {
        String cacheType = CacheEnums.I_O_H.getType();
        String key;
        if (num == null) {
            key = businessKey;
        }else {
            key = cacheManager.mergeKey(businessKey, num.toString());
        }
        NeoCacheManager.CacheValue<List> cache = cacheManager.getCache(cacheType, key, List.class);
        List<OperationHistoryDto> historyList;
        if (cache.filter() || cache.value() != null) {
            historyList =  cache.value();
        } else {
            historyList = queryRepository.queryInstanceOperationHistory(businessKey, num);
            cacheManager.setCache(cacheType, key, historyList);
        }
        return historyList;
    }

}
