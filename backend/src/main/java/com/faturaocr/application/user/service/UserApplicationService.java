package com.faturaocr.application.user.service;

import com.faturaocr.application.common.service.ApplicationService;
import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.application.user.usecase.CreateUserUseCase;
import com.faturaocr.application.user.usecase.GetUserUseCase;

import java.util.UUID;

/**
 * Application service that orchestrates user-related use cases.
 */
@ApplicationService
public class UserApplicationService {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;

    public UserApplicationService(CreateUserUseCase createUserUseCase,
            GetUserUseCase getUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
    }

    public UserResponse createUser(CreateUserCommand command) {
        return createUserUseCase.execute(command);
    }

    public UserResponse getUser(UUID userId) {
        return getUserUseCase.execute(userId);
    }
}
