package com.faturaocr.interfaces.rest.user.mapper;

import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.interfaces.rest.user.dto.UserApiResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Application DTOs and API DTOs.
 */
@Component
public class UserApiMapper {

    public UserApiResponse toApiResponse(UserResponse userResponse) {
        if (userResponse == null) {
            return null;
        }

        return new UserApiResponse(
                userResponse.getId(),
                userResponse.getFirstName(),
                userResponse.getLastName(),
                userResponse.getEmail(),
                userResponse.getRole());
    }
}
