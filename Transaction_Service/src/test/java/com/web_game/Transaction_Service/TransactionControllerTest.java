package com.web_game.Transaction_Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web_game.Transaction_Service.Repository.InventoryRepository;
import com.web_game.Transaction_Service.Repository.TransactionRepository;
import com.web_game.Transaction_Service.Repository.UserRepository;
import com.web_game.common.DTO.Request.Transaction.TransactionRequest;
import com.web_game.common.Entity.Inventory;
import com.web_game.common.Entity.User;
import com.web_game.common.Entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User buyer;
    private User seller;

    @BeforeEach
    void setup() {
        // Tạo seller (người bán thẻ)
        seller = new User();
        seller.setUsername("seller_user");
        seller.setEmail("seller@example.com");
        seller.setPasswordHash(passwordEncoder.encode("password123"));
        seller.setBalance(0.0f); // Seller không cần tiền
        seller = userRepository.save(seller);

        // Tạo buyer (người mua thẻ) với đủ tiền
        buyer = new User();
        buyer.setUsername("buyer_user");
        buyer.setEmail("buyer@example.com");
        buyer.setPasswordHash(passwordEncoder.encode("password123"));
        buyer.setBalance(1000.0f); // Đặt số dư đủ để mua thẻ
        buyer = userRepository.save(buyer);
    }

    @Test
    @Rollback
    void testCreateAndCompleteTransaction_Success() throws Exception {
        // Tạo một inventory item khả dụng cho test
        Inventory inventory = new Inventory();
        inventory.setCardId(1L);
        inventory.setUserId(seller.getUserId()); // Sử dụng seller thật
        inventory.setIsForSale(true); // Đảm bảo thẻ đang được bán
        inventory.setSalePrice(100.0f);
        inventory = inventoryRepository.save(inventory);

        TransactionRequest request = new TransactionRequest();
        request.setInventoryId(inventory.getInventoryId()); // Sử dụng ID thực tế

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-UserId", buyer.getUserId().toString()) // Sử dụng buyer thật
                        .header("X-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(201)))
                .andExpect(jsonPath("$.message", containsString("Mua thẻ thành công")))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @Rollback
    void testCreateAndCompleteTransaction_InsufficientBalance() throws Exception {
        // Tạo buyer với số dư không đủ
        User poorBuyer = new User();
        poorBuyer.setUsername("poor_buyer");
        poorBuyer.setEmail("poor@example.com");
        poorBuyer.setPasswordHash(passwordEncoder.encode("password123"));
        poorBuyer.setBalance(50.0f); // Không đủ tiền mua thẻ 100
        poorBuyer = userRepository.save(poorBuyer);

        // Tạo inventory item
        Inventory inventory = new Inventory();
        inventory.setCardId(1L);
        inventory.setUserId(seller.getUserId());
        inventory.setIsForSale(true);
        inventory.setSalePrice(100.0f);
        inventory = inventoryRepository.save(inventory);

        TransactionRequest request = new TransactionRequest();
        request.setInventoryId(inventory.getInventoryId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-UserId", poorBuyer.getUserId().toString())
                        .header("X-Roles", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(1035)))
                .andExpect(jsonPath("$.message", containsString("Số dư không đủ")));
    }

    @Test
    @Rollback
    void testCreateAndCompleteTransaction_MissingUserRole() throws Exception {
        // Tạo inventory item cho test này
        Inventory inventory = new Inventory();
        inventory.setCardId(1L);
        inventory.setUserId(seller.getUserId());
        inventory.setIsForSale(true);
        inventory.setSalePrice(100.0f);
        inventory = inventoryRepository.save(inventory);

        TransactionRequest request = new TransactionRequest();
        request.setInventoryId(inventory.getInventoryId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-UserId", buyer.getUserId().toString()))
                // Không có header X-Roles
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)))
                .andExpect(jsonPath("$.message", containsString("Forbidden")));
    }

    @Test
    @Rollback
    void testCreateAndCompleteTransaction_MissingUserId() throws Exception {
        // Tạo inventory item cho test này
        Inventory inventory = new Inventory();
        inventory.setCardId(1L);
        inventory.setUserId(seller.getUserId());
        inventory.setIsForSale(true);
        inventory.setSalePrice(100.0f);
        inventory = inventoryRepository.save(inventory);

        TransactionRequest request = new TransactionRequest();
        request.setInventoryId(inventory.getInventoryId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Roles", "USER"))
                // Không có header X-UserId
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("Missing or invalid X-UserId")));
    }

    @Test
    @Rollback
    void testGetUserTransactions_Success() throws Exception {
        mockMvc.perform(get("/transactions")
                        .header("X-UserId", buyer.getUserId().toString())
                        .header("X-Roles", "USER")
                        .param("role", "buyer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", containsString("Lấy danh sách giao dịch thành công")))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Rollback
    void testGetTransactionById_Success() throws Exception {
        // Tạo một transaction trước
        Transaction transaction = new Transaction();
        transaction.setSellerId(seller.getUserId());
        transaction.setBuyerId(buyer.getUserId());
        transaction.setInventoryId(1L);
        transaction.setPrice(100.0f);
        transaction = transactionRepository.save(transaction);

        mockMvc.perform(get("/transactions/" + transaction.getTransactionId())
                        .header("X-UserId", buyer.getUserId().toString())
                        .header("X-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", containsString("Lấy thông tin giao dịch thành công")))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @Rollback
    void testCancelTransaction_Success() throws Exception {
        // Tạo một transaction trước
        Transaction transaction = new Transaction();
        transaction.setSellerId(seller.getUserId());
        transaction.setBuyerId(buyer.getUserId());
        transaction.setInventoryId(1L);
        transaction.setPrice(100.0f);
        transaction = transactionRepository.save(transaction);

        mockMvc.perform(put("/transactions/" + transaction.getTransactionId() + "/cancel")
                        .header("X-UserId", buyer.getUserId().toString())
                        .header("X-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", containsString("Hủy giao dịch thành công")))
                .andExpect(jsonPath("$.data").exists());
    }
}