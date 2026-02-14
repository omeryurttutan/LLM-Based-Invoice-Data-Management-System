package com.faturaocr.application.user;

import com.faturaocr.application.user.dto.ChangePasswordCommand;
import com.faturaocr.application.user.dto.UpdateProfileCommand;
import com.faturaocr.application.user.dto.UserProfileResponse;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashedPwd")
                .role(Role.ACCOUNTANT)
                .build();
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileCommand command = UpdateProfileCommand.builder()
                .fullName("New Name")
                .phone("123456")
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = profileService.updateProfile(user.getId(), command);

        assertEquals("New Name", user.getFullName());
        assertEquals("123456", user.getPhone());
    }

    @Test
    void changePassword_Success() {
        ChangePasswordCommand command = ChangePasswordCommand.builder()
                .currentPassword("OldPass")
                .newPassword("NewPass")
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass", "hashedPwd")).thenReturn(true);
        when(passwordEncoder.matches("NewPass", "hashedPwd")).thenReturn(false);
        when(passwordEncoder.encode("NewPass")).thenReturn("newHashed");

        profileService.changePassword(user.getId(), command);

        verify(userRepository).save(user);
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordCommand command = ChangePasswordCommand.builder()
                .currentPassword("WrongPass")
                .newPassword("NewPass")
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashedPwd")).thenReturn(false);

        assertThrows(DomainException.class, () -> profileService.changePassword(user.getId(), command));
    }
}
