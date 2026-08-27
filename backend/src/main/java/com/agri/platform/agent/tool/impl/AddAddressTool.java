package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.FormField;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.tool.LatestAddressCache;
import com.agri.platform.agent.util.Args;
import com.agri.platform.agent.util.PiiMasker;
import com.agri.platform.dto.AddressRequest;
import com.agri.platform.entity.User;
import com.agri.platform.service.AddressService;
import com.agri.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 写工具:新增收货地址(表单卡,走确认门)。与平台地址簿的三级区域(省/市/区)+详细地址结构对齐——
 * 此前 place_order 接受自由文本地址,用户随口一句"长沙流通县"直接进订单,
 * 既不落地址簿也不是规范的三级区域。新地址一律先经本工具落地址簿,再用 地址簿#编号 下单。
 *
 * <p>consignee/phone 表单预填账号注册的姓名与手机号(服务端注入,不经 LLM 流转);
 * 必填校验在 validate(确认时)执行,槽位由表单卡收集,模型无需追问。</p>
 */
@Component
@RequiredArgsConstructor
public class AddAddressTool implements Tool {
    /** 手机号规则与手动页 AddressManage.vue 同款:^1[3-9]\d{9}$。 */
    private static final java.util.regex.Pattern PHONE = java.util.regex.Pattern.compile("^1[3-9]\\d{9}$");

    private final AddressService addressService;
    private final UserService userService;
    private final PiiMasker masker;
    private final LatestAddressCache latestAddressCache;

