package org.nf.neoflow.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.config.NeoRemoteConfig;
import org.nf.neoflow.dto.execute.ExecuteForm;
import org.nf.neoflow.dto.response.Result;
import org.nf.neoflow.exception.NeoExecuteException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * 远程操作管理器，用于远程调用 component 模块中的 RemoteOperateController
 * @author PC8650
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "neo.independence", havingValue = "true", matchIfMissing = false)
public class RemoteOperatorManager extends AbstractOperatorManager{

    private final RestTemplate restTemplate;

    private final NeoRemoteConfig remoteConfig;

    private final String URI = "/neo/remote/execute";

    /**
     * 执行节点方法
     * @param form 表单
     * @return ExecuteForm
     */
    @Override
    public ExecuteForm operate(ExecuteForm form) {
        String addr = getAddr(form.getGroup());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        HttpEntity<ExecuteForm> requestEntity = new HttpEntity<>(form, headers);

        Result<ExecuteForm> r;
        try {
            ResponseEntity<Result<ExecuteForm>> response = restTemplate.exchange(
                    addr,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<ExecuteForm>>(){}
            );
            r = response.getBody();
        } catch (Exception e) {
            log.error("流程执行失败，节点方法远程调用失败：流程 {}-版本 {}-key {}-当前节点位置 {}-error {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum(), e.getMessage());
            throw new NeoExecuteException("流程执行失败，节点方法远程调用失败");
        }


        if (Objects.isNull(r)) {
            log.error("流程执行失败，节点方法无响应：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程执行失败，节点方法无响应");
        }

        if (!r.isSuccess()) {
            log.error("流程执行失败，节点方法远程执行失败：流程 {}-版本 {}-key {}-当前节点位置 {}-error {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum(), r.msg());
            throw new NeoExecuteException("流程执行失败，节点方法远程执行失败");
        }

        if (Objects.isNull(r.data())) {
            log.error("流程执行失败，节点方法远程未返回执行表单：流程 {}-版本 {}-key {}-当前节点位置 {}",
                    form.getProcessName(), form.getVersion(), form.getBusinessKey(), form.getNum());
            throw new NeoExecuteException("流程执行失败，节点方法远程未返回执行表单");
        }

        return r.data();
    }

    /**
     * 获取分组对应的地址
     * @param group 分组信息
     * @return addr
     */
    private String getAddr(String group) {
        if (StringUtils.isBlank(group)) throw new NeoExecuteException("流程执行失败, 分组信息为空");

        String addr = remoteConfig.getGroup().get(group);

        if (StringUtils.isBlank(addr)) throw new NeoExecuteException(String.format("流程执行失败，未找到分组[%s]配置", group));

        return addr.concat(URI);
    }

}
