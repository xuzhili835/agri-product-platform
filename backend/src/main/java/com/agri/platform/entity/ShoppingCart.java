package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_shoppingcart")
public class ShoppingCart {
    @TableId(type = IdType.AUTO)
    private Integer shoppingId;
    private Integer orderId;
    private Integer count;
    private String ownName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}