package com.agri.platform.agent.dto;
import lombok.Data;

@Data
public class ConfirmRequest {
    private String pendingId;
    private boolean accept;
    private String sessionId;
}
