package com.nf.neoflow.dto.response;

/**
 * 响应结果
 * @param <T>
 */
public record Result<T>(T data,String msg){
    public static <T> Result<T> success(T data){
        return new Result<>(data,"success");
    }

    public static <T> Result<T> success(){
        return new Result<>(null,"success");
    }

    public static <T> Result<T> fail(T data){
        return new Result<>(data,"fail");
    }

}
