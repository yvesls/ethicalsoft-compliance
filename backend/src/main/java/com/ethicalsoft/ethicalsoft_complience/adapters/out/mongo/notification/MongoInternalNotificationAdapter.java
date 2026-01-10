package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationPartyDocument;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.InternalNotificationPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.ListInternalNotificationsPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.UpdateInternalNotificationStatusPort;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.Notification;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationChannel;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationParty;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoInternalNotificationAdapter implements InternalNotificationPort, ListInternalNotificationsPort, UpdateInternalNotificationStatusPort, NotificationTemplatePort {

    private final NotificationRepository notificationMongoRepository;
    private final NotificationTemplateRepository templateMongoRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationDocument saved = notificationMongoRepository.save(toDocument(notification));
        return toDomain(saved);
    }

    @Override
    public Page<Notification> listForRecipient(Long recipientUserId, Pageable pageable) {
        return notificationMongoRepository
                .findByRecipientUserIdAndStatusNot(recipientUserId, NotificationStatus.DELETED, pageable)
                .map(this::toDomain);
    }

    @Override
    public Optional<Notification> findById(String id) {
        return notificationMongoRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<NotificationTemplate> findByKey(String key) {
        return templateMongoRepository.findByKey(key)
                .map(t -> new NotificationTemplate(
                        t.getKey(),
                        t.getWhoCanSend(),
                        t.getRecipients(),
                        t.getTitle(),
                        t.getBody(),
                        t.getChannels() == null ? java.util.List.of() : t.getChannels().stream()
                                .map(s -> s == null ? null : s.toUpperCase(Locale.ROOT))
                                .filter(java.util.Objects::nonNull)
                                .map(s -> NotificationChannel.valueOf(s))
                                .toList()
                ));
    }

    private NotificationDocument toDocument(Notification n) {
        return NotificationDocument.builder()
                .id(n.id())
                .sender(toPartyDocument(n.sender()))
                .recipient(toPartyDocument(n.recipient()))
                .title(n.title())
                .content(n.content())
                .status(n.status())
                .createdAt(n.createdAt())
                .updatedAt(n.updatedAt())
                .templateKey(n.templateKey())
                .build();
    }

    private NotificationPartyDocument toPartyDocument(NotificationParty party) {
        if (party == null) {
            return null;
        }
        return NotificationPartyDocument.builder()
                .userId(party.userId())
                .fullName(party.fullName())
                .email(party.email())
                .roles(party.roles())
                .build();
    }

    private Notification toDomain(NotificationDocument d) {
        return new Notification(
                d.getId(),
                toPartyDomain(d.getSender()),
                toPartyDomain(d.getRecipient()),
                d.getTitle(),
                d.getContent(),
                d.getStatus(),
                d.getCreatedAt(),
                d.getUpdatedAt(),
                d.getTemplateKey()
        );
    }

    private NotificationParty toPartyDomain(NotificationPartyDocument d) {
        if (d == null) {
            return null;
        }
        return new NotificationParty(d.getUserId(), d.getFullName(), d.getEmail(), d.getRoles());
    }
}
