package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
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

import java.util.Map;

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

    public String role() { return "farmer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("预约专家咨询。expertName 必须用 list_experts 返回的真实专家账号;"
                        + "preferredTime 期望时间(必填);plantName 农作物;soilCondition 土壤条件;"
                        + "plantCondition 作物当前状况;plantDetail 问题描述;message 留言(可选)。"
                        + "缺少信息时先向农户追问补齐再调用。电话和地址系统自动取用户资料。需用户确认。")
                .parameters(Map.of(
                        "expertName", "string",
                        "preferredTime", "string",
                        "plantName", "string",
                        "soilCondition", "string",
                        "plantCondition", "string",
                        "plantDetail", "string",
                        "message", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        String expertName = Args.str(args.get("expertName"));
        require(expertName, "请先选择要预约的专家(可用 list_experts 查看真实专家)");
        Expert expert = expertService.getExpertByUserName(expertName);
        if (expert == null) {
            throw new RuntimeException("专家账号 " + expertName + " 不存在,请用 list_experts 查询真实专家账号");
        }
        require(Args.str(args.get("preferredTime")), "请填写期望的咨询时间(如:本周六上午)");
        require(Args.str(args.get("plantName")), "请填写咨询的农作物(如水稻)");
        require(Args.str(args.get("soilCondition")), "请填写土壤条件");
        require(Args.str(args.get("plantCondition")), "请填写作物当前状况");
        require(Args.str(args.get("plantDetail")), "请填写具体问题/作物详情");
        String phone = resolvePhone(ctx);
        String address = resolveAddress(ctx);
        return StrUtil.format("即将预约专家 {}({}) 时间:{}\n农作物:{} 土壤:{} 状况:{}\n问题:{}\n联系电话:{} 地址:{}\n确认执行?",
                expertName, expert.getRealName(), args.get("preferredTime"), args.get("plantName"),
                args.get("soilCondition"), args.get("plantCondition"), args.get("plantDetail"),
                masker.mask(phone), address);
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        ReserveRequest req = new ReserveRequest();
        req.setExpertName(Args.str(args.get("expertName")));
        req.setPreferredTime(Args.str(args.get("preferredTime")));
        req.setMessage(Args.str(args.get("message")));
        req.setPlantName(Args.str(args.get("plantName")));
        req.setSoilCondition(Args.str(args.get("soilCondition")));
        req.setPlantCondition(Args.str(args.get("plantCondition")));
        req.setPlantDetail(Args.str(args.get("plantDetail")));
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
