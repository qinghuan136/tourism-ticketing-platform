package com.qinghuan.booking;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;

/** 远程服务熔断或不可达时返回给订单编排层的明确异常。 */
public class RemoteServiceUnavailableException extends BusinessException {

    public RemoteServiceUnavailableException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public RemoteServiceUnavailableException(String message, Throwable cause) {
        this(message);
        initCause(cause);
    }
}
