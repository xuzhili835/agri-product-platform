package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.dto.FinanceRequest;
import com.agri.platform.entity.FinanceProduct;
import com.agri.platform.entity.User;
import com.agri.platform.service.FinanceService;
import com.agri.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 写工具:提交融资申请。previewOrExecute 校验必填槽位并生成确认 draft(不落库);
 * execute 调 FinanceService.applyFinance 真正落库。
 *
 * <p>槽位校验必须在 preview 阶段做并抛业务异常:编排层把异常文本作为 observation 回灌,
 * 模型继续向农户追问补槽(slot-filling)。此前 preview 不校验,缺少槽位/purpose 不足15字
 * 也能弹确认卡,用户点"确认执行"后才在 execute 阶段失败——白确认一场且历史里落
 * "[执行异常] 申请原因不能少于15个字"。</p>
 *
 * <p><strong>不收 repayment(期限)参数</strong>:applyFinance 落库时固定用套餐期限
 * (finance.setRepayment(product.getRepayment())),用户传的期限会被忽略——此前工具收这个参数,
 * draft 显示的期限与实际落库不符,属于"假执行"。</p>
 *
 * <p>realName / phone 由后端按注册信息自动取(applyFinance 内部),前端/工具无需传,避免 PII 经由 LLM 流转。</p>
 */
@Component
@RequiredArgsConstructor
public class ApplyFinanceTool implements Tool {
    private final FinanceService financeService;
    private final UserService userService;

    public String name() { return "apply_finance"; }

    public String role() { return "farmer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("提交融资申请。必填槽位:productId(套餐ID)、money(金额,不得超过套餐额度上限)、"
                        + "purpose(申请原因,≥15字)、repaymentSource(还款来源,≥15字)。"
                        + "可选:jointUserName(联合贷款人userName,必须是本平台农户账号)。"
                        + "期限由套餐决定无需传。realName/phone 由后端按注册信息自动取,无需传。需用户确认。")
                .parameters(Map.of(
                        "productId", "integer",
                        "money", "number",
                        "purpose", "string",
                        "repaymentSource", "string",
                        "jointUserName", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        Integer productId = Args.toInt(args.get("productId"));
        if (productId == null) throw new RuntimeException("缺少融资套餐ID,请先用 query_financing_products 查询套餐");
        FinanceProduct product = financeService.getProductById(productId);
        if (product == null) throw new RuntimeException("套餐#" + productId + " 不存在,请用 query_financing_products 重新选择");
        if (product.getStatus() != null && product.getStatus() == 1) {
            throw new RuntimeException("套餐 " + product.getProductName() + " 已暂停供应,请选择其他套餐");
        }
        BigDecimal money = Args.toBigDecimal(args.get("money"));
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("请填写正确的申请金额(大于0)");
        }
        // 套餐额度上限:applyFinance 不校验(只查>0),前端表单外的入口必须自校验,
        // 否则超上限金额也能确认提交成功
        if (product.getMoney() != null && money.compareTo(product.getMoney()) > 0) {
            throw new RuntimeException(StrUtil.format("申请金额{}元超过该套餐额度上限{}元,请调低金额或更换套餐",
                    money, product.getMoney()));
        }
        String purpose = Args.str(args.get("purpose"));
        if (purpose == null || purpose.length() < 15) {
            throw new RuntimeException("申请原因不能少于15个字,请补充说明资金用途");
        }
        String repaymentSource = Args.str(args.get("repaymentSource"));
        if (repaymentSource == null || repaymentSource.length() < 15) {
            throw new RuntimeException("还款来源不能少于15个字,请补充说明还款来源(如销售收入、补贴等)");
        }
        // 联合贷款人:存在性/角色校验前移到 preview——invite 在 execute 阶段才查,
        // 编造账号会让用户白确认一场
        String jointUserName = Args.str(args.get("jointUserName"));
        if (jointUserName != null && !jointUserName.isEmpty()) {
            User joint = userService.getUserByUserName(jointUserName);
            if (joint == null) {
                throw new RuntimeException("联合贷款人账号 " + jointUserName + " 不存在,请确认对方注册账号");
            }
            if (!"farmer".equals(joint.getRole())) {
                throw new RuntimeException("联合贷款人必须是农户账号," + jointUserName + " 不是农户");
            }
        }
        // 使用 StrUtil.format(regex-safe) 替代 replaceAll,避免 $ / \ 触发崩溃
        return StrUtil.format("即将提交融资申请:套餐{}({}) 金额{}元 期限{}期(按套餐)\n申请原因:{}\n还款来源:{}\n联合贷款人:{}。确认执行?",
                productId, product.getProductName(), money,
                product.getRepayment(), purpose, repaymentSource,
                jointUserName == null || jointUserName.isEmpty() ? "无" : jointUserName);
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        FinanceRequest req = new FinanceRequest();
        req.setProductId(Args.toInt(args.get("productId")));
        req.setMoney(Args.toBigDecimal(args.get("money")));
        // 期限不传:applyFinance 落库固定用套餐期限,传了也会被忽略
        req.setPurpose(String.valueOf(args.get("purpose")));                // preview 已校验 ≥15字
        req.setRepaymentSource(String.valueOf(args.get("repaymentSource"))); // preview 已校验 ≥15字
        req.setJointUserName1(args.get("jointUserName") == null ? null : String.valueOf(args.get("jointUserName")));
        // 内部按 userName 取 realName/phone;role/money/purpose/repaymentSource 校验在此抛
        financeService.applyFinance(ctx.getUserName(), req);
        return "融资申请已提交,等待银行审批";
    }
}
