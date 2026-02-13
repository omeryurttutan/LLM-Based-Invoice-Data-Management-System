package com.faturaocr.application.user;

import com.faturaocr.application.user.dto.ChangeRoleCommand;
import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    private MockedStatic<CompanyContextHolder> companyContextMock;
    private MockedStatic<SecurityContextHolder> securityContextMock;
    private SecurityContext securityContext;
    private Authentication authentication;

    private UUID companyId;
    private User user;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        companyContextMock = mockStatic(CompanyContextHolder.class);
        companyContextMock.when(CompanyContextHolder::getCompanyId).thenReturn(companyId);

        securityContextMock = mockStatic(SecurityContextHolder.class);
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        securityContextMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        user = User.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashedPwd")
                .role(Role.ACCOUNTANT)
                .build();
    }

    @AfterEach
    void tearDown() {
        companyContextMock.close();
        securityContextMock.close();
    }

    @Test
    void createUser_Success() {
        CreateUserCommand command = CreateUserCommand.builder()
                .email("new@example.com")
                .password("Pass123")
                .fullName("New User")
                .role(Role.ACCOUNTANT)
                .build();

        when(userRepository.existsByEmailAndCompanyId(any(), any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        UserResponse response = userManagementService.createUser(command);

        assertNotNull(response);
        assertEquals(companyId, response.getCompanyId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        CreateUserCommand command = CreateUserCommand.builder().email("test@example.com").build();
        when(userRepository.existsByEmailAndCompanyId(any(), any())).thenReturn(true);

        assertThrows(DomainException.class, () -> userManagementService.createUser(command));
    }

    @Test
    void changeUserRole_Success() {
        AuthenticatedUser authUser = new AuthenticatedUser(UUID.randomUUID(), "admin@example.com", companyId,
                Role.ADMIN.name());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authUser);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        ChangeRoleCommand command = ChangeRoleCommand.builder().role(Role.MANAGER).build();

        userManagementService.changeUserRole(user.getId(), command);

        assertEquals(Role.MANAGER, user.getRole());
    }

    @Test
    void changeUserRole_Self_ThrowsException() {
        AuthenticatedUser authUser = new AuthenticatedUser(user.getId(), "admin@example.com", companyId,
                Role.ADMIN.name());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authUser);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        ChangeRoleCommand command = ChangeRoleCommand.builder().role(Role.MANAGER).build();

        assertThrows(DomainException.class, () -> userManagementService.changeUserRole(user.getId(), command));
    }
}
