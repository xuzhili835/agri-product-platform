package com.agri.platform.dto;

import lombok.Data;

@Data
public class CartRequest {
    private Integer orderId;
    private Integer count;
}