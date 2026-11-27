package com.tenpearls.contactmanagement.common.exception;

import com.tenpearls.contactmanagement.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/test");
        request = mockRequest;
    }

    @Test
    void illegalArgumentResponseDoesNotExposeExceptionMessage() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("sensitive detail"), request);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("The request contains invalid values.", body.getMessage());
    }

    @Test
    void returnValueValidationFailureRemainsInternalServerError() {
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        when(exception.isForReturnValue()).thenReturn(true);

        ResponseEntity<ApiErrorResponse> response = handler.handleMethodValidation(exception, request);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred while processing your request.",
                body.getMessage());
    }

    @Test
    void inputValidationFailureRemainsBadRequest() {
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        when(exception.isForReturnValue()).thenReturn(false);

        ResponseEntity<ApiErrorResponse> response = handler.handleMethodValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void missingRequiredParameterKeepsBadRequestStatus() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("id", "String");

        ResponseEntity<ApiErrorResponse> response = handler.handleMvcException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void unsupportedMethodKeepsStatusAndAllowHeader() {
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

        ResponseEntity<ApiErrorResponse> response = handler.handleMvcException(exception, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(List.of("GET"), response.getHeaders().get(HttpHeaders.ALLOW));
    }

    @Test
    void unsupportedMediaTypeKeepsStatusAndAcceptHeader() {
        HttpMediaTypeNotSupportedException exception =
                new HttpMediaTypeNotSupportedException(
                        MediaType.APPLICATION_JSON,
                        new ArrayList<>(List.of(MediaType.APPLICATION_XML)));

        ResponseEntity<ApiErrorResponse> response = handler.handleMvcException(exception, request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals(List.of(MediaType.APPLICATION_XML), response.getHeaders().getAccept());
    }
}
