package com.web_game.common.DTO.Request.User;

import com.web_game.common.Enum.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    @Size(max = 100, message = "INVALID_FULL_NAME")
    private String fullName;

    @NotBlank(message = "INVALID_KEY")
    @Email(message = "INVALID_KEY")
    private String email;


    @Pattern(
            regexp = "^(0|\\+?84)[0-9]{9}$",
            message = "INVALID_PHONE"
    )
    private String phone;

    private Gender gender;

    private String image;

    private LocalDate dob;
}