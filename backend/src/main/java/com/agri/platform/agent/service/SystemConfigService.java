package com.agri.platform.agent.service;

import com.agri.platform.entity.SystemConfig;
import com.agri.platform.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemConfigService {
    private final SystemConfigMapper mapper;

    public boolean agentEnabled() {
        SystemConfig c = mapper.selectOne(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, "agent_enabled"));
        return c != null && "true".equalsIgnoreCase(c.getConfigValue());
    }

    /**
     * upsert 语义:行存在则更新,不存在则插入。此前只 updateById——若种子行缺失
     * (老库+迁移种子插入失败被吞),toggle 永远返回成功却影响 0 行,助手"永久停用"且无报错。
     */
    public void setAgentEnabled(boolean enabled) {
        SystemConfig existing = mapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, "agent_enabled"));
        if (existing == null) {
            SystemConfig c = new SystemConfig();
            c.setConfigKey("agent_enabled");
            c.setConfigValue(String.valueOf(enabled));
            mapper.insert(c);
        } else {
            existing.setConfigValue(String.valueOf(enabled));
            mapper.updateById(existing);
        }
    }
}
