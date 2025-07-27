package com.web_game.Transaction_Service.Repository;

import com.web_game.common.Entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByInventoryIdAndIsForSaleTrue(Long inventoryId);
}