package com.agri.platform.agent.tool.rag;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** 按 ~300 字 + 重叠切块。 */
@Component
public class Chunker {
    private static final int SIZE = 300;
    private static final int OVERLAP = 60;

    public List<String> chunk(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        for (int i = 0; i < text.length(); i += (SIZE - OVERLAP)) {
            int end = Math.min(text.length(), i + SIZE);
            out.add(text.substring(i, end));
            if (end == text.length()) break;
        }
        return out;
    }
}
