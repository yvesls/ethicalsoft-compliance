package com.ethicalsoft.ethicalsoft_complience.application.usecase.document;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.DocumentEmissionRecord;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.DocumentEmissionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterDocumentEmissionUseCase {

    public static final String TYPE_BULLETIN = "NON_COMPLIANCE_BULLETIN";
    public static final String TYPE_CERTIFICATE = "ETHICS_CERTIFICATE";
    public static final String CURRENT_TEMPLATE_VERSION = "v1";

    private final DocumentEmissionRecordRepository repository;

    public record EmissionRequest(
            String documentType,
            String authenticityCode,
            Long projectId,
            String projectName,
            Integer questionnaireId,
            String questionnaireName,
            Integer stageId,
            String stageName,
            Integer iterationId,
            String iterationName,
            String scopeLabel,
            Long emittedByUserId,
            String emittedByName,
            BigDecimal isepPercent,
            String band,
            List<Object> hashSourceParts) {
    }

    public DocumentEmissionRecord execute(EmissionRequest request) {
        if (request == null || request.authenticityCode() == null || request.documentType() == null) {
            throw new IllegalArgumentException("Dados insuficientes para registrar a emissão do documento.");
        }

        Optional<DocumentEmissionRecord> existing = safeFind(request.authenticityCode());
        if (existing.isPresent()) {
            log.info("[doc-emission] Registro já existente para código {}, retornando o existente.",
                    request.authenticityCode());
            return existing.get();
        }

        DocumentEmissionRecord doc = DocumentEmissionRecord.builder()
                .documentType(request.documentType())
                .authenticityCode(request.authenticityCode())
                .projectId(request.projectId())
                .projectName(request.projectName())
                .questionnaireId(request.questionnaireId())
                .questionnaireName(request.questionnaireName())
                .stageId(request.stageId())
                .stageName(request.stageName())
                .iterationId(request.iterationId())
                .iterationName(request.iterationName())
                .scopeLabel(request.scopeLabel())
                .emittedAt(LocalDateTime.now())
                .emittedByUserId(request.emittedByUserId())
                .emittedByName(request.emittedByName())
                .isepPercent(request.isepPercent())
                .band(request.band())
                .dataHash(sha256Hex(request.hashSourceParts()))
                .templateVersion(CURRENT_TEMPLATE_VERSION)
                .build();

        try {
            return repository.save(doc);
        } catch (Exception ex) {
            log.warn("[doc-emission] Falha ao salvar registro de emissão (code={}): {}",
                    request.authenticityCode(), ex.getMessage());
            return doc;
        }
    }

    public List<DocumentEmissionRecord> listByProject(Long projectId) {
        return repository.findByProjectIdOrderByEmittedAtDesc(projectId);
    }

    public List<DocumentEmissionRecord> listByProjectAndType(Long projectId, String documentType) {
        return repository.findByProjectIdAndDocumentTypeOrderByEmittedAtDesc(projectId, documentType);
    }

    public List<DocumentEmissionRecord> listByProjectAndQuestionnaire(Long projectId, Integer questionnaireId) {
        return repository.findByProjectIdAndQuestionnaireIdOrderByEmittedAtDesc(projectId, questionnaireId);
    }

    public Optional<DocumentEmissionRecord> findByAuthenticityCode(String code) {
        return safeFind(code);
    }

    private Optional<DocumentEmissionRecord> safeFind(String code) {
        try {
            return repository.findByAuthenticityCode(code);
        } catch (Exception ex) {
            log.warn("[doc-emission] Falha ao consultar registro por código {}: {}", code, ex.getMessage());
            return Optional.empty();
        }
    }

    private String sha256Hex(List<Object> parts) {
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        StringBuilder raw = new StringBuilder();
        for (Object part : parts) {
            raw.append(part == null ? "" : part.toString()).append('|');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return Integer.toHexString(raw.toString().hashCode());
        }
    }
}
