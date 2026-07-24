package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("tb_purchase_detail")
public class PurchaseDetail {
    @TableId(type = IdType.AUTO)
    private Integer detailId;
    private Integer purchaseId;
    private Integer orderId;
    private BigDecimal uninPrice;
    private Integer count;
    private BigDecimal sumPrice;
}