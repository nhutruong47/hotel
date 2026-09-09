package com.hsf.hotel.architecture;

import com.hsf.hotel.config.ApiController;
import com.hsf.hotel.config.ApiPaths;
import com.hsf.hotel.config.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Build-time guardrails for the HTTP contract. A new controller cannot be
 * merged unless it uses the shared marker, versioned route and response envelope.
 */
class ApiContractArchitectureTest {

    @Test
    void everyApiFollowsTheSharedContract() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Class<?> apiClass : apiClasses()) {
            if (!apiClass.getSimpleName().endsWith("Api")) {
                violations.add(apiClass.getName() + " must end with Api");
            }

            RequestMapping mapping = apiClass.getAnnotation(RequestMapping.class);
            if (mapping == null || mapping.value().length == 0) {
                violations.add(apiClass.getName() + " must declare a class-level route");
                continue;
            }

            String basePath = mapping.value()[0];
            boolean assetController = basePath.startsWith("/uploads");
            if (!assetController && !basePath.startsWith(ApiPaths.V1)) {
                violations.add(apiClass.getName() + " must be rooted at " + ApiPaths.V1);
            }
            if (basePath.startsWith(ApiPaths.V1 + "/admin")
                    && apiClass.getAnnotation(PreAuthorize.class) == null) {
                violations.add(apiClass.getName() + " must declare defense-in-depth admin authorization");
            }

            ReflectionUtils.doWithMethods(apiClass, method -> validateMethod(apiClass, method, violations),
                    method -> method.getDeclaredAnnotations().length > 0
                            && org.springframework.core.annotation.AnnotatedElementUtils
                                    .hasAnnotation(method, RequestMapping.class));
        }
        assertTrue(violations.isEmpty(), () -> "API contract violations:\n - " + String.join("\n - ", violations));
    }

    private static void validateMethod(Class<?> apiClass, Method method, List<String> violations) {
        if (!ResponseEntity.class.equals(method.getReturnType())) {
            violations.add(apiClass.getSimpleName() + "." + method.getName()
                    + " must return ResponseEntity");
            return;
        }
        Type returnType = method.getGenericReturnType();
        if (!(returnType instanceof ParameterizedType responseType)) {
            violations.add(apiClass.getSimpleName() + "." + method.getName()
                    + " must parameterize ResponseEntity");
            return;
        }
        Type bodyType = responseType.getActualTypeArguments()[0];
        if (!isEnvelope(bodyType) && !isBinary(bodyType)) {
            violations.add(apiClass.getSimpleName() + "." + method.getName()
                    + " must return ApiResponse<T>; only file downloads may bypass the envelope");
        }
    }

    private static boolean isEnvelope(Type type) {
        if (type.equals(ApiResponse.class)) {
            return true;
        }
        return type instanceof ParameterizedType parameterized
                && parameterized.getRawType().equals(ApiResponse.class);
    }

    private static boolean isBinary(Type type) {
        if (type.equals(byte[].class)) {
            return true;
        }
        if (type instanceof Class<?> bodyClass) {
            return Resource.class.isAssignableFrom(bodyClass);
        }
        return false;
    }

    private static List<Class<?>> apiClasses() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ApiController.class));
        List<Class<?>> classes = new ArrayList<>();
        for (var definition : scanner.findCandidateComponents("com.hsf.hotel")) {
            classes.add(Class.forName(definition.getBeanClassName()));
        }
        return classes;
    }
}
