package com.ethicalsoft.ethicalsoft_complience.application.usecase.template;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateListDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.TemplateQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListTemplatesUseCase {

    private final TemplateQueryPort templateQueryPort;

    public List<TemplateListDTO> execute() {
        return templateQueryPort.findAllTemplates();
    }
}
