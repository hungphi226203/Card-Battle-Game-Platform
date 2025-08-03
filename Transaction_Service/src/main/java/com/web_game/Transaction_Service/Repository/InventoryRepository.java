package com.web_game.Transaction_Service.Repository;

import com.web_game.common.Entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByInventoryIdAndIsForSaleTrue(Long inventoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.inventoryId = :id")
    Optional<Inventory> lockByIdForUpdate(@Param("id") Long id);
}