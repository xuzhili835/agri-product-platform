package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Integer paymentId;
    private Integer purchaseId;
    private String outTradeNo;       // 商户订单号（= purchase_id）
    private String alipayTradeNo;    // 支付宝交易号
    private BigDecimal totalAmount;  // 支付金额
    private String tradeStatus;      // 交易状态（TRADE_SUCCESS 等）
    private LocalDateTime payTime;   // 支付完成时间
    private LocalDateTime createTime;
}
