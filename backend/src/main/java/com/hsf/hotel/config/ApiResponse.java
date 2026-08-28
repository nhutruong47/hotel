package com.hsf.hotel.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uniform API envelope. Always returns either {@code data} or {@code error}
 * at the top level alongside the {@code meta} object that includes a
 * server-side timestamp and the request id (when set by
 * {@link com.hsf.hotel.observability.RequestIdFilter}).
 */
@JsonInclude(Include.NON_NULL)
public final class ApiResponse {

    private final Object data;
    private final ErrorBody error;
    private final Map<String, Object> meta;

    private ApiResponse(Object data, ErrorBody error, Map<String, Object> meta) {
        this.data = data;
        this.error = error;
        this.meta = meta;
    }

    public static ApiResponse ok(Object data) {
        return new ApiResponse(data, null, Meta.now());
    }

    public static ApiResponse error(String code, String message) {
        return new ApiResponse(null, new ErrorBody(code, message, null), Meta.now());
    }

    public static ApiResponse error(String code, String message, List<String> details) {
        return new ApiResponse(null, new ErrorBody(code, message, details), Meta.now());
    }

    public Object getData() {
        return data;
    }

    public ErrorBody getError() {
        return error;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    @JsonInclude(Include.NON_NULL)
    public record ErrorBody(String code, String message, List<String> details) {}

    static final class Meta {
        private Meta() {}
        static Map<String, Object> now() {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("timestamp", Instant.now().toString());
            String requestId = org.slf4j.MDC.get(com.hsf.hotel.observability.RequestIdFilter.MDC_KEY);
            if (requestId != null && !requestId.isBlank()) {
                meta.put("requestId", requestId);
            }
            return meta;
        }
    }
}