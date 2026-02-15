package com.faturaocr.domain.notification.service;

import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.enums.NotificationReferenceType;
import com.faturaocr.domain.notification.enums.NotificationType;
import com.faturaocr.domain.notification.service.channel.EmailNotificationChannel;
import com.faturaocr.domain.notification.service.channel.InAppNotificationChannel;
import com.faturaocr.domain.notification.service.channel.NotificationChannel;
import com.faturaocr.domain.notification.service.channel.PushNotificationChannel;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.infrastructure.persistence.notification.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private InAppNotificationChannel inAppChannel;
    @Mock
    private EmailNotificationChannel emailChannel;
    @Mock
    private PushNotificationChannel pushChannel;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        List<NotificationChannel> channels = new ArrayList<>();
        channels.add(inAppChannel);
        channels.add(emailChannel);
        channels.add(pushChannel);

        when(inAppChannel.getChannelName()).thenReturn("in_app");
        when(emailChannel.getChannelName()).thenReturn("email");
        when(pushChannel.getChannelName()).thenReturn("push");

        when(inAppChannel.supports(any())).thenReturn(true);
        when(emailChannel.supports(any())).thenReturn(true);
        when(pushChannel.supports(any())).thenReturn(true);

        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                channels,
                preferenceService);
    }

    @Test
    void notify_ShouldDispatchToEnabledChannels() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        NotificationType type = NotificationType.EXTRACTION_COMPLETED;

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock preferences: InApp=true, Email=true, Push=false
        when(preferenceService.isChannelEnabled(userId, type, "in_app")).thenReturn(true);
        when(preferenceService.isChannelEnabled(userId, type, "email")).thenReturn(true);
        when(preferenceService.isChannelEnabled(userId, type, "push")).thenReturn(false);

        notificationService.notify(userId, companyId, type, "Title", "Message",
                NotificationReferenceType.INVOICE, UUID.randomUUID(), null);

        verify(inAppChannel, times(1)).send(any(Notification.class), any(UUID.class));
        verify(emailChannel, times(1)).send(any(Notification.class), any(UUID.class));
        verify(pushChannel, never()).send(any(Notification.class), any(UUID.class));
    }
}
