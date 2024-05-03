package com.nf.neoflow.exception;

/**
 * 流程异常
 * @author PC8650
 */
public class NeoProcessException extends RuntimeException {

    private final static String DUPLICATE_NAME = "流程名称重复：[%s]";

    public NeoProcessException(String message) {
        super(message);
    }

    /**
     * 流程名称重复
     * @param name 流程名称
     * @return 流程异常
     */
    public static NeoProcessException duplicateName (String name) {
        return new NeoProcessException(String.format(DUPLICATE_NAME, name));
    }

}
