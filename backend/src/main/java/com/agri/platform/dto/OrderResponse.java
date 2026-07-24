package com.agri.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应 DTO
 */
@Data
public class OrderResponse {
    private Integer purchaseId;

    /** 兼容旧前端：同时以 id 暴露订单ID（如 farmer/Orders 用 order.orderId||order.id） */
    @JsonProperty("id")
    public Integer getId() {
        return purchaseId;
    }

    private String ownName;
    /** 买家真实姓名（仅卖家"收到的订单"列表回填，便于农户看到买家真名而非登录账号） */
    private String buyerRealName;
    /** 发货人/卖家真实姓名（买家订单列表回填；一单多卖家时以「、」拼接） */
    private String sellerName;
    private BigDecimal totalPrice;
    private String address;
    private Integer purchaseStatus;
    private String statusText;
    private Integer purchaseType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private List<OrderItemResponse> items;

    @Data
    public static class OrderItemResponse {
        private Integer productId;
        private String productName;
        private String productPic;
        private BigDecimal price;
        private Integer count;
        private BigDecimal totalPrice;
    }
}
