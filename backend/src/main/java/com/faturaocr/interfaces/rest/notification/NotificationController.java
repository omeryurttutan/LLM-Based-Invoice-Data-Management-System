package com.faturaocr.interfaces.rest.notification;

import com.faturaocr.domain.notification.service.NotificationService;
import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user notifications", description = "Retrieve paginated notifications for the current user")
    @ApiResponse(responseCode = "200", description = "List of notifications retrieved")
    public ResponseEntity<Page<Notification>> getNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Parameter(description = "Filter by read status (true/false)") @RequestParam(required = false) Boolean isRead,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // Handle null isRead logic
        if (isRead == null) {
            return ResponseEntity.ok(notificationService.getAllUserNotifications(currentUser.userId(), pageable));
        } else {
            return ResponseEntity.ok(notificationService.getUserNotifications(currentUser.userId(), isRead, pageable));
        }
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread count", description = "Get the number of unread notifications")
    @ApiResponse(responseCode = "200", description = "Unread count retrieved")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        long count = notificationService.getUnreadCount(currentUser.userId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark as read", description = "Mark a specific notification as read")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Void> markAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {

        notificationService.markAsRead(id, currentUser.userId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all as read", description = "Mark all notifications for the current user as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        notificationService.markAllAsRead(currentUser.userId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete notification", description = "Delete a specific notification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Void> deleteNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {

        notificationService.deleteNotification(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
