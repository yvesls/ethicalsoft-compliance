package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationChannel;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ChannelSender {

    private static final List<String> LINK_PLACEHOLDERS = List.of("projectLink", "resetLink");

    private final NotificationChannelSender channelSender;

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    public void send(NotificationTemplate template,
                     Map<String, String> placeholders,
                     Consumer<NotificationDispatchRequest.NotificationDispatchRequestBuilder> requestCustomizer) {

        Map<String, String> enriched = enrichLinks(placeholders);

        NotificationDispatchRequest.NotificationDispatchRequestBuilder builder = NotificationDispatchRequest.builder()
                .placeholders(enriched)
                .templateModel(new HashMap<>(enriched));

        requestCustomizer.accept(builder);
        NotificationDispatchRequest request = builder.build();

        Map<NotificationChannel, Runnable> channelActions = buildChannelActions(template, enriched, request);
        template.channels().forEach(channel -> {
            Runnable action = channelActions.get(channel);
            if (action != null) {
                action.run();
            }
        });
    }

    private Map<String, String> enrichLinks(Map<String, String> placeholders) {
        Map<String, String> enriched = new HashMap<>(placeholders != null ? placeholders : Map.of());
        String base = normalizedBaseUrl();
        enriched.putIfAbsent("frontendBaseUrl", base);
        LINK_PLACEHOLDERS.forEach(key -> enriched.computeIfPresent(key, (k, value) -> absolutize(value, base)));
        return enriched;
    }

    private String normalizedBaseUrl() {
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            return "";
        }
        String trimmed = frontendBaseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String absolutize(String value, String base) {
        if (value == null || value.isBlank() || base.isBlank()) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return base + (value.startsWith("/") ? value : "/" + value);
    }

    private Map<NotificationChannel, Runnable> buildChannelActions(NotificationTemplate template,
                                                                   Map<String, String> placeholders,
                                                                   NotificationDispatchRequest request) {
        Map<NotificationChannel, Runnable> actions = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : template.channels()) {
            if (!channelSender.supports(channel)) {
                continue;
            }
            switch (channel) {
                case INTERNAL -> actions.put(NotificationChannel.INTERNAL, () ->
                        channelSender.sendInternal(
                                template.key(),
                                template.title(),
                                template.body(),
                                placeholders,
                                request));
                case EMAIL -> actions.put(NotificationChannel.EMAIL, () ->
                        channelSender.sendEmail(
                                template.title(),
                                template.templateLink(),
                                placeholders,
                                request));
                default -> {
                }
            }
        }
        return actions;
    }
}
