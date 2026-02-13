package com.faturaocr.application.user.usecase;

import com.faturaocr.application.common.usecase.UseCase;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.domain.common.exception.EntityNotFoundException;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;

import java.util.UUID;

/**
 * Use case for retrieving a user by ID.
 */
public class GetUserUseCase implements UseCase<UUID, UserResponse> {

    private final UserRepository userRepository;

    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail().getValue(),
                user.getRole().name());
    }
}
