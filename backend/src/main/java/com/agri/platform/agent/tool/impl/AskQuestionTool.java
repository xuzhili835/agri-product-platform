package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.FormField;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.agent.util.PiiMasker;
import com.agri.platform.dto.QuestionRequest;
import com.agri.platform.entity.Expert;
import com.agri.platform.entity.User;
import com.agri.platform.service.ExpertService;
import com.agri.platform.service.QuestionService;
import com.agri.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 写工具:向专家提问(表单卡)。字段与必填规则逐项对齐手动页 QuestionAsk.vue:
 * 必填=专家/农作物/电话(正 ^1[3-9]\d{9}$,按注册手机号预填)/标题(5-100字)/问题描述(5-2000字)。
 * 服务层 askQuestion 只硬校验专家与标题,tb_question 的 phone/plant_name/title 为 NOT NULL——
 * 手动页口径的全量校验由 validate 在确认时执行,漏槽不再靠服务端兜底。
 *
 * <p>draft 里的联系电话走 {@link PiiMasker} 脱敏;执行时仍用真实号码。
 * 提交后后端会给被提问的专家发站内通知。</p>
 */
@Component
@RequiredArgsConstructor
public class AskQuestionTool implements Tool {
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final QuestionService questionService;
    private final ExpertService expertService;
    private final UserService userService;
    private final PiiMasker masker;

    public String name() { return "ask_question"; }

    /** 与手动页一致:仅农户/买家可向专家提问(farmer/buyer 都是本工具的可见角色)。 */
    public String role() { return "common"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("线上向专家提问(表单卡,异步问答,专家24小时内回复)。注意与 reserve_expert 的分工:"
                        + "用户想'问个问题/请教/咨询病害'等文字问答场景用本工具;'约时间/见面/上门/一对一'才用 reserve_expert。"
                        + "调用后系统会弹出可编辑表单,已提取的信息预填,缺失的槽位由用户在表单里补——"
                        + "因此信息不全也直接调用,严禁向用户追问任何字段(作物/标题/详情等都不要问)。"
                        + "expertName 必须用 list_experts 返回的真实专家账号,用户只说姓名时你自己换算;"
                        + "plantName 农作物;title 问题标题(5-100字);question 问题描述(5-2000字);"
                        + "电话表单会按账号资料预填,无需向用户索要。")
                .parameters(Map.ofEntries(
                        Map.entry("expertName", "string"),
                        Map.entry("plantName", "string"),
                        Map.entry("phone", "string"),
                        Map.entry("title", "string"),
                        Map.entry("question", "string")))
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
        String phone = Args.str(args.get("phone"));
        if (phone == null || phone.isEmpty()) phone = resolvePhone(ctx);
        return StrUtil.format("即将向专家提问:{}\n农作物:{} 电话:{}\n标题:{}\n问题:{}\n确认执行?",
                expertDisplay, nz(args.get("plantName")), masker.mask(phone),
                nz(args.get("title")), nz(args.get("question")));
    }

    /** 确认时校验(合并表单值后):必填规则与手动页 QuestionAsk.vue 完全一致。 */
    @Override
    public void validate(Map<String, Object> args) {
        require(Args.str(args.get("expertName")), "请选择咨询专家");
        require(Args.str(args.get("plantName")), "请填写农作物名称");
        String phone = Args.str(args.get("phone"));
        // 电话:表单值有就验正则(手动页同款);空值允许——execute 会兜底账号注册手机号
        if (phone != null && !phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            throw new RuntimeException("请填写正确的手机号(11位,1开头)");
        }
        String title = Args.str(args.get("title"));
        if (title == null || title.isEmpty()) throw new RuntimeException("请填写问题标题");
        if (title.length() < 5) throw new RuntimeException("问题标题不能少于5个字");
        if (title.length() > 100) throw new RuntimeException("问题标题不能超过100个字");
        String question = Args.str(args.get("question"));
        if (question == null || question.isEmpty()) throw new RuntimeException("请填写问题描述");
        if (question.length() < 5) throw new RuntimeException("问题描述不能少于5个字");
        if (question.length() > 2000) throw new RuntimeException("问题描述不能超过2000个字");
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
        // 电话按注册手机号预填(手动页同款行为),空表单值时 execute 也兜底注册资料
        User u = userService.getUserByUserName(ctx.getUserName());
        String phone = Args.str(args.get("phone"));
        if ((phone == null || phone.isEmpty()) && u != null) phone = u.getPhone();
        return List.of(
                field("expertName", "咨询专家", "select", args, true, experts, null, null),
                field("plantName", "农作物", "text", args, true, null, "如:水稻、玉米、番茄", null),
                FormField.builder().key("phone").label("联系电话").type("text")
                        .value(phone).required(true)
                        .placeholder("方便专家回访").hint("已按注册资料预填,可修改").build(),
                field("title", "问题标题", "text", args, true, null,
                        "如:小麦叶片发黄是什么原因?", "5-100个字"),
                field("question", "问题描述", "textarea", args, true, null,
                        "具体情况/症状/发生时间/已尝试的办法", "5-2000个字"));
    }

    private FormField field(String key, String label, String type, Map<String, Object> args,
                            boolean required, List<FormField.Option> options, String placeholder, String hint) {
        return FormField.builder().key(key).label(label).type(type)
                .value(Args.str(args.get(key))).required(required).options(options)
                .placeholder(placeholder).hint(hint).build();
    }

    private String nz(Object v) {
        String s = Args.str(v);
        return (s == null || s.isEmpty()) ? "未填写" : s;
    }

    private void require(String v, String msg) {
        if (v == null || v.trim().isEmpty()) throw new RuntimeException(msg);
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        // 电话:表单值(预填注册手机号,可改)优先,空则兜底注册资料
        String phone = Args.str(args.get("phone"));
        if (phone == null || phone.isEmpty()) phone = resolvePhone(ctx);
        QuestionRequest req = new QuestionRequest();
        req.setExpertName(Args.str(args.get("expertName")));
        req.setPlantName(nz(args.get("plantName")));   // tb_question.plant_name NOT NULL
        req.setPhone(phone);
        req.setTitle(Args.str(args.get("title")));
        req.setQuestion(Args.str(args.get("question")));
        questionService.askQuestion(ctx.getUserName(), req);
        return StrUtil.format("问题已提交给{},可在'我的问题'中查看回答", displayExpert(req.getExpertName()));
    }

    private String displayExpert(String expertUserName) {
        Expert e = expertService.getExpertByUserName(expertUserName);
        return e != null ? e.getRealName() : expertUserName;
    }

    private String resolvePhone(ToolContext ctx) {
        User u = userService.getUserByUserName(ctx.getUserName());
        String phone = u != null ? u.getPhone() : null;
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("您的账号未登记联系电话,请在表单中填写");
        }
        return phone;
    }
}
