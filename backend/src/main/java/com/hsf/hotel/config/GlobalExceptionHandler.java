package com.hsf.hotel.config;

import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.RateLimitExceededException;
import com.hsf.hotel.observability.RequestIdFilter;
import com.hsf.hotel.security.Sanitizers;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts every exception thrown inside {@code com.hsf.hotel.*} controllers
 * to the uniform {@link ApiResponse} envelope. The error body intentionally
 * omits stack traces, internal types, and database driver messages — every
 * public response carries only the safe fields.
 *
 * <p>Internal diagnostics (full stack traces, exception classes) are still
 * logged server-side with the {@code requestId} from MDC so support can
 * correlate them to a single client report.
 */
@RestControllerAdvice(basePackages = "com.hsf.hotel")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException e, HttpServletRequest req) {
        logWarn(req, e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + Sanitizers.safeForLog(fe.getDefaultMessage()))
                .collect(Collectors.toList());
        logWarn(req, "ValidationException", "Method argument validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCodes.VALIDATION_FAILED, "Validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest req) {
        List<String> details = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + Sanitizers.safeForLog(v.getMessage()))
                .collect(Collectors.toList());
        logWarn(req, "ConstraintViolationException", "Constraint violation");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCodes.VALIDATION_FAILED, "Validation failed", details));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e, HttpServletRequest req) {
        logWarn(req, e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCodes.BAD_REQUEST, "Bad request"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e, HttpServletRequest req) {
        logWarn(req, "BadCredentialsException", "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCodes.INVALID_CREDENTIALS, "Invalid username or password"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException e, HttpServletRequest req) {
        logWarn(req, "AuthenticationException", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCodes.UNAUTHORIZED, "Authentication required"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest req) {
        logWarn(req, "AccessDeniedException", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCodes.FORBIDDEN, "You do not have permission to perform this action"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException e, HttpServletRequest req) {
        logWarn(req, "EntityNotFoundException", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCodes.RESOURCE_NOT_FOUND, "Resource not found"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest req) {
        Throwable root = e.getMostSpecificCause();
        logWarn(req, "DataIntegrityViolationException", root != null ? root.getMessage() : e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCodes.CONFLICT, "Operation conflicts with current data"));
    }

    @ExceptionHandler({OptimisticLockException.class, PessimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleLockingException(Exception e, HttpServletRequest req) {
        logWarn(req, e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCodes.CONCURRENCY_CONFLICT,
                        "The resource was modified by another transaction"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException e, HttpServletRequest req) {
        logWarn(req, "MaxUploadSizeExceededException", "File too large");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(ErrorCodes.FILE_TOO_LARGE, "File exceeds the maximum upload size"));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(RateLimitExceededException e, HttpServletRequest req) {
        logWarn(req, "RateLimitExceededException", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(ErrorCodes.RATE_LIMIT_EXCEEDED, "Too many requests"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        logWarn(req, "MethodNotAllowed", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ErrorCodes.METHOD_NOT_ALLOWED, "HTTP method is not supported for this resource"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        logWarn(req, "UnsupportedMediaType", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(ErrorCodes.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException e, HttpServletRequest req) {
        logError(req, "RuntimeException", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCodes.INTERNAL_ERROR, "An unexpected error occurred"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest req) {
        logError(req, "Exception", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCodes.INTERNAL_ERROR, "An unexpected error occurred"));
    }

    private void logWarn(HttpServletRequest req, String type, String message) {
        log.warn("[{}] {} {} -> {}: {}", reqId(), req.getMethod(), req.getRequestURI(),
                type, Sanitizers.safeForLog(message));
    }

    private void logError(HttpServletRequest req, String type, String message, Throwable ex) {
        log.error("[{}] {} {} -> {}: {}", reqId(), req.getMethod(), req.getRequestURI(),
                type, Sanitizers.safeForLog(message), ex);
    }

    private static String reqId() {
        String id = MDC.get(RequestIdFilter.MDC_KEY);
        return id != null ? id : "-";
    }
}
