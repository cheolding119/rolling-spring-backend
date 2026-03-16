package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.openmat.event.OpenMatDeletedEvent;
import com.rolling.api.domain.openmat.event.OpenMatUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpenMatPushNotificationEventHandlerTest {

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private OpenMatPushNotificationEventHandler handler;

    @Test
    @DisplayName("오픈매트 수정 이벤트는 OPEN_MAT_UPDATED 푸시로 변환된다")
    void handleUpdatedEvent_sendsUpdatedPush() {
        handler.handle(new OpenMatUpdatedEvent(11L, "주말 오픈매트", List.of(2L, 3L)));

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(pushNotificationService).sendToUsers(eq(List.of(2L, 3L)), commandCaptor.capture());

        PushNotificationCommand command = commandCaptor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.OPEN_MAT_UPDATED);
        assertThat(command.targetId()).isEqualTo(11L);
        assertThat(command.data()).containsEntry("openMatId", "11");
    }

    @Test
    @DisplayName("오픈매트 삭제 이벤트는 OPEN_MAT_DELETED 푸시로 변환된다")
    void handleDeletedEvent_sendsDeletedPush() {
        handler.handle(new OpenMatDeletedEvent(12L, "평일 오픈매트", List.of(5L)));

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(pushNotificationService).sendToUsers(eq(List.of(5L)), commandCaptor.capture());

        PushNotificationCommand command = commandCaptor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.OPEN_MAT_DELETED);
        assertThat(command.targetId()).isEqualTo(12L);
        assertThat(command.data()).containsEntry("openMatId", "12");
    }
}
