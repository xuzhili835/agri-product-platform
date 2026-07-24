package com.agri.platform.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private Integer purchaseType; // 1购物车 2直接购买
    private String address; // 收货地址
    private List<OrderItem> items; // 商品列表（直接购买时使用）

    @Data
    public static class OrderItem {
        private Integer orderId;
        private Integer count;
    }
}