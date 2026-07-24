package com.agri.platform.dto;

import lombok.Data;

/**
 * 农户提交还款时的请求体：流水号/备注 + 凭证图片URL（凭证经 /upload 上传后回传 url）。
 */
@Data
public class RepaymentSubmitRequest {
    /** 还款流水号或备注，如银行转账流水、微信/支付宝转账单号 */
    private String transactionNo;
    /** 还款凭证图片URL（/upload 返回的 url），可选 */
    private String payProof;
}
