package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.dto.FinanceRequest;
import com.agri.platform.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 写工具:提交融资申请。previewOrExecute 仅生成确认 draft(不落库);execute 调 FinanceService.applyFinance 真正落库。
 *
 * <p>槽位填充:purpose / repaymentSource 必须 ≥15 字,否则 applyFinance 抛业务异常 → 编排层把异常文本作为
 * observation 回灌 → 模型继续追问农户(slot-filling),不在工具内部做校验。</p>
 *
 * <p>realName / phone 由后端按注册信息自动取(applyFinance 内部),前端/工具无需传,避免 PII 经由 LLM 流转。</p>
 */
@Component
@RequiredArgsConstructor
public class ApplyFinanceTool implements Tool {
    private final FinanceService financeService;

    public String name() { return "apply_finance"; }

    public String role() { return "farmer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("提交融资申请。必填槽位:productId(套餐ID)、money(金额)、purpose(申请原因,≥15字)、repaymentSource(还款来源,≥15字)。可选:repayment(期限,默认用套餐期限)、jointUserName(联合贷款人userName)。realName/phone 由后端按注册信息自动取,无需传。需用户确认。")
                .parameters(Map.of(
                        "productId", "integer",
                        "money", "number",
                        "purpose", "string",
                        "repaymentSource", "string",
                        "repayment", "integer",
                        "jointUserName", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        // 只生成预览,不落库。使用 StrUtil.format(regex-safe) 替代 replaceAll,避免 $ / \ 触发崩溃。
        return StrUtil.format("即将提交融资申请:套餐#{} 金额{}元 期限{}期\n申请原因:{}\n还款来源:{}\n联合贷款人:{}。确认执行?",
                args.get("productId"), args.get("money"),
                args.getOrDefault("repayment", "按套餐"), args.get("purpose"), args.get("repaymentSource"),
                args.getOrDefault("jointUserName", "无"));
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        FinanceRequest req = new FinanceRequest();
        req.setProductId(toInt(args.get("productId")));
        req.setMoney(toBigDecimal(args.get("money")));
        req.setRepayment(toInt(args.get("repayment")));
        req.setPurpose(String.valueOf(args.get("purpose")));                // applyFinance 校验 ≥15字
        req.setRepaymentSource(String.valueOf(args.get("repaymentSource"))); // applyFinance 校验 ≥15字
        req.setJointUserName1(args.get("jointUserName") == null ? null : String.valueOf(args.get("jointUserName")));
        // 内部按 userName 取 realName/phone;role/money/purpose/repaymentSource 校验在此抛
        financeService.applyFinance(ctx.getUserName(), req);
        return "融资申请已提交,等待银行审批";
    }

    private Integer toInt(Object o) {
        return o == null ? null : Integer.valueOf(o.toString());
    }

    private BigDecimal toBigDecimal(Object o) {
        return o == null ? null : new BigDecimal(o.toString());
    }
}
