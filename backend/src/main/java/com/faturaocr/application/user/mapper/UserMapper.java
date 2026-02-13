package com.faturaocr.application.user.mapper;

import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.domain.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User domain entity and Application DTOs.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail().getValue(),
                user.getRole().name());
    }
}
