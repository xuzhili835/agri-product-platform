package com.agri.platform.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车响应 DTO
 */
@Data
public class CartResponse {
    // 购物车项ID
    private Integer cartId;

    // 商品ID
    private Integer productId;

    // 数量
    private Integer count;

    private String ownName;

    // 商品信息
    private String title;
    private BigDecimal price;
    private String picPath;
    private String content;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
