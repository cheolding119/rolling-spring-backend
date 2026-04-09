package com.rolling.api.global.alert;

import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerAlertTest {

    private final OperationalAlertPublisher operationalAlertPublisher = Mockito.mock(OperationalAlertPublisher.class);
    private final ObjectProvider<OperationalAlertPublisher> alertPublisherProvider = Mockito.mock(ObjectProvider.class);
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(alertPublisherProvider);

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("예상치 못한 예외는 Slack alert publisher를 호출한다")
    void handleException_publishesUnexpectedExceptionAlert() {
        when(alertPublisherProvider.getIfAvailable()).thenReturn(operationalAlertPublisher);
        MDC.put("requestId", "req-1");

        globalExceptionHandler.handleException(new IllegalStateException("boom"));

        verify(operationalAlertPublisher).publishUnexpectedException(org.mockito.ArgumentMatchers.any(IllegalStateException.class));
    }

    @Test
    @DisplayName("외부 API 예외는 Slack alert publisher를 호출한다")
    void handleRestClientException_publishesExternalApiAlert() {
        when(alertPublisherProvider.getIfAvailable()).thenReturn(operationalAlertPublisher);

        globalExceptionHandler.handleRestClientException(new RestClientException("gateway timeout"));

        verify(operationalAlertPublisher).publishExternalApiFailure(
                org.mockito.ArgumentMatchers.any(RestClientException.class),
                org.mockito.ArgumentMatchers.eq("GlobalExceptionHandler")
        );
    }

    @Test
    @DisplayName("BusinessException은 Slack alert publisher를 호출하지 않는다")
    void handleBusinessException_doesNotPublishAlert() {
        when(alertPublisherProvider.getIfAvailable()).thenReturn(operationalAlertPublisher);

        globalExceptionHandler.handleBusinessException(
                new BusinessException("FORBIDDEN", "forbidden", HttpStatus.FORBIDDEN)
        );

        verify(operationalAlertPublisher, never()).publishUnexpectedException(org.mockito.ArgumentMatchers.any());
        verify(operationalAlertPublisher, never()).publishExternalApiFailure(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }
}
