package com.faturaocr.application.user.dto;

import com.faturaocr.application.common.usecase.Command;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Command DTO for creating a new user.
 */
@Getter
@AllArgsConstructor
public class CreateUserCommand implements Command {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String password;
}
