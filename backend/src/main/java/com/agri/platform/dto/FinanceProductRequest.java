package com.agri.platform.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinanceProductRequest {
    private Integer productId;
    private String productName;
    private String bankName;
    private String introduce;
    private String bankPhone;
    private BigDecimal money;
    private BigDecimal rate;
    private Integer repayment;
}