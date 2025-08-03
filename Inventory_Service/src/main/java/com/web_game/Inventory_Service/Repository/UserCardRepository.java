package com.web_game.Inventory_Service.Repository;

import com.web_game.common.Entity.Inventory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCardRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByUserId(Long userId);

    Optional<Inventory> findByUserUserIdAndCardCardId(Long userId, Long cardId);

    Optional<Inventory> findByInventoryIdAndIsForSaleTrue(Long inventoryId);

    List<Inventory> findByIsForSaleTrue();

    List<Inventory> findByIsForSaleTrueAndIsOnDeckFalse();

    Optional<Inventory> findByInventoryIdAndUserId(Long inventoryId, Long userId);

    @Query("SELECT i FROM Inventory i WHERE i.inventoryId IN :inventoryIds AND i.userId = :userId")
    List<Inventory> findByInventoryIdInAndUserId(@Param("inventoryIds") List<Long> inventoryIds,
                                                 @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"card"})
    @Query("SELECT i FROM Inventory i WHERE i.userId = :userId AND i.isOnDeck = true")
    List<Inventory> findDeckCardsByUserId(@Param("userId") Long userId);

}