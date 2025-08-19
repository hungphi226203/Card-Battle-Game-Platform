package com.web_game.Notification_Service.Repository;

import com.web_game.common.Entity.Notification;
import com.web_game.common.Enum.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy thông báo của user theo thời gian
    Page<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Lấy thông báo chưa đọc
    List<Notification> findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // Đếm thông báo chưa đọc
    long countByUserUserIdAndIsReadFalse(Long userId);

    // Lấy thông báo theo loại
    List<Notification> findByUserUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);

    // Đánh dấu tất cả thông báo của user là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    // Đánh dấu thông báo cụ thể là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId AND n.user.userId = :userId")
    void markAsReadByIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    // Xóa thông báo cũ (cleanup job)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    //=======ADMIN===========
    @Query("SELECT n FROM Notification n WHERE n.groupId IS NOT NULL AND n.id = (SELECT MAX(n2.id) FROM Notification n2 WHERE n2.groupId = n.groupId) ORDER BY n.createdAt DESC")
    List<Notification> findGlobalNotifications();

    @Modifying
    @Query(value = "UPDATE notifications SET " +
            "message = COALESCE(:message, message), " +
            "type = COALESCE(:type, type) " +
            "WHERE group_id = :groupId",
            nativeQuery = true)
    void updateByGroupId(@Param("groupId") String groupId,
                         @Param("message") String message,
                         @Param("type") String type);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.groupId = :groupId")
    int deleteByGroupId(@Param("groupId") String groupId);
}