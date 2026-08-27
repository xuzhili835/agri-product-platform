package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.FormField;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.agent.util.PiiMasker;
import com.agri.platform.dto.ReserveRequest;
import com.agri.platform.entity.Address;
import com.agri.platform.entity.Expert;
import com.agri.platform.entity.User;
import com.agri.platform.service.AddressService;
import com.agri.platform.service.ExpertService;
import com.agri.platform.service.ReserveService;
import com.agri.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 写工具:预约专家咨询。参数对齐 ReserveService.makeReservation 的必填项;
 * 电话/地址由后端按用户资料自动取(PII 不过 LLM),preview 校验必填槽位并解析联系方式生成 draft,
 * execute 落库。
 *
 * <p>preview 阶段同时校验专家账号真实存在(ExpertService 查证)——makeReservation 不校验 expertName,
 * 此前唯一防线是 system prompt,模型编造专家名会落一条指向不存在账号的孤儿预约。</p>
 *
 * <p>draft 里的联系电话走 {@link PiiMasker} 脱敏(中间4位打星):draft 会进聊天历史表并在
 * 前端展示,存明文会放大越权读取(已另修)时的泄露面;执行时仍用真实号码。</p>
 */
@Component
@RequiredArgsConstructor
public class ReserveExpertTool implements Tool {
    private final ReserveService reserveService;
    private final UserService userService;
    private final ExpertService expertService;
    private final AddressService addressService;
    private final PiiMasker masker;

    public String name() { return "reserve_expert"; }

    public String role() { return "common"; }   // 业务层不限制角色,买家也可预约(此前误配为 farmer)

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("预约专家咨询(表单卡):调用后系统会弹出可编辑表单,已提取的信息预填,缺失的槽位由用户在表单里补——"
                        + "因此信息不全也直接调用,严禁向用户追问任何字段(时间/面积/农作物等都不要问)。"
                        + "expertName 必须用 list_experts 返回的真实专家账号,用户只说姓名时你自己换算;"
                        + "preferredTime '周X上午/下午'粒度即可;plantName 农作物;area 种植面积(如:50亩);"
                        + "plantCondition/plantDetail 用户提过就填;message 留言。电话和地址系统自动取用户资料。")
                .parameters(Map.of(
                        "expertName", "string",
                        "preferredTime", "string",
                        "plantName", "string",
                        "plantCondition", "string",
                        "plantDetail", "string",
                        "area", "string",
                        "message", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        String expertName = Args.str(args.get("expertName"));
        String expertDisplay = "未选择(请在表单中选择)";
        if (expertName != null && !expertName.isEmpty()) {
            Expert expert = expertService.getExpertByUserName(expertName);
            if (expert == null) {
                throw new RuntimeException("专家账号 " + expertName + " 不存在,请用 list_experts 查询真实专家账号");
            }
            expertDisplay = expertName + "(" + expert.getRealName() + ")";
        }
        // 槽位完整性不在 preview 卡:缺失槽由表单卡收集(模型无需追问),必填校验挪到 validate(确认时)
        String phone = resolvePhone(ctx);
        String address = resolveAddress(ctx);
        return StrUtil.format("即将预约专家:{} 时间:{}\n农作物:{} 面积:{} 状况:{}\n问题:{}\n联系电话:{} 地址:{}\n确认执行?",
                expertDisplay, nz(args.get("preferredTime")), nz(args.get("plantName")), nz(args.get("area")),
                nz(args.get("plantCondition")), nz(args.get("plantDetail")),
                masker.mask(phone), address);
    }

    /** 确认时校验(合并表单值后):必填槽位齐全才放行 execute。 */
    @Override
    public void validate(Map<String, Object> args) {
        require(Args.str(args.get("expertName")), "请选择专家");
        require(Args.str(args.get("preferredTime")), "请填写期望时间(如:下周三下午)");
        require(Args.str(args.get("plantName")), "请填写农作物(如:芒果)");
        require(Args.str(args.get("area")), "请填写种植面积(如:50亩)");
    }

    @Override
    public List<FormField> formFields(ToolContext ctx, Map<String, Object> args) {
        List<FormField.Option> experts = new ArrayList<>();
        List<Expert> all = expertService.getAllExperts();
        if (all != null) {
            experts = all.stream()
                    .map(e -> FormField.Option.builder().value(e.getUserName())
                            .label(e.getRealName() + "(" + e.getProfession() + ")").build())
                    .collect(Collectors.toList());
        }
        return List.of(
                field("expertName", "专家", "select", args, true, experts, null, null),
                field("preferredTime", "期望时间", "text", args, true, null, "如:下周三下午", null),
                field("plantName", "农作物", "text", args, true, null, "如:芒果", null),
                field("area", "种植面积", "text", args, true, null, "如:50亩", null),
                field("plantCondition", "作物当前状况", "textarea", args, false, null, null, "选填"),
                field("plantDetail", "问题描述", "textarea", args, false, null, null, "选填"),
                field("message", "给专家的留言", "text", args, false, null, null, "选填"));
    }

    private FormField field(String key, String label, String type, Map<String, Object> args,
                            boolean required, List<FormField.Option> options, String placeholder, String hint) {
        return FormField.builder().key(key).label(label).type(type)
                .value(Args.str(args.get(key))).required(required).options(options)
                .placeholder(placeholder).hint(hint).build();
    }

    /** 描述性字段兜底:空白→"未填写"(tb_reserve 相应列 NOT NULL,null 会在确认执行时 SQL 失败)。 */
    private String nz(Object v) {
        String s = Args.str(v);
        return (s == null || s.isEmpty()) ? "未填写" : s;
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        ReserveRequest req = new ReserveRequest();
        req.setExpertName(Args.str(args.get("expertName")));
        req.setPreferredTime(Args.str(args.get("preferredTime")));
        req.setMessage(Args.str(args.get("message")));
        req.setPlantName(Args.str(args.get("plantName")));
        req.setSoilCondition(nz(args.get("soilCondition")));
        req.setPlantCondition(nz(args.get("plantCondition")));
        req.setPlantDetail(nz(args.get("plantDetail")));
        req.setArea(Args.str(args.get("area")));   // 种植面积(tb_reserve.area NOT NULL,漏传会确认后 SQL 失败)
        // 电话/地址自动取用户资料,不经过 LLM
        req.setPhone(resolvePhone(ctx));
        req.setAddress(resolveAddress(ctx));
        reserveService.makeReservation(ctx.getUserName(), req);
        return "专家预约已提交,可在'我的预约'中查看记录";
    }

    private void require(String v, String msg) {
        if (v == null || v.trim().isEmpty()) throw new RuntimeException(msg);
    }

    private String resolvePhone(ToolContext ctx) {
        User u = userService.getUserByUserName(ctx.getUserName());
        String phone = u != null ? u.getPhone() : null;
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("您的账号未登记联系电话,请先在个人中心补充后再预约");
        }
        return phone;
    }

    private String resolveAddress(ToolContext ctx) {
        User u = userService.getUserByUserName(ctx.getUserName());
        String addr = u != null ? u.getAddress() : null;
        if (addr != null && !addr.trim().isEmpty()) return addr;
        Address def = addressService.getDefaultAddress(ctx.getUserName());
        if (def != null) return def.getFullAddress();
        throw new RuntimeException("您的账号未登记地址,请先在个人中心补充后再预约");
    }
}
