package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.notification;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationPartyDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.InternalNotificationPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.ListInternalNotificationsPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.NotificationTemplatePort;
import com.ethicalsoft.ethicalsoft_complience.application.port.notification.UpdateInternalNotificationStatusPort;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoInternalNotificationAdapter implements InternalNotificationPort, ListInternalNotificationsPort, UpdateInternalNotificationStatusPort, NotificationTemplatePort {

    private final NotificationRepository notificationMongoRepository;
    private final NotificationTemplateRepository templateMongoRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationDocument doc = toDocument(notification);
        log.info("[notification-mongo] Salvando notificação interna templateKey={} recipientUserId={} recipientEmail={} status={}",
                doc.getTemplateKey(),
                doc.getRecipient() != null ? doc.getRecipient().getUserId() : null,
                doc.getRecipient() != null ? doc.getRecipient().getEmail() : null,
                doc.getStatus());

        NotificationDocument saved = notificationMongoRepository.save(doc);

        log.info("[notification-mongo] Notificação interna salva id={} templateKey={} recipientUserId={}",
                saved.getId(),
                saved.getTemplateKey(),
                saved.getRecipient() != null ? saved.getRecipient().getUserId() : null);

        return toDomain(saved);
    }

    @Override
    public List<Notification> listUnseenForRecipient(Long recipientUserId) {
        return notificationMongoRepository
                .findByRecipient_UserIdAndStatusOrderByCreatedAtDesc(recipientUserId, NotificationStatus.UNREAD)
                .stream()
                .map(this::toDomain)
                .toList();
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
                        t.getTemplateLink(),
                        t.getChannels() == null ? java.util.List.of() : t.getChannels().stream()
                                .map(s -> s == null ? null : s.toUpperCase(Locale.ROOT))
                                .filter(java.util.Objects::nonNull)
                                .map(NotificationChannel::valueOf)
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
