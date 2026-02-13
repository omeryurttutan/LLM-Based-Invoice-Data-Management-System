package com.faturaocr.interfaces.rest.user;

import com.faturaocr.application.user.dto.CreateUserCommand;
import com.faturaocr.application.user.dto.UserResponse;
import com.faturaocr.application.user.service.UserApplicationService;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.interfaces.rest.common.BaseController;
import com.faturaocr.interfaces.rest.user.dto.UserRequest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for User operations.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController {

    private final UserApplicationService userService;

    public UserController(UserApplicationService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {

        CreateUserCommand command = new CreateUserCommand(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword());

        UserResponse response = userService.createUser(command);
        return created(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        UserResponse response = userService.getUser(id);
        return ok(response);
    }
}
