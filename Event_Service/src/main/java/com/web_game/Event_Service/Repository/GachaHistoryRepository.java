package com.web_game.Event_Service.Repository;

import com.web_game.common.Entity.GachaHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GachaHistoryRepository extends JpaRepository<GachaHistory, Long> {
}
