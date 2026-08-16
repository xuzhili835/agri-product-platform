package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.PiiMasker;
import com.agri.platform.entity.Address;
import com.agri.platform.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 只读工具:列出买家地址簿(编号/省市区/详细/收件人/默认标记)。
 * 下单地址只能来自地址簿——模型据此引导用户选择"地址簿#N"或"默认地址",
 * 杜绝把"长沙流通县"这类随口一句话直接塞进订单(用户实测踩过)。
 */
@Component
@RequiredArgsConstructor
public class ListAddressesTool implements Tool {
    private final AddressService addressService;
    private final PiiMasker masker;

    public String name() { return "list_addresses"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return false; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("列出当前买家的收货地址簿,返回 地址#编号+省市区+详细地址+收件人+是否默认。"
                        + "下单前必须先调用本工具:用户说'默认地址'才传默认;用户提到任何具体地址"
                        + "(城市/区县/街道/新地址名)时,必须在列表中找到对应条目并把'地址簿#编号'传给 place_order,"
                        + "禁止不查列表就默认传'默认地址';列表没有用户要的地址时引导走 add_address 新增。")
                .parameters(Map.of())
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        List<Address> list = addressService.getAddressList(ctx.getUserName());
        if (list == null || list.isEmpty()) {
            return "地址簿为空。请让用户提供 省/市/区/详细地址,先调 add_address 新增后再下单(收件人/手机号留空,系统自动用注册资料)。";
        }
        return list.stream()
                .map(a -> StrUtil.format("地址#{}{} {} {} {} {} 收件人:{} 电话:{}",
                        a.getId(),
                        a.getIsDefault() != null && a.getIsDefault() == 1 ? "[默认]" : "",
                        a.getProvince(), a.getCity(), a.getArea(), a.getAddressDetail(),
                        a.getConsignee(), masker.mask(a.getPhone())))
                .collect(Collectors.joining("\n"));
    }

    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
