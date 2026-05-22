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
@Document(collection = "pdf_document_configs")
public class PdfDocumentConfigDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    private String templateLink;

    private String documentTitle;

    private String systemName;

    private String validationUrl;

    private String issuerLabel;

    private String footerNote;

    private String impactSummary;

    private List<String> defaultCorrectiveActions;
}
