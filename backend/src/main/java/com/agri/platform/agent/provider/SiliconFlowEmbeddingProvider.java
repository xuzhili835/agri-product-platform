package com.agri.platform.agent.provider;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agri.platform.config.SiliconFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiliconFlowEmbeddingProvider implements EmbeddingProvider {

    private final SiliconFlowProperties props;

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) return new ArrayList<>();
        JSONObject body = new JSONObject();
        body.set("model", props.getEmbedModel());
        body.set("input", texts);
        body.set("encoding_format", "float");
        String resp = HttpRequest.post(props.getBaseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(20_000)   // 检索路径同步调用,不能占满 orchestrator 的 55s 总预算
                .execute()
                .body();
        // 错误响应(无 data,如 {"code":..., "message":...})此前直接 NPE,调用侧只能看到 "[工具异常] null"
        JSONArray data = JSONUtil.parseObj(resp).getJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new RuntimeException("embedding 接口返回错误: " + StrUtil.maxLength(resp, 200));
        }
        List<float[]> out = new ArrayList<>();
        for (Object o : data) {
            JSONArray arr = ((JSONObject) o).getJSONArray("embedding");
            float[] v = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) v[i] = arr.getFloat(i).floatValue();
            out.add(v);
        }
        return out;
    }

    /** float[] -> byte[](LITTLE_ENDIAN)存 LONGBLOB。 */
    public static byte[] toBytes(float[] v) {
        ByteBuffer bb = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) bb.putFloat(f);
        return bb.array();
    }

    /** byte[] -> float[]。 */
    public static float[] toFloats(byte[] b) {
        ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        float[] v = new float[b.length / 4];
        for (int i = 0; i < v.length; i++) v[i] = bb.getFloat();
        return v;
    }
}
