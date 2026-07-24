package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_finance_product")
public class FinanceProduct {
    @TableId(type = IdType.AUTO)
    private Integer productId;
    private String productName;
    private String bankName;
    /** 发布该产品的银行账号(userName)，用于银行间数据隔离：银行仅能看到/管理本行产品 */
    private String bankUserName;
    private String introduce;
    private String bankPhone;
    private BigDecimal money;
    private BigDecimal rate;
    private Integer repayment;
    private Integer status; // 0在售/可申请 1暂停供应（不可删除，只能暂停）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}