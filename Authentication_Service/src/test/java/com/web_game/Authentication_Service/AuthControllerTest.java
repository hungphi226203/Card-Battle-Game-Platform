package com.web_game.Authentication_Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web_game.common.DTO.Request.Auth.AuthRequest;
import com.web_game.common.DTO.Request.Auth.RegisterRequest;
import com.web_game.common.DTO.Request.Auth.VerifyRequest;
import com.web_game.common.Enum.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegister() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser3");
        request.setEmail("testuse3r@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setPhone("0123456789");
        request.setGender(Gender.MALE);
        request.setDob(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đăng ký thành công"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void testLogin() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("huy3");
        request.setPassword("123456");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data").isNotEmpty()); // token
    }

    @Test
    void testVerifyToken() throws Exception {
        // Lấy token trước (giả định đã có user testuser đăng ký/login ở trên)
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String token = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token từ JSON (dùng ObjectMapper để parse cho chuẩn)
        String jwt = objectMapper.readTree(token).get("data").asText();

        VerifyRequest verifyRequest = new VerifyRequest();
        verifyRequest.setToken(jwt);

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token hợp lệ"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }
}
