package com.nf.neoflow.service;

import com.nf.neoflow.component.BaseUserChoose;
import com.nf.neoflow.constants.QueryForOperatorType;
import com.nf.neoflow.dto.query.QueryForOperatorDto;
import com.nf.neoflow.dto.query.QueryForOperatorForm;
import com.nf.neoflow.repository.ProcessRepository;
import com.nf.neoflow.utils.PageUtils;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 查询服务
 */
@Service
@AllArgsConstructor
public class QueryService {

    private final BaseUserChoose userChoose;
    private final ProcessRepository processRepository;

    /**
     * 查询 发起 / 待办 / 已办 列表
     * @param form 表单
     * @return Page<QueryForOperatorDto>
     */
    public Page<QueryForOperatorDto> queryForOperator(QueryForOperatorForm form) {
        Integer queryType = form.getType();
        String query = userChoose.getCandidateRange(form.getUserId(), form.getUsername(), queryType);

        Pageable pageable;

        if (QueryForOperatorType.PENDING.equals(queryType)) {
            pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "updateTime", form.getDesc());
            return processRepository.queryForOperatorPending(form.getName(), form.getVersion(), form.getBusinessKey(), query, pageable);
        } else if (QueryForOperatorType.INITIATE.equals(queryType)) {
            pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "initiateTime", form.getDesc());
            return processRepository.queryForOperatorInitiate(form.getName(), form.getVersion(), form.getBusinessKey(), query, form.getInstanceStatus(), pageable);
        }

        pageable = PageUtils.initPageable(form.getPageNumber(), form.getPageSize(), "doneTime", form.getDesc());
        return processRepository.queryForOperatorDone(form.getName(), form.getVersion(), form.getBusinessKey(), query, form.getNodeStatus(), pageable);
    }



}
