package com.faturaocr.application.user;

import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.application.user.UserManagementService;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.testutil.TestDataBuilder;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userService;

    private MockedStatic<CompanyContextHolder> companyContextHolderMock;

    @BeforeEach
    void setUp() {
        companyContextHolderMock = Mockito.mockStatic(CompanyContextHolder.class);
        companyContextHolderMock.when(CompanyContextHolder::getCompanyId).thenReturn(TestFixtures.COMPANY_ID);
    }

    @AfterEach
    void tearDown() {
        companyContextHolderMock.close();
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserCommand command = CreateUserCommand.builder()
                .email("new@example.com")
                .password("pass")
                .fullName("Name")
                .phone("Phone")
                .role(Role.ACCOUNTANT)
                .build();

        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            return TestDataBuilder.aUser().withId(UUID.randomUUID()).withEmail(u.getEmailValue()).build();
        });

        // When
        UserResponse response = userService.createUser(command);

        // Then
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
    }
}
