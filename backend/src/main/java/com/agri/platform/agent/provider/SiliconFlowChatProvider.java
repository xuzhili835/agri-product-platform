package com.agri.platform.agent.provider;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agri.platform.agent.dto.*;
import com.agri.platform.config.SiliconFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiliconFlowChatProvider implements ChatProvider {

    private final SiliconFlowProperties props;

    @Override
    public ChatResponse chat(List<ChatMessage> messages, List<ToolSpec> tools, String model) {
        String resp;
        try {
            resp = doRequest(messages, tools, model);
        } catch (Exception e) {
            log.warn("[硅基chat] 模型 {} 请求异常: {}", model, e.getMessage());
            return fallbackOrGraceful(messages, tools, model);
        }
        // 先检视原始响应,识别 API 错误(无 choices,如 {"code":30003,"message":"Model disabled."})
        JSONObject json;
        try {
            json = JSONUtil.parseObj(resp);
        } catch (Exception e) {
            log.warn("[硅基chat] 模型 {} 响应非 JSON: {}", model, StrUtil.maxLength(resp, 200));
            return fallbackOrGraceful(messages, tools, model);
        }
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            String errMsg = json.getStr("message");
            log.warn("[硅基chat] 模型 {} 返回错误: {}", model, errMsg != null ? errMsg : StrUtil.maxLength(resp, 200));
            return fallbackOrGraceful(messages, tools, model);
        }
        // 成功路径:choices 存在,走解析逻辑。解析异常(如有 choices 无 message 的怪响应)
        // 同样走 fallback——此前直接 NPE 到 500,是"静默空 reply"修复的同族漏网
        try {
            log.debug("[硅基chat] resp={}", StrUtil.maxLength(resp, 500));
            return parseResponse(resp);
        } catch (Exception e) {
            log.warn("[硅基chat] 模型 {} 响应解析异常: {}", model, e.getMessage());
            return fallbackOrGraceful(messages, tools, model);
        }
    }

    /**
     * 执行 HTTP 调用(私有,便于 chat() 包裹 try/catch 并检视原始响应)。
     * timeout 25s:orchestrator 总预算 55s(deadline),单次请求最长可能连跑两次
     * (主模型 25s 超时 + fallback 25s),50s 仍留在预算内;若单次 40s×2=80s 则必击穿预算,
     * 前端 60s 先超时、后端仍在跑并落库 → 用户重试产生重复消息/重复 pending。
     */
    private String doRequest(List<ChatMessage> messages, List<ToolSpec> tools, String model) {
        String body = buildRequestBody(messages, tools, model);
        return HttpRequest.post(props.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(25_000)
                .execute()
                .body();
    }

    /**
     * 兜底逻辑:有 fallback 模型且与当前 model 不同 → 重试一次;否则返回优雅降级消息。
     * 递归安全:第二次调用时 model == fb,!fb.equals(model) 为 false,直接走 graceful 分支,不会无限递归。
     */
    private ChatResponse fallbackOrGraceful(List<ChatMessage> messages, List<ToolSpec> tools, String model) {
        String fb = props.getChatModelFallback();
        if (StrUtil.isNotBlank(fb) && !fb.equals(model)) {
            log.warn("[硅基chat] 切换 fallback 模型: {}", fb);
            return chat(messages, tools, fb);
        }
        log.warn("[硅基chat] 无可用 fallback,返回降级提示 (model={})", model);
        ChatResponse out = new ChatResponse();
        out.setText("智能助手暂时不可用,请稍后再试");
        return out;
    }

    /** 构建请求体(包级可见便于单测)。 */
    String buildRequestBody(List<ChatMessage> messages, List<ToolSpec> tools, String model) {
        JSONObject body = new JSONObject();
        body.set("model", model);
        body.set("temperature", 0.3);
        JSONArray msgs = new JSONArray();
        for (ChatMessage m : messages) {
            JSONObject o = new JSONObject();
            o.set("role", m.getRole());
            if (m.getContent() != null) o.set("content", m.getContent());
            if (m.getToolCallId() != null) o.set("tool_call_id", m.getToolCallId());
            if (m.getToolCallsJson() != null) o.set("tool_calls", JSONUtil.parseArray(m.getToolCallsJson()));
            msgs.add(o);
        }
        body.set("messages", msgs);
        if (tools != null && !tools.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (ToolSpec t : tools) {
                JSONObject func = new JSONObject();
                func.set("name", t.getName());
                func.set("description", t.getDescription());
                JSONObject schema = new JSONObject();
                schema.set("type", "object");
                JSONObject propsObj = new JSONObject();
                if (t.getParameters() != null) {
                    t.getParameters().forEach((k, v) -> {
                        JSONObject p = new JSONObject();
                        p.set("type", v);
                        propsObj.set(k, p);
                    });
                }
                schema.set("properties", propsObj);
                func.set("parameters", schema);
                JSONObject tool = new JSONObject();
                tool.set("type", "function");
                tool.set("function", func);
                arr.add(tool);
            }
            body.set("tools", arr);
        }
        return body.toString();
    }

    /** 解析响应(包级可见便于单测)。message 缺失/为空的怪响应返回空文本(由上层走 fallback)。 */
    ChatResponse parseResponse(String resp) {
        JSONObject json = JSONUtil.parseObj(resp);
        JSONArray choices = json.getJSONArray("choices");
        ChatResponse out = new ChatResponse();
        if (choices == null || choices.isEmpty()) {
            out.setText("");
            return out;
        }
        JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
        if (msg == null) {
            out.setText("");
            return out;
        }
        out.setText(msg.getStr("content"));
        JSONArray tcs = msg.getJSONArray("tool_calls");
        if (tcs != null && !tcs.isEmpty()) {
            List<ToolCall> list = new ArrayList<>();
            for (Object o : tcs) {
                JSONObject tc = (JSONObject) o;
                JSONObject fn = tc.getJSONObject("function");
                ToolCall call = new ToolCall();
                call.setId(tc.getStr("id"));
                call.setName(fn.getStr("name"));
                call.setArguments(fn.getStr("arguments"));
                list.add(call);
            }
            out.setToolCalls(list);
        }
        return out;
    }
}
