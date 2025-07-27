package com.web_game.common.DTO.Request.Auth;

import com.web_game.common.Enum.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    @NotBlank(message = "USERNAME_INVALID")
    @Size(min = 3, max = 50, message = "USERNAME_INVALID")
    private String username;

    @NotBlank(message = "INVALID_KEY")
    @Email(message = "INVALID_KEY")
    private String email;

    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 6, message = "INVALID_PASSWORD")
    private String password;

    @NotBlank(message = "INVALID_KEY")
    private String fullName;

    @NotBlank(message = "INVALID_PHONE")
    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "INVALID_PHONE"
    )
    private String phone;

    @NotNull(message = "INVALID_GENDER")
    private Gender gender;

    @Past(message = "INVALID_DOB")
    private LocalDate dob;
}