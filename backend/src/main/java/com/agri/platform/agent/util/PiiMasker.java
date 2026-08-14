package com.agri.platform.agent.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/**
 * PII 脱敏:工具结果进 LLM 前抹掉手机号/身份证。
 *
 * <p>注意替换顺序:必须先处理 18 位身份证,再处理 11 位手机号。
 * 否则手机号正则会先匹配到身份证前 11 位,导致身份证脱敏失败。
 * 先脱敏身份证后,星号打断了数字串,手机号正则不会再误匹配。
 */
@Component
public class PiiMasker {
    private static final Pattern ID_CARD = Pattern.compile("(\\d{3})\\d{11}(\\d{4})");
    private static final Pattern PHONE = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");

    public String mask(String text) {
        if (text == null) return null;
        // 先身份证后手机号:避免手机号正则误吃身份证前 11 位
        text = ID_CARD.matcher(text).replaceAll("$1***********$2");
        text = PHONE.matcher(text).replaceAll("$1****$2");
        return text;
    }
}
