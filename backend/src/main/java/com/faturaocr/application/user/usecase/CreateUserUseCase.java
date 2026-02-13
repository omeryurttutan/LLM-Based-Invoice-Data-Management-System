package com.faturaocr.application.user.usecase;

import com.faturaocr.application.common.usecase.UseCase;
import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.UserRole;
import com.faturaocr.domain.common.exception.DomainException;

/**
 * Use case for creating a new user.
 */
public class CreateUserUseCase implements UseCase<CreateUserCommand, UserResponse> {

    private final UserRepository userRepository;

    public CreateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse execute(CreateUserCommand command) {
        Email email = Email.of(command.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DomainException("USER_ALREADY_EXISTS",
                    "User with email " + email + " already exists");
        }

        User user = User.create(
                command.getFirstName(),
                command.getLastName(),
                email,
                command.getPassword(), // In real impl: hash the password
                UserRole.USER);

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail().getValue(),
                savedUser.getRole().name());
    }
}
