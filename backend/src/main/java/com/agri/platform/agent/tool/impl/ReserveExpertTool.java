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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 写工具:预约专家咨询(表单卡)。字段与必填规则逐项对齐手动页 ReserveExpert.vue:
 * 必填=专家/农作物/面积/所在地区/详细地址/电话(正 ^1[3-9]\d{9}$)/土壤条件/作物条件/作物详情(≥10字);
 * 选填=期望时间/留言。提交时地址按手动页口径拼串(region+详细地址直接相连)。
 *
 * <p>与手动页唯一的便利差异:电话按注册资料预填(手动页同样预填 userStore.userInfo.phone)、
 * 所在地区/详细地址优先用模型提取值,否则预填默认地址簿条目——均为服务端注入,PII 不经 LLM;
 * 用户在表单里可改,改后以表单值为准。专家账号必须真实存在(ExpertService 查证)。</p>
 *
 * <p>draft 里的联系电话走 {@link PiiMasker} 脱敏(中间4位打星):draft 会进聊天历史表并在
 * 前端展示;执行时仍用真实号码。</p>
 */
@Component
@RequiredArgsConstructor
public class ReserveExpertTool implements Tool {
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

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
                        + "因此信息不全也直接调用,严禁向用户追问任何字段(时间/面积/农作物/土壤等都不要问)。"
                        + "expertName 必须用 list_experts 返回的真实专家账号,用户只说姓名时你自己换算;"
                        + "preferredTime '周X上午/下午'粒度即可(选填);plantName 农作物;plantArea 种植面积(如:50亩);"
                        + "plantCondition/plantDetail/soilCondition 用户提过就填,没提留空由表单收集(必填项用户会补);"
                        + "message 留言。电话/地址表单会按账号资料预填,无需向用户索要;"
                        + "province/city/area(区/县)用户提过地名就拆开传,没提留空由表单级联选择。")
                .parameters(Map.ofEntries(
                        Map.entry("expertName", "string"),
                        Map.entry("preferredTime", "string"),
                        Map.entry("plantName", "string"),
                        Map.entry("plantArea", "string"),
                        Map.entry("soilCondition", "string"),
                        Map.entry("plantCondition", "string"),
                        Map.entry("plantDetail", "string"),
                        Map.entry("province", "string"),
                        Map.entry("city", "string"),
                        Map.entry("area", "string"),
                        Map.entry("addressDetail", "string"),
                        Map.entry("phone", "string"),
                        Map.entry("message", "string")))
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
        String phone = Args.str(args.get("phone"));
        if (phone == null || phone.isEmpty()) phone = resolvePhone(ctx);
        return StrUtil.format("即将预约专家:{}\n农作物:{} 面积:{}\n地址:{} 联系电话:{}\n期望时间:{}(选填)\n土壤条件:{} 作物条件:{}\n问题详情:{}\n留言:{}\n确认执行?",
                expertDisplay,
                nz(args.get("plantName")), nz(args.get("plantArea")),
                nz(previewAddress(ctx, args)), masker.mask(phone),
                nzOr(args.get("preferredTime"), "未填写"),
                nz(args.get("soilCondition")), nz(args.get("plantCondition")),
                nz(args.get("plantDetail")),
                nzOr(args.get("message"), "无"));
    }

    /** 确认时校验(合并表单值后):必填规则与手动页 ReserveExpert.vue 完全一致。 */
    @Override
    public void validate(Map<String, Object> args) {
        require(Args.str(args.get("expertName")), "请选择专家");
        require(Args.str(args.get("plantName")), "请填写农作物(如:芒果)");
        require(Args.str(args.get("plantArea")), "请填写种植面积(如:50亩)");
        require(Args.str(args.get("province")), "请选择所在地区(省)");
        require(Args.str(args.get("city")), "请选择所在地区(市)");
        require(Args.str(args.get("area")), "请选择所在地区(区/县)");
        require(Args.str(args.get("addressDetail")), "请填写详细地址(街道/村组/门牌)");
        String phone = Args.str(args.get("phone"));
        // 电话:表单值有就验正则(手动页同款);空值允许——execute 会兜底账号注册手机号
        if (phone != null && !phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            throw new RuntimeException("请填写正确的手机号(11位,1开头)");
        }
        require(Args.str(args.get("soilCondition")), "请描述土壤条件(如:沙壤土、肥沃等)");
        require(Args.str(args.get("plantCondition")), "请描述作物条件(如:生长阶段、长势等)");
        String detail = Args.str(args.get("plantDetail"));
        if (detail == null || detail.isEmpty()) throw new RuntimeException("请详细描述作物情况");
        if (detail.length() < 10) throw new RuntimeException("作物详情不能少于10个字,请补充说明");
        // preferredTime/message 手动页即为选填,不校验
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
        // 电话按注册手机号预填(手动页同款行为);所在地区/详细地址有默认地址簿时预填、可改
        User u = userService.getUserByUserName(ctx.getUserName());
        String phone = Args.str(args.get("phone"));
        if ((phone == null || phone.isEmpty()) && u != null) phone = u.getPhone();
        String province = Args.str(args.get("province"));
        String city = Args.str(args.get("city"));
        String area = Args.str(args.get("area"));
        String detail = Args.str(args.get("addressDetail"));
        Address def = addressService.getDefaultAddress(ctx.getUserName());
        if (def != null) {
            if (province == null) province = def.getProvince();
            if (city == null) city = def.getCity();
            if (area == null) area = def.getArea();
            if (detail == null) detail = def.getAddressDetail();
        }
        String region = ((province == null ? "" : province + " ") + (city == null ? "" : city + " ")
                + (area == null ? "" : area)).trim();
        return List.of(
                field("expertName", "专家", "select", args, true, experts, null, null),
                field("plantName", "农作物", "text", args, true, null, "如:芒果", null),
                field("plantArea", "种植面积", "text", args, true, null, "如:50亩", null),
                FormField.builder().key("region").label("所在地区").type("region")
                        .value(region.isEmpty() ? null : region).required(true)
                        .placeholder(null).hint(null).build(),
                FormField.builder().key("addressDetail").label("详细地址").type("textarea")
                        .value(detail).required(true)
                        .placeholder("街道、村组、门牌等详细地址").hint(null).build(),
                FormField.builder().key("phone").label("电话").type("text")
                        .value(phone).required(true)
                        .placeholder("请输入联系电话").hint("已按注册资料预填,可修改").build(),
                field("preferredTime", "期望时间", "text", args, false, null, "如:下周三下午", "选填"),
                field("soilCondition", "土壤条件", "textarea", args, true, null, "如:沙壤土、肥沃等", null),
                field("plantCondition", "作物条件", "textarea", args, true, null, "如:生长阶段、长势等", null),
                field("plantDetail", "作物详情", "textarea", args, true, null, "请详细描述作物情况", "不少于10个字"),
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

    private String nzOr(Object v, String def) {
        String s = Args.str(v);
        return (s == null || s.isEmpty()) ? def : s;
    }

    /** 预览用地址:表单值优先(region+详细地址拼接,手动页 join 口径),空则回退资料地址。 */
    private String previewAddress(ToolContext ctx, Map<String, Object> args) {
        String built = buildAddress(args);
        return built != null ? built : resolveAddress(ctx);
    }

    /** 地址按手动页口径拼串:省+市+区+详细地址直接相连。 */
    private String buildAddress(Map<String, Object> args) {
        String province = Args.str(args.get("province"));
        String city = Args.str(args.get("city"));
        String area = Args.str(args.get("area"));
        String detail = Args.str(args.get("addressDetail"));
        if (province == null || city == null || area == null || detail == null) return null;
        return province + city + area + detail;
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
        req.setArea(nz(args.get("plantArea")));    // 种植面积(tb_reserve.area NOT NULL);键名 plantArea,area 已被区县占用
        // 电话:表单值(预填注册手机号,可改)优先,空则兜底注册资料
        String phone = Args.str(args.get("phone"));
        if (phone == null || phone.isEmpty()) phone = resolvePhone(ctx);
        req.setPhone(phone);
        // 地址:表单省市区+详细地址拼接(手动页口径);旧卡/异常路径无表单值时兜底资料地址
        String address = buildAddress(args);
        if (address == null) address = resolveAddress(ctx);
        req.setAddress(address);
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
            throw new RuntimeException("您的账号未登记联系电话,请在表单中填写");
        }
        return phone;
    }

    private String resolveAddress(ToolContext ctx) {
        User u = userService.getUserByUserName(ctx.getUserName());
        String addr = u != null ? u.getAddress() : null;
        if (addr != null && !addr.trim().isEmpty()) return addr;
        Address def = addressService.getDefaultAddress(ctx.getUserName());
        if (def != null) return def.getFullAddress();
        throw new RuntimeException("您的账号未登记地址,请在表单中填写所在地区和详细地址");
    }
}
