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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 写工具:提交融资申请(表单卡)。业务规则严格对齐手动页 farmer/Finance.vue:
 * <ul>
 *   <li><strong>金额按套餐固定</strong>:tb_finance.money 落库时直接取套餐额度(product.money),
 *       表单里不出现可编辑的金额框——与手动设计的"额度随套餐"一致,模型也不收 money 参数。</li>
 *   <li><strong>联合贷款人=选联系人发起邀请</strong>:两个下拉(jointUserName1/2)来自
 *       {@link UserService#listContacts}(农户联系人,排除自己),label 展示姓名+电话供辨认;
 *       提交后由 applyFinance 创建邀请,对方同意才回填资料——不是手填账号。</li>
 *   <li>期限按套餐(repayment);realName/phone/idNum 由后端按注册信息回填(PII 不过 LLM)。</li>
 * </ul>
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
                .description("提交融资申请(表单卡,需用户确认)。productId(套餐ID)、purpose(申请原因,≥15字)、"
                        + "repaymentSource(还款来源,≥15字)。"
                        + "可选:jointUserName1/jointUserName2(联合贷款人的userName,必须从本平台农户账号中选,最多2人)。"
                        + "金额由套餐固定决定,不需要也无法提供;期限按套餐;realName/phone由系统自动取注册资料,均无需传。"
                        + "信息不全也直接调用,缺失槽位由系统表单收集,严禁向用户追问任何字段;"
                        + "用户说不出套餐时 productId 留空(用户在表单下拉中自己选择)。")
                .parameters(Map.of(
                        "productId", "integer",
                        "purpose", "string",
                        "repaymentSource", "string",
                        "jointUserName1", "string",
                        "jointUserName2", "string"))
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
        // 使用 StrUtil.format(regex-safe) 替代 replaceAll,避免 $ / \ 触发崩溃
        String head = productId == null ? "未选择(请在表单中选择)"
                : StrUtil.format("#{} {}", productId, productTitle(product));
        return StrUtil.format("即将提交融资申请:套餐{}\n申请金额:{}元(按套餐额度固定) 还款期限:{}期(按套餐)\n申请原因:{}\n还款来源:{}\n联合贷款人:{},{}(提交后对方将收到邀请)\n确认执行?",
                head,
                product == null || product.getMoney() == null ? "-" : product.getMoney(),
                product == null ? "-" : product.getRepayment(),
                nz(args.get("purpose")), nz(args.get("repaymentSource")),
                nzOr(args.get("jointUserName1"), "无"),
                nzOr(args.get("jointUserName2"), "无"));
    }

    /** 确认时校验(合并表单值后):必填齐全、联合人真实存在且互不重复才放行 execute。 */
    @Override
    public void validate(Map<String, Object> args) {
        Integer productId = Args.toInt(args.get("productId"));
        if (productId == null) throw new RuntimeException("请选择融资套餐");
        FinanceProduct product = financeService.getProductById(productId);
        if (product == null) throw new RuntimeException("套餐#" + productId + " 不存在,请重新选择");
        if (product.getStatus() != null && product.getStatus() == 1) {
            throw new RuntimeException("套餐 " + product.getProductName() + " 已暂停供应,请选择其他套餐");
        }
        // 金额落库即套餐额度;套餐本身必须配好额度,否则库端 NOT NULL 会炸出原始 SQL 异常
        if (product.getMoney() == null || product.getMoney().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("该套餐未配置可用额度,请更换套餐");
        }
        String purpose = Args.str(args.get("purpose"));
        if (purpose == null || purpose.length() < 15) {
            throw new RuntimeException("申请原因不能少于15个字,请补充说明资金用途");
        }
        String repaymentSource = Args.str(args.get("repaymentSource"));
        if (repaymentSource == null || repaymentSource.length() < 15) {
            throw new RuntimeException("还款来源不能少于15个字,请补充说明还款来源(如销售收入、补贴等)");
        }
        // 联合贷款人:下拉已限定真实农户;此处兜底校验防模型绕过表单传编造账号,
        // 让用户白确认一场(invite 在 execute 阶段才查)。规则同手动页:必须农户、两人不能重复
        String j1 = Args.str(args.get("jointUserName1"));
        String j2 = Args.str(args.get("jointUserName2"));
        checkJoint(j1);
        checkJoint(j2);
        if (j1 != null && j2 != null && j1.equals(j2)) {
            throw new RuntimeException("两个联合贷款人不能是同一个人");
        }
    }

    private void checkJoint(String jointUserName) {
        if (jointUserName == null || jointUserName.isEmpty()) return;
        User joint = userService.getUserByUserName(jointUserName);
        if (joint == null) {
            throw new RuntimeException("联合贷款人账号 " + jointUserName + " 不存在,请在表单下拉中重新选择");
        }
        if (!"farmer".equals(joint.getRole())) {
            throw new RuntimeException("联合贷款人必须是农户账号," + jointUserName + " 不是农户");
        }
    }

    @Override
    public List<FormField> formFields(ToolContext ctx, Map<String, Object> args) {
        List<FormField.Option> products = new ArrayList<>();
        List<FinanceProduct> onSale = financeService.getProductList(1, 50).getRecords();
        if (onSale != null) {
            for (FinanceProduct p : onSale) {
                // product_name/bank_name 都可能为 NULL(如#4只有银行名),按手动页语义兜底:
                // 有名称用"银行·名称",名称缺失退回银行名本身,避免"· null"
                String bank = StrUtil.nullToEmpty(p.getBankName());
                String name = StrUtil.nullToEmpty(p.getProductName());
                String title = name.isEmpty() ? bank
                        : (bank.isEmpty() || bank.equals(name) ? name : bank + "·" + name);
                products.add(FormField.Option.builder()
                        .value(String.valueOf(p.getProductId()))
                        .label(StrUtil.format("{}(额度{}元 利率{} {}期)", title,
                                p.getMoney(), p.getRate() == null ? "-" : p.getRate() + "%",
                                p.getRepayment() == null ? "-" : p.getRepayment()))
                        .build());
            }
        }
        // 联合贷款人候选 = 农户联系人(排除自己),姓名+电话展示便于辨认,值用账号。
        // 对齐手动页:"提交后对方会收到邀请通知,对方同意后自动绑定资料"
        List<FormField.Option> contacts = new ArrayList<>();
        try {
            List<User> list = userService.listContacts(ctx.getUserName(), "farmer");
            if (list != null) {
                for (User u : list) {
                    contacts.add(FormField.Option.builder()
                            .value(u.getUserName())
                            .label(StrUtil.format("{}({}) {}", u.getRealName() == null ? u.getUserName() : u.getRealName(),
                                    u.getUserName(), u.getPhone() == null ? "" : u.getPhone()))
                            .build());
                }
            }
        } catch (Exception ignore) {
            // 联系人加载失败只是没有候选,不影响主流程
        }
        String inviteHint = "选填。提交后对方收到邀请,同意后其资料自动绑定到本申请";
        return List.of(
                field("productId", "融资套餐", "select", args, true, products, null, "额度与利率随套餐固定,选择后即为申请金额"),
                field("purpose", "申请原因", "textarea", args, true, null, "资金用途说明", "不少于15个字"),
                field("repaymentSource", "还款来源", "textarea", args, true, null, "如:农产品销售收入、补贴等", "不少于15个字"),
                field("jointUserName1", "联合贷款人1", "select", args, false, contacts, null, inviteHint),
                field("jointUserName2", "联合贷款人2", "select", args, false, contacts, null, "最多选2人,不能重复"));
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
        // validate 刚刚预检过同一 pendingId 的合并参数,此处置复查询套餐只多一次主键查;
        // 好处:金额严格以套餐为准,不存在"模型传多少落多少"的口子
        Integer productId = Args.toInt(args.get("productId"));
        FinanceProduct product = financeService.getProductById(productId);
        if (product == null) throw new RuntimeException("套餐#" + productId + " 不存在,请重新选择");
        FinanceRequest req = new FinanceRequest();
        req.setProductId(productId);
        req.setMoney(product.getMoney());      // 金额=套餐额度(手动设计如此)
        req.setPurpose(String.valueOf(args.get("purpose")));                 // validate 已校验 ≥15字
        req.setRepaymentSource(String.valueOf(args.get("repaymentSource"))); // validate 已校验 ≥15字
        req.setJointUserName1(toStrOrNull(args.get("jointUserName1")));
        req.setJointUserName2(toStrOrNull(args.get("jointUserName2")));
        // 内部按 userName 取 realName/phone;role/money/purpose/repaymentSource 校验在此抛
        financeService.applyFinance(ctx.getUserName(), req);
        return StrUtil.format("融资申请已提交(套餐:{} 申请金额:{}元),等待银行审批;联合贷款人会收到邀请通知",
                productTitle(product), product.getMoney());
    }

    /** 产品名可能为 NULL(如#4 邮政轻松贷):有名称用名称,缺失退回银行名,避免"套餐:null"。 */
    private String productTitle(FinanceProduct p) {
        String name = StrUtil.nullToEmpty(p.getProductName());
        String bank = StrUtil.nullToEmpty(p.getBankName());
        return name.isEmpty() ? (bank.isEmpty() ? "#" + p.getProductId() : bank) : name;
    }

    private String toStrOrNull(Object o) {
        String s = Args.str(o);
        return s == null || s.isEmpty() ? null : s;
    }
}
