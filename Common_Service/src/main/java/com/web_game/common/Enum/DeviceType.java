package com.web_game.common.Enum;

public enum DeviceType {
    WEB("web"),
    UNITY("unity"),
    MOBILE("mobile");

    private final String value;

    DeviceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DeviceType fromString(String value) {
        if (value == null) return WEB;

        for (DeviceType type : DeviceType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return WEB;
    }
}