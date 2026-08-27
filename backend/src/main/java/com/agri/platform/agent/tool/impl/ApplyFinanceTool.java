package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.FormField;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 写工具:提交融资申请(表单卡)。previewOrExecute 只做套餐存在性/在售校验并生成 draft(不落库),
 * 金额/字数等必填校验在 validate(确认时、合并表单值后)执行——槽位由表单卡收集,模型无需追问。
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
                .description("提交融资申请(表单卡,需用户确认)。productId(套餐ID)、money(金额)、"
                        + "purpose(申请原因,≥15字)、repaymentSource(还款来源,≥15字)。"
                        + "可选:jointUserName(联合贷款人userName,必须是本平台农户账号)。"
                        + "信息不全也直接调用,缺失槽位由系统表单收集,严禁向用户追问任何字段;"
                        + "用户说不出套餐时 productId 也可留空(用户在表单下拉中自己选择)。"
                        + "期限由套餐决定无需传。realName/phone 由后端按注册信息自动取,无需传。")
                .parameters(Map.of(
                        "productId", "integer",
                        "money", "number",
                        "purpose", "string",
                        "repaymentSource", "string",
                        "jointUserName", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        // 套餐存在性/在售校验保留:模型编造套餐ID时在出卡前就纠偏(错误回灌,模型重新选)
        Integer productId = Args.toInt(args.get("productId"));
        FinanceProduct product = productId == null ? null : financeService.getProductById(productId);
        if (productId != null && product == null) {
            throw new RuntimeException("套餐#" + productId + " 不存在,请用 query_financing_products 重新选择");
        }
        if (product != null && product.getStatus() != null && product.getStatus() == 1) {
            throw new RuntimeException("套餐 " + product.getProductName() + " 已暂停供应,请选择其他套餐");
        }
        // 金额/原因/还款来源的必填与字数校验挪到 validate(确认时):preview 只管出卡,缺槽由表单收集
        // 使用 StrUtil.format(regex-safe) 替代 replaceAll,避免 $ / \ 触发崩溃
        return StrUtil.format("即将提交融资申请:套餐{}({}) 金额{}元 期限{}期(按套餐)\n申请原因:{}\n还款来源:{}\n联合贷款人:{}。确认执行?",
                productId == null ? "未选择(请在表单中选择)" : productId,
                product == null ? "" : product.getProductName(),
                nz(args.get("money")),
                product == null ? "-" : product.getRepayment(),
                nz(args.get("purpose")), nz(args.get("repaymentSource")),
                nzOr(args.get("jointUserName"), "无"));
    }

    /** 确认时校验(合并表单值后):必填/额度/字数齐全才放行 execute。 */
    @Override
    public void validate(Map<String, Object> args) {
        Integer productId = Args.toInt(args.get("productId"));
        if (productId == null) throw new RuntimeException("请选择融资套餐");
        FinanceProduct product = financeService.getProductById(productId);
        if (product == null) throw new RuntimeException("套餐#" + productId + " 不存在,请重新选择");
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
        // 联合贷款人存在性/角色校验:invite 在 execute 阶段才查,编造账号会让用户白确认一场
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
    }

    @Override
    public List<FormField> formFields(ToolContext ctx, Map<String, Object> args) {
        List<FormField.Option> products = new ArrayList<>();
        List<FinanceProduct> onSale = financeService.getProductList(1, 50).getRecords();
        if (onSale != null) {
            for (FinanceProduct p : onSale) {
                products.add(FormField.Option.builder()
                        .value(String.valueOf(p.getProductId()))
                        .label(StrUtil.format("{}·{}(额度{}元 利率{}期{})", p.getBankName(), p.getProductName(),
                                p.getMoney(), p.getRate() == null ? "-" : p.getRate() + "%",
                                p.getRepayment() == null ? "" : " " + p.getRepayment() + "期"))
                        .build());
            }
        }
        return List.of(
                field("productId", "融资套餐", "select", args, true, products, null, null),
                field("money", "申请金额(元)", "number", args, true, null, null, "不得超过套餐额度上限"),
                field("purpose", "申请原因", "textarea", args, true, null, "资金用途说明", "不少于15个字"),
                field("repaymentSource", "还款来源", "textarea", args, true, null, "如:农产品销售收入、补贴等", "不少于15个字"),
                field("jointUserName", "联合贷款人账号", "text", args, false, null, "选填,对方须是本平台农户", null));
    }

    private FormField field(String key, String label, String type, Map<String, Object> args,
                            boolean required, List<FormField.Option> options, String placeholder, String hint) {
        return FormField.builder().key(key).label(label).type(type)
                .value(Args.str(args.get(key))).required(required).options(options)
                .placeholder(placeholder).hint(hint).build();
    }

    private String nz(Object o) {
        String s = Args.str(o);
        return s == null || s.isEmpty() ? "未填写" : s;
    }

    private String nzOr(Object o, String def) {
        String s = Args.str(o);
        return s == null || s.isEmpty() ? def : s;
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        FinanceRequest req = new FinanceRequest();
        req.setProductId(Args.toInt(args.get("productId")));
        req.setMoney(Args.toBigDecimal(args.get("money")));
        // 期限不传:applyFinance 落库固定用套餐期限,传了也会被忽略
        req.setPurpose(String.valueOf(args.get("purpose")));                // validate 已校验 ≥15字
        req.setRepaymentSource(String.valueOf(args.get("repaymentSource"))); // validate 已校验 ≥15字
        req.setJointUserName1(args.get("jointUserName") == null ? null : String.valueOf(args.get("jointUserName")));
        // 内部按 userName 取 realName/phone;role/money/purpose/repaymentSource 校验在此抛
        financeService.applyFinance(ctx.getUserName(), req);
        return "融资申请已提交,等待银行审批";
    }
}
