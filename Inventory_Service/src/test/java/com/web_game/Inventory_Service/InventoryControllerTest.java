package com.web_game.Inventory_Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web_game.Inventory_Service.Service.InventoryService;
import com.web_game.common.DTO.Request.UserCard.SellCardRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("USER rao bán thẻ thành công")
    void testListCardForSaleSuccess() throws Exception {
        SellCardRequest request = new SellCardRequest();
        request.setSalePrice(1000f);

        Mockito.doNothing().when(inventoryService).listCardForSale(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());

        mockMvc.perform(put("/inventory/1434/sell")
                        .header("X-Roles", "USER")
                        .header("X-UserId", "15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rao bán thẻ thành công"));
    }

    @Test
    @DisplayName("❌ Không có quyền USER thì bị 403 Forbidden")
    void testListCardForSaleForbidden() throws Exception {
        SellCardRequest request = new SellCardRequest();
        request.setSalePrice(1000f);

        // Không cần mock service throw exception vì controller sẽ tự check role
        // Controller đang trả về "Forbidden: USER role required" based on header X-Roles

        mockMvc.perform(put("/inventory/1/sell")
                        .header("X-Roles", "MANAGER") // Role sai
                        .header("X-UserId", "15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: USER role required"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("❌ Thiếu header X-Roles")
    void testListCardForSaleMissingRole() throws Exception {
        SellCardRequest request = new SellCardRequest();
        request.setSalePrice(1000f);

        mockMvc.perform(put("/inventory/1/sell")
                        // Không có header X-Roles
                        .header("X-UserId", "15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("❌ Thiếu header X-UserId")
    void testListCardForSaleMissingUserId() throws Exception {
        SellCardRequest request = new SellCardRequest();
        request.setSalePrice(1000f);

        mockMvc.perform(put("/inventory/1/sell")
                        .header("X-Roles", "USER")
                        // Không có header X-UserId
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(9999));
    }

    @Test
    @DisplayName("❌ Request body không hợp lệ")
    void testListCardForSaleInvalidRequest() throws Exception {
        mockMvc.perform(put("/inventory/1/sell")
                        .header("X-Roles", "USER")
                        .header("X-UserId", "15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty request body
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Service layer test - Mock exception")
    void testListCardForSaleServiceException() throws Exception {
        SellCardRequest request = new SellCardRequest();
        request.setSalePrice(1000f);

        // Mock service để throw exception khác
        Mockito.doThrow(new RuntimeException("Database error"))
                .when(inventoryService).listCardForSale(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());

        mockMvc.perform(put("/inventory/1434/sell")
                        .header("X-Roles", "USER")
                        .header("X-UserId", "15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // Tùy vào cách handle exception
    }
}