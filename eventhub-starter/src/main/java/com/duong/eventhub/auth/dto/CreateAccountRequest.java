package com.duong.eventhub.auth.dto;

import com.duong.eventhub.user.Role;
import jakarta.validation.constraints.*;

public record CreateAccountRequest(
        @Email
        @NotBlank
        String email,
        @Min(8)
        String password,
        @NotBlank
        @Size(min = 6,max = 20)
        String displayName,
        @NotNull
        boolean enabled,
        @NotBlank
        Role role
) {
}
