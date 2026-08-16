package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.dto.AddressRequest;
import com.agri.platform.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 写工具:新增收货地址(走确认门)。与平台地址簿的三级区域(省/市/区)+详细地址结构对齐——
 * 此前 place_order 接受自由文本地址,用户随口一句"长沙流通县"直接进订单,
 * 既不落地址簿也不是规范的三级区域。新地址一律先经本工具落地址簿,再用 地址簿#编号 下单。
 */
@Component
@RequiredArgsConstructor
public class AddAddressTool implements Tool {
    private final AddressService addressService;

    public String name() { return "add_address"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("新增收货地址到地址簿(需用户确认)。必填:province(省)、city(市)、area(区/县)、"
                        + "addressDetail(街道门牌详细地址)、consignee(收件人)、phone(手机号)。"
                        + "可选:isDefault(是否设为默认,布尔)。用户想用新地址下单时先调本工具新增,"
                        + "成功后用新地址的'地址簿#编号'调 place_order。信息齐全直接调用。")
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
        String consignee = Args.str(args.get("consignee"));
        String phone = Args.str(args.get("phone"));
        if (province == null) throw new RuntimeException("请提供省份(如:湖南省)");
        if (city == null) throw new RuntimeException("请提供城市(如:长沙市)");
        if (area == null) throw new RuntimeException("请提供区/县(如:流通县)");
        if (detail == null) throw new RuntimeException("请提供详细地址(街道/门牌号)");
        if (consignee == null) throw new RuntimeException("请提供收件人姓名");
        if (phone == null || phone.length() != 11) throw new RuntimeException("请提供 11 位收件人手机号");
        boolean def = Boolean.parseBoolean(String.valueOf(args.getOrDefault("isDefault", "false")));
        return StrUtil.format("即将新增收货地址:\n{} {} {} {}\n收件人:{} 电话:{}{}\n确认执行?",
                province, city, area, detail, consignee, phone, def ? "(设为默认地址)" : "");
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        AddressRequest req = new AddressRequest();
        req.setProvince(Args.str(args.get("province")));
        req.setCity(Args.str(args.get("city")));
        req.setArea(Args.str(args.get("area")));
        req.setAddressDetail(Args.str(args.get("addressDetail")));
        req.setConsignee(Args.str(args.get("consignee")));
        req.setPhone(Args.str(args.get("phone")));
        boolean def = Boolean.parseBoolean(String.valueOf(args.getOrDefault("isDefault", "false")));
        req.setIsDefault(def ? 1 : 0);
        addressService.addAddress(ctx.getUserName(), req);
        return StrUtil.format("收货地址已新增{}。可对买家说'用新地址下单'或让买家从地址列表选择。",
                def ? "并设为默认" : "");
    }
}
