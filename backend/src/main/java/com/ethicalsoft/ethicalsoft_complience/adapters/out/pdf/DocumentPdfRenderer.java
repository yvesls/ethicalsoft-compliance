package com.ethicalsoft.ethicalsoft_complience.adapters.out.pdf;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentPdfRenderer {

    private final FreeMarkerConfigurer freemarkerConfig;

    public byte[] render(String templatePath, Map<String, Object> model) {
        try {
            var template = freemarkerConfig.getConfiguration().getTemplate(templatePath);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(
                    template, escapeModel(model));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();

            return output.toByteArray();
        } catch (Exception e) {
            log.error("[pdf-renderer] Falha ao gerar PDF a partir do template {}", templatePath, e);
            throw new BusinessException("Falha ao gerar o documento PDF: " + e.getMessage());
        }
    }

    private Map<String, Object> escapeModel(Map<String, Object> model) {
        Map<String, Object> escaped = new LinkedHashMap<>();
        if (model != null) {
            model.forEach((key, value) -> escaped.put(key, escapeValue(value)));
        }
        return escaped;
    }

    @SuppressWarnings("unchecked")
    private Object escapeValue(Object value) {
        if (value instanceof String text) {
            return escapeXml(text);
        }
        if (value instanceof Map<?, ?> map) {
            return escapeModel((Map<String, Object>) map);
        }
        if (value instanceof List<?> list) {
            List<Object> escapedList = new ArrayList<>(list.size());
            list.forEach(item -> escapedList.add(escapeValue(item)));
            return escapedList;
        }
        return value;
    }

    private String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
