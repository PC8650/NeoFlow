package org.nf.neoflow.exception.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nf.neoflow.dto.response.Result;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * @author PC8650
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleProcessException(Exception ex) {
        ErrorResponse errorResponse;
        if (ex instanceof MethodArgumentNotValidException e) {
            errorResponse = new ErrorResponse(e.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(";")));
        }else {
            errorResponse = new ErrorResponse(ex.getMessage());
        }
        log.error(ex.getMessage(),ex);

        return new ResponseEntity<>(Result.fail(errorResponse), HttpStatus.BAD_REQUEST);
    }

    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private String message;
    }

}
