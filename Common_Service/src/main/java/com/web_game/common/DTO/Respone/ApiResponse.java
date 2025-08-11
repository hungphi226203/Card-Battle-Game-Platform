package com.web_game.common.DTO.Respone;

import lombok.*;

//@Data
//@Builder
//public class ApiResponse {
//    private int code;
//    private String message;
//    private Object data;
//
//    public ApiResponse() {
//    }
//
//    public ApiResponse(int code, String message) {
//        this.code = code;
//        this.message = message;
//    }
//
//    public ApiResponse(int code, String message, Object data) {
//        this.code = code;
//        this.message = message;
//        this.data = data;
//    }
//}
@Data
@Builder
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}