package com.ethicalsoft.ethicalsoft_complience.adapters.out.notification;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationChannel;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ChannelSender {

    private final NotificationChannelSender channelSender;

    public void send(NotificationTemplate template,
                     Map<String, String> placeholders,
                     Consumer<NotificationDispatchRequest.NotificationDispatchRequestBuilder> requestCustomizer) {

        NotificationDispatchRequest.NotificationDispatchRequestBuilder builder = NotificationDispatchRequest.builder()
                .placeholders(placeholders)
                .templateModel(new java.util.HashMap<>(placeholders));

        requestCustomizer.accept(builder);
        NotificationDispatchRequest request = builder.build();

        Map<NotificationChannel, Runnable> channelActions = buildChannelActions(template, placeholders, request);
        template.channels().forEach(channel -> {
            Runnable action = channelActions.get(channel);
            if (action != null) {
                action.run();
            }
        });
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
