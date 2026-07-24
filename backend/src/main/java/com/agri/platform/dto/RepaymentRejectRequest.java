package com.agri.platform.dto;

import lombok.Data;

/**
 * 银行驳回某期还款时的请求体：驳回原因（会记入 tb_repayment.reject_reason 并通知农户）。
 */
@Data
public class RepaymentRejectRequest {
    private String reason;
}
