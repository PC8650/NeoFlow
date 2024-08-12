package org.nf.neoflow.repository;

import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.models.Process;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * 创建Demo流程
 * @author PC8650
 */
@ConditionalOnProperty(value = "neo.demo", havingValue = "true", matchIfMissing = false)
public interface DemoRepository extends Neo4jRepository<Process,Long> {

    /**
     * 一个任何人可发起，无方法，且操作类型(operationType)由发起人审核(发起人审核标识读取配置: {@link  NeoFlowConfig#getInitiatorFlag()})的示范流程模型
     * 该流程拥有3条中间节点路径，其中 'm1-1', 'm2-2', 'm2-3' 允许拒绝驳回
     */
    @Query("""
        optional match (f:Process{`name`:'demo'})
        with timestamp() as time where f is null
        with time, datetime({epochSeconds: time/1000}) as date, apoc.date.format(time) as df
    
        create (p:Process)-[vb:VERSION]->(v:Version)-[:INSTANCE]->(:Instance{`name`:'instance'})
        set p.`name` = 'demo', p.group = 'demo', p.`active` = true, p.activeVersion = 1,
        p.createBy = '-1', p.updateBy = '-1', p.createTime = date, p.updateTime = date,
        p.activeHistory = [apoc.convert.toJson({
            version: 1,
            activeId: '-1',
            activeName: 'Demo',
            activeTime: df
        })],
        v.version = 1, v.createBy = '-1', v.createByName = 'Demo', v.terminatedMethod = ''
    
        create (p)-[a:ACTIVE{createBy:'-1', createTime:date}]->(v)
    
        with v
    
        create (v)-[:MODEL]->(s:ModelNode)-[:NEXT{condition:1}]->(m1:ModelNode)-[:NEXT{condition:1}]->(m21:ModelNode)-[:NEXT{condition:1}]->(c:ModelNode)
        set s.autoInterval = 0, s.conditionByMethod = false, s.identity = 'start', s.location = 1, s.`name` = 'begin', s.nodeUid = '1', s.onlyPassExecute = true,
        s.operationCandidateInfo = [], s.operationMethod = '', s.operationType = $0,
        c.autoInterval = 0, c.conditionByMethod = false, c.identity = 'complete', c.location = 3, c.`name` = 'complete', c.nodeUid = '7', c.onlyPassExecute = true,
        c.operationCandidateInfo = [], c.operationType = $0,
        m1.conditionByMethod = true, m1.identity = 'middle1-1', m1.location = 2, m1.`name` = 'm1-1', m1.nodeUid = '2', m1.onlyPassExecute = true,
        m1.operationCandidateInfo = [], m1.operationMethod = '', m1.operationType = $0,
        m21.conditionByMethod = false, m21.identity = 'middle2-1', m21.location = 2, m21.`name` = 'm2-1', m21.nodeUid = '3', m21.onlyPassExecute = true,
        m21.operationCandidateInfo = [], m21.operationMethod = '', m21.operationType = $0
    
        create (m21)<-[:NEXT{condition:1}]-(m22:ModelNode)<-[:NEXT{condition:2}]-(m1)-[:NEXT{condition:3}]->(m23:ModelNode)-[:NEXT{condition:1}]->(m21)
        set m22.conditionByMethod = false, m22.identity = 'middle2-2', m22.location = 2, m22.`name` = 'm2-2', m22.nodeUid = '4', m22.onlyPassExecute = true,
        m22.operationCandidateInfo = [], m22.operationMethod = '', m22.operationType = $0,
        m23.conditionByMethod = false, m23.identity = 'middle2-3', m23.location = 2, m23.`name` = 'm2-3', m23.nodeUid = '5', m23.onlyPassExecute = true,
        m23.operationCandidateInfo = [], m23.operationMethod = '', m23.operationType = $0
    
        with m1, m22, m23
    
        create (m1)-[:NEXT]->(t:ModelNode)
        set t.autoInterval = 0, t.conditionByMethod = false, t.identity = 'terminate', t.location = 4, t.`name` = 'finish', t.nodeUid = '6', t.onlyPassExecute = true,
        t.operationCandidateInfo = [], t.operationType = $0
        create (m22)-[:NEXT]->(t)
        create (m23)-[:NEXT]->(t)
    """)
    void createDemo(int initiatorFlag);
}
