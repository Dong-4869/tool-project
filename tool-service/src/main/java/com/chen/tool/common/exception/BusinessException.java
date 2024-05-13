package com.chen.tool.common.exception;

import lombok.Data;

/**
 * 业务异常
 * @author 13103
 */
@Data
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    protected final Integer errorCode;

    /**
     * 错误信息
     */
    protected final String errorMsg;


    public BusinessException(Integer errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }



    @Override
    public String getMessage() {
        return errorMsg;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
