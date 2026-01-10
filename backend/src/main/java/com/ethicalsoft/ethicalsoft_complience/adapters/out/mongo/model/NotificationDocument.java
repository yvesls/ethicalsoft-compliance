package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model;

import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
@CompoundIndex(name = "recipient_status_idx", def = "{ 'recipient.userId': 1, 'status': 1 }")
public class NotificationDocument {
    @Id
    private String id;

    private NotificationPartyDocument sender;

    private NotificationPartyDocument recipient;

    private String title;
    private String content;

    private NotificationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String templateKey;
}

