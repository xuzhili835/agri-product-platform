package com.agri.platform.agent.provider;

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
        JSONObject body = new JSONObject();
        body.set("model", props.getEmbedModel());
        body.set("input", texts);
        body.set("encoding_format", "float");
        String resp = HttpRequest.post(props.getBaseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(60000)
                .execute()
                .body();
        JSONArray data = JSONUtil.parseObj(resp).getJSONArray("data");
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
