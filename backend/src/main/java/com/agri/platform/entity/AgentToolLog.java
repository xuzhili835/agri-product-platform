package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_agent_tool_log")
public class AgentToolLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String sessionId;
    private String userName;
    private String toolName;
    private String arguments;
    private String result;
    private String status;      // ok/error/pending/cancelled/timeout
    private Integer durationMs;
    private Integer confirmed;
    private LocalDateTime createTime;
}
