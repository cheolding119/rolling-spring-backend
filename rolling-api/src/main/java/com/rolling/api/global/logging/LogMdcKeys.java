package com.rolling.api.global.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogMdcKeys {

    public static final String DEFAULT_VALUE = "-";

    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String METHOD = "method";
    public static final String PATH = "path";
    public static final String STATUS = "status";
    public static final String ERROR_CODE = "errorCode";
    public static final String DOMAIN_ID = "domainId";

    public static void initializeRequestContext(String requestId, String traceId, String method, String path) {
        MDC.put(REQUEST_ID, requestId);
        MDC.put(TRACE_ID, traceId);
        MDC.put(USER_ID, DEFAULT_VALUE);
        MDC.put(METHOD, method);
        MDC.put(PATH, path);
        MDC.put(STATUS, DEFAULT_VALUE);
        MDC.put(ERROR_CODE, DEFAULT_VALUE);
        MDC.put(DOMAIN_ID, DEFAULT_VALUE);
    }

    public static void clearRequestContext() {
        MDC.remove(REQUEST_ID);
        MDC.remove(TRACE_ID);
        MDC.remove(USER_ID);
        MDC.remove(METHOD);
        MDC.remove(PATH);
        MDC.remove(STATUS);
        MDC.remove(ERROR_CODE);
        MDC.remove(DOMAIN_ID);
    }
}
