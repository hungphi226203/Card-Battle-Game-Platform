package com.web_game.common.Enum;

public enum NotificationType {
    TRANSACTION_SUCCESS("Giao dịch thành công"),
    TRANSACTION_FAILED("Giao dịch thất bại"),
    INVENTORY_UPDATE("Cập nhật kho đồ"),
    GACHA_REWARD("Thưởng gacha"),
    MARKET_ACTIVITY("Hoạt động chợ"),
    SYSTEM_NOTIFICATION("Thông báo hệ thống"),
    WELCOME("Chào mừng"),
    BALANCE_UPDATE("Cập nhật số dư");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}