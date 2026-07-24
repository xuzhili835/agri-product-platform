package com.agri.platform.dto;

import lombok.Data;

@Data
public class FinancingIntentionRequest {
    private String realName;
    private String phone;
    private String address;
    private Integer amount; // 期望金额(万)
    private String application; // 融资用途
    private String item; // 农作物
    private Integer area; // 种植面积(亩)
    private Integer repaymentPeriod; // 还款期限(月)
}