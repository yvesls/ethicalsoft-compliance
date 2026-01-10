package com.ethicalsoft.ethicalsoft_complience.domain.notification;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResolvePlaceholderPolicy {

    public String resolve(String template, Map<String, String> values) {
        if (template == null || template.isBlank() || values == null || values.isEmpty()) {
            return template;
        }
        String resolved = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return resolved;
    }
}

