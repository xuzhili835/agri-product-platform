package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_purchase")
public class Purchase {
    @TableId(type = IdType.AUTO)
    private Integer purchaseId;
    private String ownName;
    private BigDecimal totalPrice;
    private String address;
    private Integer purchaseStatus; // 1待付款 2待发货 3待收货 4已完成 5已取消
    private Integer purchaseType; // 1购物车 2直接购买
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}