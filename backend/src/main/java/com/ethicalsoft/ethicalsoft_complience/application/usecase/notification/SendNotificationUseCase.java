package com.ethicalsoft.ethicalsoft_complience.application.usecase.notification;

import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTypeStrategy;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.notification.command.SendNotificationCommand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SendNotificationUseCase {

    private final List<NotificationTypeStrategy> strategies;

    @Transactional
    public void execute(SendNotificationCommand command) {
        NotificationType type = command.type();
        Map<NotificationType, NotificationTypeStrategy> strategyMap = new EnumMap<>(NotificationType.class);
        for (NotificationTypeStrategy strategy : strategies) {
            strategyMap.put(strategy.type(), strategy);
        }

        var strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new BusinessException("No strategy found for notification type: " + type);
        }
        strategy.send(command);
    }
}
