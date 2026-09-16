package com.qinghuan.web.advice;

import com.qinghuan.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void handleHandlerMethodValidation_shouldReturnUnifiedInvalidRequestResponse() {
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        ParameterValidationResult result = mock(ParameterValidationResult.class);
        MessageSourceResolvable error = mock(MessageSourceResolvable.class);
        when(error.getDefaultMessage()).thenReturn("手机号格式不正确");
        when(result.getResolvableErrors()).thenReturn(List.of(error));
        when(exception.getParameterValidationResults()).thenReturn(List.of(result));

        ResponseEntity<ApiResponse<Void>> response =
                new GlobalExceptionHandler().handleHandlerMethodValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("手机号格式不正确", response.getBody().message());
    }
}
