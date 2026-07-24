package com.agri.platform.dto;

import com.agri.platform.entity.Finance;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 银行工作台「数据概览」聚合统计
 */
@Data
public class BankFinanceStats {
    /** 待审批数量 */
    private Integer pendingCount;
    /** 已通过数量 */
    private Integer approvedCount;
    /** 已拒绝数量 */
    private Integer rejectedCount;
    /** 申请总金额（元） */
    private BigDecimal totalAmount;
    /** 本月已通过放款金额（元） */
    private BigDecimal monthlyAmount;
    /** 审批通过率（百分比，0-100） */
    private Integer approvalRate;
    /** 待审批列表（最多 5 条，最早的优先） */
    private List<Finance> pendingList;
    /** 最近已处理记录（最多 5 条，最新的优先） */
    private List<Finance> recentList;
}
