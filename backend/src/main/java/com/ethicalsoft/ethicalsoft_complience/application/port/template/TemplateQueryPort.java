package com.ethicalsoft.ethicalsoft_complience.application.port.template;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateListDTO;

import java.util.List;

public interface TemplateQueryPort {
    List<TemplateListDTO> findAllTemplates();
    ProjectTemplate findFullTemplateById(String templateMongoId);
}

