package org.nf.neoflow.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.repository.DemoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * demo 创建
 * @author PC8650
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "neo.demo", havingValue = "true", matchIfMissing = false)
public class DemoCreator {

    private final NeoFlowConfig config;
    private final DemoRepository demoRepository;

    @PostConstruct
    public void createDemo() {
        int initiatorFlag = config.getInitiatorFlag();
        log.info("创建demo流程, initiatorFlag：{}", initiatorFlag);
        demoRepository.createDemo(initiatorFlag);
        log.info("创建demo流程完成");
    }

}
