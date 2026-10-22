package com.bach.vetclinic.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ProblemDetail unavailable(org.springframework.web.server.ResponseStatusException e,HttpServletRequest request) {
        return problem(HttpStatus.valueOf(e.getStatusCode().value()),e.getReason(),request);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ProblemDetail authentication(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa", request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ProblemDetail forbidden(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", request);
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Định dạng dữ liệu hoặc tham số không hợp lệ", request);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ProblemDetail integrity(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Dữ liệu trùng lặp, vượt giới hạn hoặc đang được tham chiếu", request);
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Dữ liệu không hợp lệ");
        return problem(HttpStatus.BAD_REQUEST, message, request);
    }

    private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
