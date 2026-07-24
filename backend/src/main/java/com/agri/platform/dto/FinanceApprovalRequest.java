package com.agri.platform.dto;

import lombok.Data;

@Data
public class FinanceApprovalRequest {
    private Integer status; // 1已通过 2已驳回
    private String remark; // 审批备注
}