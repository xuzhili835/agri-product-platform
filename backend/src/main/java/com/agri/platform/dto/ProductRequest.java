package com.agri.platform.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    private Integer orderId;
    private String title;
    private BigDecimal price;
    private String content;
    private String picPath;
    private String type; // goods货源/demand需求
    private Integer orderStatus; // 0待交易 1交易中 2已完成
}