package com.agri.platform.agent.tool;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 会话级"已搜索商品编号"缓存:search_market 把返回的真实编号记入,
 * place_order 校验编号必须来自本会话搜索结果。
 *
 * <p>动机(实测):模型可能跳过搜索直接编 orderId——撞上不存在的编号会报错回灌(可自愈),
 * 但也可能撞上<strong>库里恰好存在的其他商品</strong>,把用户不想买的东西出成确认卡;
 * 还会把历史里的"订单号:14"误当"商品#14"传入。编号白名单从代码层杜绝这两类混淆。</p>
 */
@Component
public class SearchedProductCache {

    private static final long TTL_MS = 10 * 60_000L;

    /** sessionId -> (orderId -> 记录时间)。 */
    private final Map<String, Map<Integer, Long>> map = new ConcurrentHashMap<>();

    public void record(String sessionId, Integer orderId) {
        if (sessionId == null || orderId == null) return;
        map.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(orderId, System.currentTimeMillis());
    }

    public boolean contains(String sessionId, Integer orderId) {
        if (sessionId == null || orderId == null) return false;
        Map<Integer, Long> m = map.get(sessionId);
        if (m == null) return false;
        Long t = m.get(orderId);
        if (t == null) return false;
        if (System.currentTimeMillis() - t > TTL_MS) {
            m.remove(orderId);
            return false;
        }
        return true;
    }

    /** 每 5 分钟清一次过期会话,防内存缓慢增长。 */
    @Scheduled(fixedDelay = 300_000)
    public void sweep() {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(e -> e.getValue().isEmpty());
        map.values().forEach(m -> m.entrySet().removeIf(en -> now - en.getValue() > TTL_MS));
    }
}
