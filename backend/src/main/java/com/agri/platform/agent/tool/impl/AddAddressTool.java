package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
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

import java.util.Map;

/**
 * 写工具:新增收货地址(走确认门)。与平台地址簿的三级区域(省/市/区)+详细地址结构对齐——
 * 此前 place_order 接受自由文本地址,用户随口一句"长沙流通县"直接进订单,
 * 既不落地址簿也不是规范的三级区域。新地址一律先经本工具落地址簿,再用 地址簿#编号 下单。
 *
 * <p>consignee/phone 可省略:用户说"用我自己的信息"时不向用户索要,自动取账号注册的
 * 姓名与手机号(与 reserve_expert 同口径,PII 不经 LLM 流转);资料缺失才要求补充。</p>
 */
@Component
@RequiredArgsConstructor
public class AddAddressTool implements Tool {
    private final AddressService addressService;
    private final UserService userService;
    private final PiiMasker masker;
    private final LatestAddressCache latestAddressCache;

    public String name() { return "add_address"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("新增收货地址到地址簿(需用户确认)。必填:province(省)、city(市)、area(区/县)、"
                        + "addressDetail(街道门牌详细地址)。consignee(收件人)/phone(手机号)几乎总是留空——"
                        + "系统自动使用账号注册的姓名和手机号,不要向用户询问、不要在文字里提及;"
                        + "仅当用户本轮明确给出其他收件人姓名和电话时才传;"
                        + "isDefault(是否设为默认,布尔)。用户想用新地址下单时先调本工具新增,"
                        + "成功后 place_order 的 address 传'新地址'即可引用刚新增的地址(也支持'地址簿#编号')。信息齐全直接调用。")
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
        String province = Args.str(args.get("province"));
        String city = Args.str(args.get("city"));
        String area = Args.str(args.get("area"));
        String detail = Args.str(args.get("addressDetail"));
        if (province == null) throw new RuntimeException("请提供省份(如:湖南省)");
        if (city == null) throw new RuntimeException("请提供城市(如:长沙市)");
        if (area == null) throw new RuntimeException("请提供区/县(如:芙蓉区)");
        if (detail == null) throw new RuntimeException("请提供详细地址(街道/门牌号)");
        // 模型常把区名重复填进详细地址(如 area=芙蓉区 detail=芙蓉区805号)——去掉重复前缀
        if (area != null && detail.startsWith(area)) detail = detail.substring(area.length()).trim();
        if (city != null && detail.startsWith(city)) detail = detail.substring(city.length()).trim();
        if (detail.isEmpty()) throw new RuntimeException("请提供详细地址(街道/门牌号,不要只填区名)");
        // 收件人/手机号:用户给了就用;没给自动取账号注册资料,资料也没有才要求补充
        // null 或空串/占位都视为未提供——模型可能传 "" 占位,一律自动回填注册资料
        String consignee = Args.str(args.get("consignee"));
        String phone = Args.str(args.get("phone"));
        if (consignee == null || consignee.isEmpty() || phone == null || phone.isEmpty()) {
            User u = userService.getUserByUserName(ctx.getUserName());
            if ((consignee == null || consignee.isEmpty()) && u != null
                    && u.getRealName() != null && !u.getRealName().trim().isEmpty()) {
                consignee = u.getRealName().trim();
            }
            if ((phone == null || phone.isEmpty()) && u != null
                    && u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
                phone = u.getPhone().trim();
            }
        }
        if (consignee == null || consignee.isEmpty()) throw new RuntimeException("请提供收件人姓名(账号未登记真实姓名)");
        if (phone == null || phone.length() != 11) throw new RuntimeException("请提供 11 位收件人手机号(账号未登记可用手机号)");
        boolean def = Boolean.parseBoolean(String.valueOf(args.getOrDefault("isDefault", "false")));
        return StrUtil.format("即将新增收货地址:\n{} {} {} {}\n收件人:{} 电话:{}{}\n确认执行?",
                province, city, area, detail, consignee, masker.mask(phone), def ? "(设为默认地址)" : "");
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        AddressRequest req = new AddressRequest();
        req.setProvince(Args.str(args.get("province")));
        req.setCity(Args.str(args.get("city")));
        req.setArea(Args.str(args.get("area")));
        String detail = Args.str(args.get("addressDetail"));
        String areaV = Args.str(args.get("area"));
        String cityV = Args.str(args.get("city"));
        if (detail != null && areaV != null && detail.startsWith(areaV)) detail = detail.substring(areaV.length()).trim();
        if (detail != null && cityV != null && detail.startsWith(cityV)) detail = detail.substring(cityV.length()).trim();
        req.setAddressDetail(detail);
        String consignee = Args.str(args.get("consignee"));
        String phone = Args.str(args.get("phone"));
        if (consignee == null || consignee.isEmpty() || phone == null || phone.isEmpty()) {
            User u = userService.getUserByUserName(ctx.getUserName());
            if ((consignee == null || consignee.isEmpty()) && u != null) consignee = u.getRealName();
            if ((phone == null || phone.isEmpty()) && u != null) phone = u.getPhone();
        }
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
