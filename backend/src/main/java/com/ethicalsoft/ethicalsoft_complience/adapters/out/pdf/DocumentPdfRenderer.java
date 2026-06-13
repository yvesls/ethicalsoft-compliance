package com.ethicalsoft.ethicalsoft_complience.adapters.out.pdf;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentPdfRenderer {

    private final FreeMarkerConfigurer freemarkerConfig;

    public byte[] render(String templatePath, Map<String, Object> model) {
        try {
            var template = freemarkerConfig.getConfiguration().getTemplate(templatePath);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template,
                    model == null ? Map.of() : model);
            String xhtml = sanitizeToXhtml(html);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(xhtml, null);
                builder.toStream(out);
                builder.run();
                return out.toByteArray();
            }
        } catch (IOException | TemplateException | RuntimeException ex) {
            log.error("[pdf-renderer] Falha ao renderizar template {}", templatePath, ex);
            throw new BusinessException("Falha ao gerar o documento PDF: " + ex.getMessage());
        }
    }

    private String sanitizeToXhtml(String html) {
        if (html == null) {
            return "";
        }
        return html
                .replace("&nbsp;", "&#160;")
                .replace("<br>", "<br/>")
                .replace("<hr>", "<hr/>");
    }
}
