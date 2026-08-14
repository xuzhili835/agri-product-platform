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

    public void setAgentEnabled(boolean enabled) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey("agent_enabled");
        c.setConfigValue(String.valueOf(enabled));
        mapper.updateById(c);   // 主键存在则更新(configKey 是 @TableId(INPUT),Task 2 种子已插入该行)
    }
}
