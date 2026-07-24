package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.BankFinanceStats;
import com.agri.platform.dto.FinanceApprovalRequest;
import com.agri.platform.dto.FinanceProductRequest;
import com.agri.platform.dto.FinanceRequest;
import com.agri.platform.dto.FinancingIntentionRequest;
import com.agri.platform.entity.Finance;
import com.agri.platform.entity.FinanceProduct;
import com.agri.platform.entity.FinancingIntention;
import com.agri.platform.service.FinanceService;
import com.agri.platform.service.FinancingIntentionService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @Autowired
    private FinancingIntentionService intentionService;

    @Autowired
    private JwtUtil jwtUtil;

    // 融资产品相关
    @PostMapping("/product/publish")
    public Result<String> publishProduct(@RequestHeader("Authorization") String token,
                                          @RequestBody FinanceProductRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        financeService.publishProduct(userName, request);
        return Result.success("产品发布成功");
    }

    @GetMapping("/product/list")
    public Result<Page<FinanceProduct>> productList(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(financeService.getProductList(page, pageSize));
    }

    @GetMapping("/product/{productId}")
    public Result<FinanceProduct> productDetail(@PathVariable Integer productId) {
        return Result.success(financeService.getProductById(productId));
    }

    @PostMapping("/apply")
    public Result<String> apply(@RequestHeader("Authorization") String token,
                                @RequestBody FinanceRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        financeService.applyFinance(userName, request);
        return Result.success("申请提交成功");
    }

    @GetMapping("/apply/list")
    public Result<Page<Finance>> myApply(@RequestHeader("Authorization") String token,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(financeService.getApplyList(userName, page, pageSize));
    }

    @GetMapping("/apply/bank")
    public Result<Page<Finance>> bankApprovalList(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(financeService.getBankApprovalList(page, pageSize));
    }

    @PutMapping("/apply/{financeId}")
    public Result<String> approve(@RequestHeader("Authorization") String token,
                                  @PathVariable Integer financeId,
                                  @RequestBody FinanceApprovalRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        financeService.approveFinance(financeId, userName, request);
        return Result.success("审批完成");
    }

    @PutMapping("/apply/update/{financeId}")
    public Result<String> updateFinance(@PathVariable Integer financeId,
                                        @RequestHeader("Authorization") String token,
                                        @RequestBody FinanceRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        financeService.updateFinance(financeId, userName, request);
        return Result.success("申请更新成功");
    }

    @DeleteMapping("/apply/{financeId}")
    public Result<String> cancelApply(@RequestHeader("Authorization") String token,
                                      @PathVariable Integer financeId) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        financeService.deleteFinance(financeId, userName);
        return Result.success("申请已撤销");
    }

    // ==================== 银行工作台 ====================

    /**
     * 银行工作台 - 融资申请列表（全状态，支持筛选）
     */
    @GetMapping("/bank/applications")
    public Result<Page<Finance>> bankApplications(@RequestHeader("Authorization") String token,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer productId,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(financeService.getBankApplications(userName, status, keyword, productId, startDate, endDate, page, pageSize));
    }

    /**
     * 银行工作台 - 数据概览聚合统计
     */
    @GetMapping("/bank/stats")
    public Result<BankFinanceStats> bankStats(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(financeService.getBankStats(userName));
    }

    /**
     * 银行工作台 - 智能匹配：在「申请中」的真实融资申请里按金额区间/期限/作物关键字筛选，
     * 回填 productName + credit + productNames（该农户农产品），按信用分优先展示。
     * 与 /intention/match（基于无人写入的融资意向表，恒空）不同，此处基于真实 tb_finance 数据。
     */
    @GetMapping("/bank/match")
    public Result<Page<Finance>> bankMatch(@RequestHeader("Authorization") String token,
                                            @RequestParam(required = false) BigDecimal minMoney,
                                            @RequestParam(required = false) BigDecimal maxMoney,
                                            @RequestParam(required = false) Integer repayment,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(financeService.getBankMatch(userName, minMoney, maxMoney, repayment, page, pageSize));
    }

    /**
     * 银行工作台 - 本行全部融资产品（含已暂停），供产品管理页展示
     */
    @GetMapping("/bank/product/list")
    public Result<Page<FinanceProduct>> bankProductList(@RequestHeader("Authorization") String token,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int pageSize) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(financeService.getBankProductList(userName, page, pageSize));
    }

    /**
     * 银行工作台 - 更新融资产品（仅银行角色；核心条款发布后锁定，仅可改介绍与电话）
     */
    @PutMapping("/bank/product/{productId}")
    public Result<String> updateProduct(@RequestHeader("Authorization") String token,
                                         @PathVariable Integer productId,
                                         @RequestBody FinanceProductRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        financeService.updateProduct(userName, productId, request);
        return Result.success("产品更新成功");
    }

    /**
     * 银行工作台 - 切换融资产品上下架状态（0在售 / 1暂停供应）。产品不支持删除。
     */
    @PutMapping("/bank/product/{productId}/status")
    public Result<String> setProductStatus(@RequestHeader("Authorization") String token,
                                            @PathVariable Integer productId,
                                            @RequestParam Integer status) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        financeService.setProductStatus(userName, productId, status);
        return Result.success(status != null && status == 1 ? "已暂停供应" : "已恢复供应");
    }

    // 融资意向相关
    @PostMapping("/intention")
    public Result<String> submitIntention(@RequestHeader("Authorization") String token,
                                           @RequestBody FinancingIntentionRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        intentionService.submitIntention(userName, request);
        return Result.success("意向提交成功");
    }

    @GetMapping("/intention/match")
    public Result<Page<FinancingIntention>> matchList(@RequestParam(required = false) Integer minAmount,
                                                       @RequestParam(required = false) Integer maxAmount,
                                                       @RequestParam(required = false) String item,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(intentionService.getMatchedList(
                minAmount != null ? minAmount : 0,
                maxAmount != null ? maxAmount : 0,
                item,
                page,
                pageSize
        ));
    }
}