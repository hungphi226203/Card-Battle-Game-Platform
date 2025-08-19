package com.web_game.Gateway_Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @LocalServerPort
    int gatewayPort;

    @Autowired
    WebTestClient webTestClient; // đã auto bind vào cổng RANDOM_PORT của gateway

    @Autowired
    ObjectMapper objectMapper;

    private WebTestClient gatewayClient;

    @BeforeEach
    void setUp() {
        this.gatewayClient = webTestClient.mutate()
                .baseUrl("http://localhost:" + gatewayPort)
                .build();
    }

    // Helper: login qua GATEWAY để lấy token thật từ Auth Service
    private String loginAndGetToken(String username, String password) throws Exception {
        String body = gatewayClient.post()
                .uri("/auth/login") // đi qua gateway -> route tới auth_service (8086)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", username,
                        "password", password
                ))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode root = objectMapper.readTree(body);
        // AuthController trả ApiResponse { code, message, data }, token nằm ở "data"
        assertThat(root.has("data")).as("Response phải có field 'data' chứa token").isTrue();
        String token = root.get("data").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    @Test
    @DisplayName("Thiếu token → Gateway trả 401 cho route bảo vệ")
    void whenMissingToken_thenUnauthorized() {
        gatewayClient.get()
                .uri("/users/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").value(msg -> assertThat(msg.toString()).contains("Unauthorized"));
    }

    @Test
    @DisplayName("Token sai định dạng/invalid → Gateway trả 401")
    void whenInvalidToken_thenUnauthorized() {
        gatewayClient.get()
                .uri("/users/me")
                .header("Authorization", "Bearer abc.def.ghi")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").value(msg -> assertThat(msg.toString()).contains("Unauthorized"));
    }

    @Test
    @DisplayName("Login qua Gateway → lấy token thật → gọi /users/me qua Gateway")
    void loginThenAccessProtectedThroughGateway() throws Exception {
        String username = "huy3";
        String password = "123456";

        // B1: login để lấy token thật từ AuthService qua Gateway
        String token = loginAndGetToken(username, password);

        // B2: Gọi endpoint bảo vệ /users/me qua Gateway
        var entityExchangeResult = gatewayClient.get()
                .uri("/users/me") // đây là endpoint có thật trong UserService
                .header("Authorization", "Bearer " + token)
                .exchange()
                .returnResult(String.class);

        int status = entityExchangeResult.getStatus().value();
        System.out.println("Status khi gọi /users/me qua Gateway = " + status);

        // B3: Chỉ cần chắc chắn KHÔNG phải 401 (Gateway xác thực token thành công)
        assertThat(status)
                .as("Gateway đã xác thực token, không nên trả 401")
                .isNotEqualTo(401);
    }

    @Test
    @DisplayName("Login qua Gateway → token hợp lệ → gọi một route khác (ví dụ /users/**)")
    void loginThenAccessAnotherProtectedRoute() throws Exception {
        String token = loginAndGetToken("huy3", "123456");

        var res = gatewayClient.get()
                .uri("/users/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .returnResult(String.class);

        int status = res.getStatus().value();
        assertThat(status).isNotEqualTo(401);
    }
}
