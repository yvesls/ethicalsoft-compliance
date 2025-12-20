package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateListDTO;

import java.util.List;

public interface TemplateQueryPort {
    List<TemplateListDTO> findAllTemplates();
    ProjectTemplate findFullTemplateById(String templateMongoId);
}

