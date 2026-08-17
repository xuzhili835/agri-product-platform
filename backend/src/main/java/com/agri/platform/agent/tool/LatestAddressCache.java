package com.agri.platform.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话级"最近新增地址"缓存:add_address 落库成功后记下新地址编号,
 * place_order 收到 address="新地址/最新地址"时直接引用——
 * 用户说"用新地址下单"时模型无需先调 list_addresses 翻编号
 * (实测模型会偷懒直接传'默认地址',导致新地址下单元素被无视)。
 */
@Component
public class LatestAddressCache {

    private static final long TTL_MS = 30 * 60_000L;

    private record Entry(Integer addressId, long at) {}

    private final Map<String, Entry> map = new ConcurrentHashMap<>();

    public void record(String sessionId, Integer addressId) {
        if (sessionId == null || addressId == null) return;
        map.put(sessionId, new Entry(addressId, System.currentTimeMillis()));
    }

    /** 取本会话最近新增的地址编号;没有或已过期返回 null。 */
    public Integer get(String sessionId) {
        if (sessionId == null) return null;
        Entry e = map.get(sessionId);
        if (e == null) return null;
        if (System.currentTimeMillis() - e.at() > TTL_MS) {
            map.remove(sessionId);
            return null;
        }
        return e.addressId();
    }
}
