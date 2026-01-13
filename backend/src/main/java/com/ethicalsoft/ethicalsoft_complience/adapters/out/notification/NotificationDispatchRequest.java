package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class NotificationDispatchRequest {
    private Long senderUserId;
    private String senderName;
    private String senderEmail;
    private List<String> senderRoles;

    private Long recipientUserId;
    private String recipientName;
    private String recipientEmail;
    private List<String> recipientRoles;

    private Map<String, String> placeholders;
    private Map<String, Object> templateModel;
}

