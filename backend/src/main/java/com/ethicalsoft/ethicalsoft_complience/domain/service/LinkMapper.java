package com.ethicalsoft.ethicalsoft_complience.domain.service;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.LinkDTO;
import org.springframework.stereotype.Service;

@Service
public class LinkMapper {

    public LinkDTO toDto(QuestionnaireResponse.LinkDocument doc) {
        if (doc == null) return null;
        LinkDTO dto = new LinkDTO();
        dto.setDescricao(doc.getDescricao());
        dto.setUrl(doc.getUrl());
        return dto;
    }

    public QuestionnaireResponse.LinkDocument toDocument(LinkDTO dto) {
        if (dto == null) return null;
        QuestionnaireResponse.LinkDocument doc = new QuestionnaireResponse.LinkDocument();
        doc.setDescricao(dto.getDescricao());
        doc.setUrl(dto.getUrl());
        return doc;
    }
}

