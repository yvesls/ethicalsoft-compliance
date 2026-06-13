package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_ai_tokens")
public class UserAiTokenDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    private String encryptedToken;

    private String tokenHint;

    private String provider;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
