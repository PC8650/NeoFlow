package org.nf.neoflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配合 {@link ProcessOperator ProcessOperator}，标注方其流程业务需要执行的方法
 * @author PC8650
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProcessMethod {

    /**
     * 一个类中，所有 {@link ProcessMethod ProcessMethod} 的 name 都应保持唯一性
     * @return 定义的方法名称
     */
    String name() default "";

}
