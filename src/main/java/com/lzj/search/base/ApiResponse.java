package com.lzj.search.base;

/**
 * @author lizijian
 */
public class ApiResponse {

    private Integer code;
    private String message;
    private Object data;
    private boolean more;

    public ApiResponse() {
    }

    public ApiResponse(Integer code, String message, Object data, boolean more) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.more = more;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isMore() {
        return more;
    }

    public void setMore(boolean more) {
        this.more = more;
    }

    public static ApiResponse ofSuccess(Object data) {
        return new ApiResponse(200, "ok", data, false);
    }

    public static ApiResponse ofMessage(int code, Object data) {
        return new ApiResponse(code, "ok", data, false);
    }
}
