package com.faturaocr.interfaces.rest.notification;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faturaocr.infrastructure.persistence.notification.NotificationRepository;
import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.enums.NotificationType;
import com.faturaocr.domain.notification.enums.NotificationSeverity;

import java.time.Instant;
import java.util.UUID;

class NotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    @Autowired
    private NotificationRepository notificationRepository;

    private Company testCompany;
    private String authToken;
    private com.faturaocr.domain.user.entity.User user;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("Notify Test Company", "1231231234");
        user = testDataSeeder.seedUser(testCompany.getId(), "notify@test.com", "Password123!", Role.MANAGER);
        authToken = testDataSeeder.loginAndGetToken(mockMvc, "notify@test.com", "Password123!");

        // Seed some notifications directly
        createNotification(user.getId(), "Test Title 1", "Message 1", false);
        createNotification(user.getId(), "Test Title 2", "Message 2", true);
    }

    private void createNotification(UUID userId, String title, String message, boolean read) {
        Notification entity = Notification.builder()
                .userId(userId)
                .companyId(testCompany.getId())
                .title(title)
                .message(message)
                .type(NotificationType.SYSTEM_ANNOUNCEMENT)
                .severity(NotificationSeverity.INFO)
                .isRead(read)
                .readAt(read ? Instant.now() : null)
                .build();

        notificationRepository.save(entity);
    }

    @Test
    void getNotifications_ShouldReturnUserNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void getUnreadCount_ShouldReturnCorrectCount() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1)); // Only 1 is unread
    }
}
