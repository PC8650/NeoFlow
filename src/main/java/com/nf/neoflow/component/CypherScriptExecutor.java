package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * cypher 脚本执行器，启动后执行脚本创建约束和索引
 * @author PC8650
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CypherScriptExecutor {

    private final Driver driver;

    private final ResourceLoader resourceLoader;

    private final NeoFlowConfig config;

    @PostConstruct
    private void start() throws Exception {
        log.info("===CypherScriptExecutor  Start===");
        String constraintAndIndexFile = config.getConstraintAndIndexFile();
        //约束和普通索引
        execute(constraintAndIndexFile);
        //全文索引脚本, 由于全文索引名称在查询时需要使用，所以不支持自定义
        execute("classpath:cypher/fullTextIndex.cypher");
        log.info("===CypherScriptExecutor  end===");
    }

    /**
     * 读取脚本文件并执行
     * @param scriptFileClassPath 脚本文件类路径
     * @throws Exception
     */
    private void execute(String scriptFileClassPath) throws Exception{
        log.info("读取：{}", scriptFileClassPath);
        Resource resource = resourceLoader.getResource(scriptFileClassPath);
        List<String> cyphers;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
             cyphers = reader.lines().filter(c -> c.startsWith("create") || c.startsWith("CREATE")).toList();
            log.info("读取完成：{}", scriptFileClassPath);
        } catch (Exception e) {
            log.error(String.format("读取失败：%s", scriptFileClassPath), e);
           throw e;
        }

        log.info("执行：{}", scriptFileClassPath);
        try {
           executeScript(cyphers);
           log.info("执行完成：{}", scriptFileClassPath);
        }catch (Exception e) {
            log.error(String.format("执行失败：%s", scriptFileClassPath), e);
            throw e;
        }
    }

    /**
     * 执行脚本
     * @param cyphers 一个文件的cypher语句
     */
    private void executeScript(List<String> cyphers) {
        try (Session session = driver.session()) {
            for (String cypher : cyphers) {
                session.run(cypher);
            }
        }
    }

}
