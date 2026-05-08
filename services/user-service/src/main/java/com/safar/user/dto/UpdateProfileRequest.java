package com.safar.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100) String name,
        @Size(max = 100) String displayName,
        @Email String email,
        String avatarUrl,
        String bio,
        @Size(max = 200) String languages,
        @Pattern(regexp = "^(en|hi|ta|te|kn|mr|bn|gu)$", message = "Unsupported language code") String language,
        @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Phone must be a 10-digit Indian mobile number")
        String phone,
        LocalDate dateOfBirth,
        @Size(max = 30) String gender,
        @Size(max = 60) String nationality,
        String address,
        @Size(max = 200) String passportName,
        @Size(max = 30) String passportNumber,
        LocalDate passportExpiry
) {}
