package com.nf.neoflow.models;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Instance 流程版本实例
 * @author PC8650
 */
@Data
@Node
public class Instance {

    @Id
    @GeneratedValue
    private Long id;

}
