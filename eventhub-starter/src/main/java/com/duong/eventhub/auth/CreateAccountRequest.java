package com.duong.eventhub.auth;

import com.duong.eventhub.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @Email
        @NotBlank
        String email,
        @Min(8)
        String password,
        @NotNull
        boolean enabled,
        @NotBlank
        Role role
) {
}
