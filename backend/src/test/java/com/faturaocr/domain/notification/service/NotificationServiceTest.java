package com.faturaocr.domain.notification.service;

import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.enums.NotificationReferenceType;
import com.faturaocr.domain.notification.enums.NotificationSeverity;
import com.faturaocr.domain.notification.enums.NotificationType;
import com.faturaocr.domain.notification.service.channel.NotificationChannel;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.infrastructure.persistence.notification.NotificationRepository;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationPreferenceService preferenceService;

    private NotificationChannel mockChannel;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        mockChannel = mock(NotificationChannel.class);
        List<NotificationChannel> channels = Collections.singletonList(mockChannel);
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                channels,
                preferenceService);
    }

    @Test
    @DisplayName("Should notify and dispatch to enabled channels")
    void shouldNotifyAndDispatchToEnabledChannels() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = TestFixtures.COMPANY_ID;
        NotificationType type = NotificationType.INVOICE_VERIFIED;

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // Mock channel behavior
        when(mockChannel.supports(type)).thenReturn(true);
        when(mockChannel.getChannelName()).thenReturn("email");
        when(preferenceService.isChannelEnabled(userId, type, "email")).thenReturn(true);

        // When
        notificationService.notify(userId, companyId, type, "Title", "Message", NotificationReferenceType.INVOICE,
                UUID.randomUUID(), null);

        // Then
        verify(notificationRepository).save(any(Notification.class));
        try {
            verify(mockChannel).send(any(Notification.class), eq(userId));
        } catch (Exception e) {
            // Should not happen
        }
    }

    @Test
    @DisplayName("Should not dispatch if channel is disabled")
    void shouldNotDispatchIfChannelIsDisabled() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = TestFixtures.COMPANY_ID;
        NotificationType type = NotificationType.INVOICE_VERIFIED;

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        when(mockChannel.supports(type)).thenReturn(true);
        when(mockChannel.getChannelName()).thenReturn("email");
        when(preferenceService.isChannelEnabled(userId, type, "email")).thenReturn(false);

        // When
        notificationService.notify(userId, companyId, type, "Title", "Message", NotificationReferenceType.INVOICE,
                UUID.randomUUID(), null);

        // Then
        verify(notificationRepository).save(any(Notification.class));
        try {
            verify(mockChannel, never()).send(any(Notification.class), eq(userId));
        } catch (Exception e) {
        }
    }

    @Test
    @DisplayName("Should notify company users")
    void shouldNotifyCompanyUsers() {
        // Given
        UUID companyId = TestFixtures.COMPANY_ID;
        User user1 = com.faturaocr.testutil.TestDataBuilder.aUser().withId(UUID.randomUUID()).build();
        User user2 = com.faturaocr.testutil.TestDataBuilder.aUser().withId(UUID.randomUUID()).build();

        when(userRepository.findAllByCompanyId(companyId)).thenReturn(List.of(user1, user2));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.notifyCompany(companyId, NotificationType.INVOICE_REJECTED, "Title", "Message",
                NotificationReferenceType.INVOICE, UUID.randomUUID(), null);

        // Then
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should mark notification as read")
    void shouldMarkNotificationAsRead() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setRead(false);

        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));

        // When
        notificationService.markAsRead(notificationId, userId);

        // Then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should get unread count")
    void shouldGetUnreadCount() {
        // Given
        UUID userId = UUID.randomUUID();
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

        // When
        long count = notificationService.getUnreadCount(userId);

        // Then
        assertThat(count).isEqualTo(5L);
    }
}
