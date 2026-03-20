package com.rolling.api.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class RequestTrackingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TRACE_PARENT_HEADER = "traceparent";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveIncomingTraceId(request);
        String requestId = StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString();

        LogMdcKeys.initializeRequestContext(
                requestId,
                requestId,
                request.getMethod(),
                request.getRequestURI()
        );
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(TRACE_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put(LogMdcKeys.STATUS, Integer.toString(response.getStatus()));
            log.info("HTTP request completed");
            LogMdcKeys.clearRequestContext();
        }
    }

    private String resolveIncomingTraceId(HttpServletRequest request) {
        String requestIdHeader = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestIdHeader)) {
            return requestIdHeader;
        }

        String traceIdHeader = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(traceIdHeader)) {
            return traceIdHeader;
        }

        String correlationIdHeader = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(correlationIdHeader)) {
            return correlationIdHeader;
        }

        String traceParent = request.getHeader(TRACE_PARENT_HEADER);
        if (!StringUtils.hasText(traceParent)) {
            return null;
        }

        String[] segments = traceParent.split("-");
        if (segments.length >= 4 && StringUtils.hasText(segments[1])) {
            return segments[1];
        }
        return null;
    }
}
