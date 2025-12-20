package com.ethicalsoft.ethicalsoft_complience.application.usecase.template;

import com.ethicalsoft.ethicalsoft_complience.application.port.TemplateQueryPort;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetFullTemplateUseCase {

    private final TemplateQueryPort templateQueryPort;

    public ProjectTemplate execute(String id) {
        return templateQueryPort.findFullTemplateById(id);
    }
}
