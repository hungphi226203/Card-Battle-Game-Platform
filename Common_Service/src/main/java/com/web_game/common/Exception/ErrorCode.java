package com.web_game.common.Exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(1000, "Khóa không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(1002, "Tên người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Tên đăng nhập phải có ít nhất 4 ký tự", HttpStatus.BAD_REQUEST),

    INVALID_FULL_NAME(1022, "Tên không hợp lệ", HttpStatus.BAD_REQUEST),

    INVALID_PASSWORD(1004, "Mật khẩu không đúng", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_LENGTH(1005, "Mật khẩu phải từ 6 ký tự trở lên", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_MUST_BE_DIFFERENT(1006, "Mật khẩu mới không được trùng mật khẩu cũ", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1007, "Email đã tồn tại", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_FOUND(1008, "Email không tồn tại", HttpStatus.NOT_FOUND),

    USER_NOT_FOUND(1009, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1010, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1011, "Bạn không có quyền truy cập", HttpStatus.FORBIDDEN),

    INVALID_DOB(1012, "Tuổi phải ít nhất 13 tuổi", HttpStatus.BAD_REQUEST),
    PHONE_NOT_BLANK(1013, "Số điện thoại không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(1014, "Số điện thoại không hợp lệ", HttpStatus.BAD_REQUEST),

    INVALID_GENDER(1015, "Giới tính phải được chọn", HttpStatus.BAD_REQUEST),
    INVALID_REFRESH_TOKEN(1016, "Refresh token không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_RESET_TOKEN(1017, "Token reset không hợp lệ", HttpStatus.BAD_REQUEST),

    INVALID_ROLE(1018, "Vai trò không hợp lệ", HttpStatus.BAD_REQUEST),
    SINGLE_ADMIN_VIOLATION(1019, "Chỉ được phép có một admin", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_SELF(1020, "Không thể sửa đổi tài khoản của chính bạn", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_MANAGER(1, "Không thể sửa đổi vai trò MANAGER của chính bạn", HttpStatus.BAD_REQUEST),

    CARD_CODE_ALREADY_EXISTS(1021, "Mã thẻ bài đã tồn tại", HttpStatus.BAD_REQUEST),
    CARD_CODE_NOT_BLANK(1022, "Mã thẻ bài không được để trống", HttpStatus.BAD_REQUEST),
    CARD_NAME_NOT_BLANK(1023, "Tên thẻ bài không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_RARITY(1024, "Độ hiếm của thẻ bài không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_CARD_VALUE(1025, "Giá trị thẻ bài phải lớn hơn hoặc bằng 0", HttpStatus.BAD_REQUEST),

    INVALID_EFFECT_VALUE(1006, "Effect không tồn tại", HttpStatus.BAD_REQUEST),
    EFFECT_IN_USE(1007, "Hiệu ứng đang được sử dụng bởi thẻ bài, không thể xóa", HttpStatus.BAD_REQUEST),

    USER_CARD_NOT_FOUND(1030, "Thẻ không có trong kho", HttpStatus.NOT_FOUND),
    USER_CARD_ALREADY_EXISTS(1031, "Thẻ đã có trong kho", HttpStatus.BAD_REQUEST),

    TRANSACTION_AMOUNT_INVALID(1026, "Số tiền giao dịch phải lớn hơn hoặc bằng 0", HttpStatus.BAD_REQUEST),
    TRANSACTION_TYPE_INVALID(1027, "Loại giao dịch không hợp lệ", HttpStatus.BAD_REQUEST),

    INVENTORY_QUANTITY_INVALID(1028, "Số lượng thẻ trong kho phải lớn hơn hoặc bằng 0", HttpStatus.BAD_REQUEST),
    INVENTORY_NOT_FOUND(1029, "Kho thẻ bài không tồn tại", HttpStatus.NOT_FOUND),

    TRANSACTION_NOT_FOUND(1033, "Giao dịch không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_TRANSACTION_STATUS(1034, "Trạng thái giao dịch không hợp lệ", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(1035, "Số dư không đủ", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION(1036, "Giao dịch không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_CARD_NOT_FOR_SALE(1037, "Thẻ không được rao bán", HttpStatus.BAD_REQUEST),
    CARD_ALREADY_SOLD(1037, "Thẻ vừa bán", HttpStatus.BAD_REQUEST),

    CARD_ALREADY_FOR_SALE(1038, "Thẻ đã được rao bán", HttpStatus.BAD_REQUEST),
    CARD_NOT_FOR_SALE(1039, "Thẻ chưa được rao bán", HttpStatus.BAD_REQUEST),

    INVALID_DECK_SIZE(3001, "Deck phải có đúng 30 thẻ", HttpStatus.BAD_REQUEST),
    INVALID_CARD_OWNERSHIP(3002, "Một hoặc nhiều thẻ không thuộc về bạn", HttpStatus.BAD_REQUEST),
    CARD_IN_SALE_CANNOT_BE_IN_DECK(3003, "Thẻ đang được rao bán không thể thêm vào deck", HttpStatus.BAD_REQUEST),
    DECK_FULL(3004, "Deck đã đầy (30 thẻ)", HttpStatus.BAD_REQUEST),
    CARD_ALREADY_IN_DECK(3005, "Thẻ đã có trong deck", HttpStatus.BAD_REQUEST),
    CARD_NOT_IN_DECK(3006, "Thẻ không có trong deck", HttpStatus.BAD_REQUEST),
    CARD_IN_DECK_CANNOT_BE_SOLD(3007, "Thẻ đang trong deck không thể được rao bán", HttpStatus.BAD_REQUEST),

    OKEN_EXPIRED(1010, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1011, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(1013, "Phiên đăng nhập đã hết hạn", HttpStatus.UNAUTHORIZED),
    DEVICE_NOT_SUPPORTED(1014, "Loại thiết bị không được hỗ trợ", HttpStatus.BAD_REQUEST),

    NOTIFICATION_MESSAGE_NOT_BLANK(1030, "Nội dung thông báo không được để trống", HttpStatus.BAD_REQUEST),
    NOTIFICATION_TYPE_INVALID(1031, "Loại thông báo không hợp lệ", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}