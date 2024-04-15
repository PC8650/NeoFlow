package com.nf.neoflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注一个类表示用来进行流程业务操作
 * @author PC8650
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProcessOperator {

    /**
     * 在整个项目中，流程名称应该唯一
     * @return 流程名称
     */
    String name() default "";

}
