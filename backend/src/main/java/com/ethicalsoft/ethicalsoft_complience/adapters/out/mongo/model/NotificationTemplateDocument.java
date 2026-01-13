package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_templates")
public class NotificationTemplateDocument {
    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    private List<String> whoCanSend;
    private List<String> recipients;

    private String title;
    private String body;
    private String templateLink;

    private List<String> channels;
}
