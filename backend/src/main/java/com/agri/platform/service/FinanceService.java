package com.agri.platform.service;

import com.agri.platform.dto.BankFinanceStats;
import com.agri.platform.dto.FinanceApprovalRequest;
import com.agri.platform.dto.FinanceProductRequest;
import com.agri.platform.dto.FinanceRequest;
import com.agri.platform.entity.Finance;
import com.agri.platform.entity.FinanceProduct;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;

public interface FinanceService {
    void publishProduct(String userName, FinanceProductRequest request);
    Page<FinanceProduct> getProductList(int page, int pageSize);
    FinanceProduct getProductById(Integer productId);
    void applyFinance(String userName, FinanceRequest request);
    Page<Finance> getApplyList(String userName, int page, int pageSize);
    Page<Finance> getBankApprovalList(int page, int pageSize);
    /** 银行审批融资申请；bankUserName 用于校验该申请确属本行产品，禁止跨行审批 */
    void approveFinance(Integer financeId, String bankUserName, FinanceApprovalRequest request);
    void updateFinance(Integer financeId, String userName, FinanceRequest request);
    void deleteFinance(Integer financeId, String userName);

    /** 银行工作台：本行产品的全状态融资申请列表（支持状态/关键词/产品/时间区间过滤） */
    Page<Finance> getBankApplications(String bankUserName, Integer status, String keyword, Integer productId, String startDate, String endDate, int page, int pageSize);

    /**
     * 银行智能匹配：在本行产品对应的「申请中」融资申请里，按 信用/联合贷款人信用/交易活跃度/借贷负担
     * 等多重因素自动计算综合匹配度并降序排列；金额/期限目标为可选的细化指标。
     * @param bankUserName 当前银行账号，用于隔离本行数据
     * @param minMoney  金额下限（元），null 忽略（可选细化指标）
     * @param maxMoney  金额上限（元），null 忽略（可选细化指标）
     * @param repayment 期限（月），null/0 忽略（可选细化指标）
     */
    Page<Finance> getBankMatch(String bankUserName, BigDecimal minMoney, BigDecimal maxMoney, Integer repayment, int page, int pageSize);

    /** 银行工作台：本行数据概览聚合统计 */
    BankFinanceStats getBankStats(String bankUserName);

    /** 银行工作台：更新融资产品（仅本行；核心条款发布后锁定，仅可改介绍与电话） */
    void updateProduct(String userName, Integer productId, FinanceProductRequest request);

    /** 银行工作台：融资产品不支持删除，仅切换上下架状态（0在售 / 1暂停供应） */
    void setProductStatus(String userName, Integer productId, Integer status);

    /** 银行工作台：本行全部融资产品（含已暂停），供产品管理页展示 */
    Page<FinanceProduct> getBankProductList(String bankUserName, int page, int pageSize);
}