    public String name() { return "add_address"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("新增收货地址到地址簿(表单卡,需用户确认)。province(省)、city(市)、area(区/县)、"
                        + "addressDetail(街道门牌详细地址);consignee(收件人)/phone(手机号)留空时系统自动使用"
                        + "账号注册的姓名和手机号,不要向用户询问;isDefault(是否设为默认,布尔)。"
                        + "信息不全也直接调用,缺失槽位由系统表单收集,严禁向用户追问任何字段。"
                        + "用户想用新地址下单时先调本工具新增,成功后 place_order 的 address 传'新地址'即可"
                        + "引用刚新增的地址(也支持'地址簿#编号')。")
                .parameters(Map.of(
                        "province", "string",
                        "city", "string",
                        "area", "string",
                        "addressDetail", "string",
                        "consignee", "string",
                        "phone", "string",
                        "isDefault", "boolean"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        // 必填校验挪到 validate(确认时):preview 只管出卡,缺槽由表单收集,模型无需追问
        String province = Args.str(args.get("province"));
        String city = Args.str(args.get("city"));
        String area = Args.str(args.get("area"));
        String detail = stripAreaPrefix(Args.str(args.get("addressDetail")), area, city);
        // 收件人/手机号:用户给了就用;没给自动取账号注册资料(表单里同样预填注册资料)
        User u = userService.getUserByUserName(ctx.getUserName());
        String consignee = Args.str(args.get("consignee"));
        if ((consignee == null || consignee.isEmpty()) && u != null
                && u.getRealName() != null && !u.getRealName().trim().isEmpty()) {
            consignee = u.getRealName().trim();
        }
        String phone = Args.str(args.get("phone"));
        if ((phone == null || phone.isEmpty()) && u != null
                && u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
            phone = u.getPhone().trim();
        }
        boolean def = Boolean.parseBoolean(String.valueOf(args.getOrDefault("isDefault", "false")));
        return StrUtil.format("即将新增收货地址:\n{} {} {} {}\n收件人:{} 电话:{}{}\n确认执行?",
                nz(province), nz(city), nz(area), nz(detail),
                nz(consignee), phone == null ? "未填写" : masker.mask(phone),
                def ? "(设为默认地址)" : "");
    }

    /** 确认时校验(合并表单值后):三级区域+详细地址必填才放行 execute。 */
    @Override
    public void validate(Map<String, Object> args) {
        String province = Args.str(args.get("province"));
        String city = Args.str(args.get("city"));
        String area = Args.str(args.get("area"));
        String detail = stripAreaPrefix(Args.str(args.get("addressDetail")), area, city);
        if (province == null || province.isEmpty()) throw new RuntimeException("请选择省份");
        if (city == null || city.isEmpty()) throw new RuntimeException("请选择城市");
        if (area == null || area.isEmpty()) throw new RuntimeException("请选择区/县");
        if (detail == null || detail.isEmpty()) throw new RuntimeException("请填写详细地址(街道/门牌号)");
        // 手机号:手动页正则同款;空值允许(将自动回填注册资料),填了就必须合法
        String phone = Args.str(args.get("phone"));
        if (phone != null && !phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            throw new RuntimeException("请填写正确的手机号(11位,1开头)");
        }
    }

    @Override
    public List<FormField> formFields(ToolContext ctx, Map<String, Object> args) {
        // 省市区组装为 region 值("省 市 区"):前端 cascader 预填、提交时拆回三个字段
        String province = Args.str(args.get("province"));
        String city = Args.str(args.get("city"));
        String area = Args.str(args.get("area"));
        String region = (province == null ? "" : province + " ") + (city == null ? "" : city + " ")
                + (area == null ? "" : area);
        // 收件人/手机号:参数给了用参数,没给预填账号注册资料(服务端注入,不经 LLM)
        User u = userService.getUserByUserName(ctx.getUserName());
        String consignee = Args.str(args.get("consignee"));
        if ((consignee == null || consignee.isEmpty()) && u != null) consignee = u.getRealName();
        String phone = Args.str(args.get("phone"));
        if ((phone == null || phone.isEmpty()) && u != null) phone = u.getPhone();
        boolean profileHas = consignee != null && !consignee.isEmpty() && phone != null && phone.length() == 11;
        return List.of(
                FormField.builder().key("region").label("所在地区").type("region")
                        .value(region.trim().isEmpty() ? null : region.trim()).required(true)
                        .placeholder(null).hint(null).build(),
                field("addressDetail", "详细地址", "text", args, true, "街道、门牌号", null),
                FormField.builder().key("consignee").label("收件人").type("text")
                        .value(consignee).required(!profileHas)
                        .placeholder(profileHas ? null : "账号未登记,请填写")
                        .hint(profileHas ? "已按注册资料预填" : null).build(),
                FormField.builder().key("phone").label("手机号").type("text")
                        .value(phone).required(!profileHas)
                        .placeholder(profileHas ? null : "账号未登记,请填写11位手机号")
                        .hint(profileHas ? "已按注册资料预填" : null).build(),
                FormField.builder().key("isDefault").label("设为默认地址").type("switch")
                        .value(String.valueOf(Boolean.parseBoolean(String.valueOf(args.getOrDefault("isDefault", "false")))))
                        .required(false).placeholder(null).hint(null).build());
    }

    private FormField field(String key, String label, String type, Map<String, Object> args,
                            boolean required, String placeholder, String hint) {
        return FormField.builder().key(key).label(label).type(type)
                .value(Args.str(args.get(key))).required(required)
                .placeholder(placeholder).hint(hint).build();
    }

    /** 模型常把区名重复填进详细地址(如 area=芙蓉区 detail=芙蓉区805号)——去掉重复前缀。 */
    private String stripAreaPrefix(String detail, String area, String city) {
        if (detail == null) return null;
        if (area != null && detail.startsWith(area)) detail = detail.substring(area.length()).trim();
        if (city != null && detail.startsWith(city)) detail = detail.substring(city.length()).trim();
        return detail == null || detail.isEmpty() ? null : detail;
    }

    private String nz(String s) {
        return s == null || s.isEmpty() ? "未填写" : s;
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        AddressRequest req = new AddressRequest();
        req.setProvince(Args.str(args.get("province")));
        req.setCity(Args.str(args.get("city")));
        req.setArea(Args.str(args.get("area")));
        req.setAddressDetail(stripAreaPrefix(Args.str(args.get("addressDetail")),
                Args.str(args.get("area")), Args.str(args.get("city"))));
        String consignee = Args.str(args.get("consignee"));
        String phone = Args.str(args.get("phone"));
        if (consignee == null || consignee.isEmpty() || phone == null || phone.isEmpty()) {
            User u = userService.getUserByUserName(ctx.getUserName());
            if ((consignee == null || consignee.isEmpty()) && u != null) consignee = u.getRealName();
            if ((phone == null || phone.isEmpty()) && u != null) phone = u.getPhone();
        }
        if (consignee == null || consignee.isEmpty()) throw new RuntimeException("请提供收件人姓名(账号未登记真实姓名)");
        if (phone == null || phone.length() != 11) throw new RuntimeException("请提供 11 位收件人手机号(账号未登记可用手机号)");
        req.setConsignee(consignee);
        req.setPhone(phone);
        boolean def = Boolean.parseBoolean(String.valueOf(args.getOrDefault("isDefault", "false")));
        req.setIsDefault(def ? 1 : 0);
        addressService.addAddress(ctx.getUserName(), req);
        // 落库后反查新地址编号:记入会话缓存并把编号告诉模型/用户——
        // 用户说"用新地址下单"时 place_order 传'新地址'即可,模型无需再翻 list_addresses(实测会偷懒传'默认地址')
        Integer newId = null;
        for (com.agri.platform.entity.Address a : addressService.getAddressList(ctx.getUserName())) {
            if (a.getProvince() != null && a.getProvince().equals(req.getProvince())
                    && a.getCity() != null && a.getCity().equals(req.getCity())
                    && a.getAddressDetail() != null && a.getAddressDetail().equals(req.getAddressDetail())
                    && (newId == null || (a.getId() != null && a.getId() > newId))) {
                newId = a.getId();
            }
        }
        if (newId != null) {
            latestAddressCache.record(ctx.getSessionId(), newId);
        }
        // 返回文本会直接展示给用户并回灌给模型——只能写用户向内容,严禁"可对买家说…"类内部指令
        return StrUtil.format("收货地址已新增{}{}。您可以对我说\"用新地址下单\"继续购买。",
                def ? "并设为默认" : "", newId != null ? StrUtil.format("(地址簿#{})", newId) : "");
    }
}
