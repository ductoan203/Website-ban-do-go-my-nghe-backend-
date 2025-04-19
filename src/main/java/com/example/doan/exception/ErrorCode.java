    package com.example.doan.exception;

    import lombok.Getter;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.HttpStatusCode;

    @Getter
    public enum ErrorCode {
        UNCATEGORIZED_EXCEPTION("Uncategorized exception", 9999, HttpStatus.INTERNAL_SERVER_ERROR),
        INVALID_KEY("Invalid input", 1000, HttpStatus.BAD_REQUEST),
        USER_NOT_FOUND("User not found", 1001, HttpStatus.NOT_FOUND),
        USERNAME_EXISTS("User already exists", 1002, HttpStatus.BAD_REQUEST),
        USERNAME_INVALID("Username must be at least 5 characters", 1003, HttpStatus.BAD_REQUEST),
        INVALID_PASSWORD("Password must be at least 8 characters", 1004, HttpStatus.BAD_REQUEST),
        EMAIL_EXISTS("Email already exists", 1005, HttpStatus.BAD_REQUEST),
        UNAUTHENTICATED("Unauthenticated", 1006, HttpStatus.UNAUTHORIZED),
        UNAUTHORIZED("You do not have permission", 1007, HttpStatus.FORBIDDEN),
        INVALID_OTP("Invalid OTP code", 1008, HttpStatus.BAD_REQUEST),
        OTP_EXPIRED("OTP code has expired", 1009, HttpStatus.BAD_REQUEST),
        USER_NOT_VERIFIED("User has not been verified", 1010, HttpStatus.UNAUTHORIZED),
        USER_ALREADY_VERIFIED("User has already been verified", 1011, HttpStatus.BAD_REQUEST),

        INVALID_CATEGORY_NAME("Category name already exists", 1012, HttpStatus.BAD_REQUEST),
        CATEGOTY_NOT_FOUND("Category not found", 1013, HttpStatus.NOT_FOUND),
        CATEGORY_DELETE_FAILED("Không thể xoá danh mục vì đang chứa sản phẩm", 1014, HttpStatus.BAD_REQUEST),
        PRODUCT_NOT_FOUND("Product not found", 1015, HttpStatus.NOT_FOUND),
        CART_ITEM_NOT_FOUND("Cart item not found", 1016, HttpStatus.NOT_FOUND),
        PRODUCT_LARGE_THAN_0("Số lượng sản phẩm phải lớn hơn 0", 1017, HttpStatus.BAD_REQUEST),

        CART_EMPTY("Giỏ hàng trống", 1018, HttpStatus.BAD_REQUEST),
        ORDER_NOT_FOUND("Order not found", 1019, HttpStatus.NOT_FOUND),
        ORDER_CANCEL_FAILED("Chỉ có thể huỷ đơn đang chờ xử lý", 1020, HttpStatus.BAD_REQUEST),
        ORDER_STATUS_CHANGE_FAILED("Không thể thay đổi trạng thái đơn hàng", 1021, HttpStatus.BAD_REQUEST),

        ORDER_CANCELED_BY_USER("Đơn hàng đã bị huỷ bởi người dùng và không thể chỉnh sửa", 1022, HttpStatus.BAD_REQUEST),


        ;

        ErrorCode(String message, int code, HttpStatusCode statusCode) {
            this.message = message;
            this.code = code;
            this.statusCode = statusCode;
        }

        private String message;
        private int code;
        private HttpStatusCode statusCode;


    }
