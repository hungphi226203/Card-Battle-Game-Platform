package com.web_game.common.DTO.shared;

import com.web_game.common.Enum.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserDTO {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Gender gender;
    private Float balance;
    private String image;
    private LocalDate dob;
    private Integer stage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean locked;
    private List<String> roles;
}