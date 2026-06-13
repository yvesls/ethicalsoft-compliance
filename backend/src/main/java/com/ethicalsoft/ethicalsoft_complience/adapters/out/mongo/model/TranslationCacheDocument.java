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
@Document(collection = "translation_cache")
public class TranslationCacheDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String hash;

    private String language;

    private String original;

    private String translated;

    private LocalDateTime createdAt;
}
