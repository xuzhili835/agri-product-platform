package com.agri.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付宝（沙箱）配置
 * - appId:          沙箱应用 APPID
 * - appPrivateKey:  应用私钥（用于签名请求，仅本机保存，走 application-local.yml 不入库）
 * - alipayPublicKey: 支付宝公钥（用于验签支付宝响应/回调）
 * - gateway:        沙箱网关地址
 * - returnUrl:      支付完成后浏览器跳转地址（同步跳转，本地 localhost 即可）
 * - notifyUrl:      异步回调地址（需公网可达；本地开发可不配，靠 return_url + 主动查询兜底）
 */
@Data
@ConfigurationProperties("alipay")
public class AlipayProperties {
    private String appId = "";
    private String appPrivateKey = "";
    private String alipayPublicKey = "";
    private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String charset = "utf-8";
    private String signType = "RSA2";
    private String format = "json";
    private String returnUrl = "http://localhost:3000/alipay-result";
    private String notifyUrl = "";
}
