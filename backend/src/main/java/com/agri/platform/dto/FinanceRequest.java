package com.agri.platform.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinanceRequest {
    private Integer financeId;
    private Integer productId;
    private String realName;
    private String phone;
    private String idNum;
    private BigDecimal money;
    private Integer repayment;
    private String combinationName1;
    private String combinationPhone1;
    private String combinationIdnum1;
    private String combinationName2;
    private String combinationPhone2;
    private String combinationIdnum2;
    /** 联合贷款人1的 userName（前端从联系人列表选择，后端据此创建邀请，对方同意后回填 combination 字段） */
    private String jointUserName1;
    /** 联合贷款人2的 userName */
    private String jointUserName2;
    private String fileInfo;
    private String purpose;          // 申请原因/融资用途
    private String repaymentSource;  // 还款来源
}