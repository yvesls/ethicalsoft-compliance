package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.EmitNonComplianceBulletinUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.GenerateComplianceCertificateUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.GenerateNonComplianceBulletinUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.RegisterDocumentEmissionUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.DocumentEmissionRecordDTO;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ComplianceDocumentController {

    private final GenerateNonComplianceBulletinUseCase generateBulletinUseCase;
    private final GenerateComplianceCertificateUseCase generateCertificateUseCase;
    private final EmitNonComplianceBulletinUseCase emitBulletinUseCase;
    private final RegisterDocumentEmissionUseCase registerEmissionUseCase;

    @GetMapping("/questionnaires/{questionnaireId}/bulletin")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<byte[]> viewBulletin(@PathVariable Long projectId,
                                               @PathVariable Integer questionnaireId,
                                               @AuthenticationPrincipal User currentUser) {
        GenerateNonComplianceBulletinUseCase.GeneratedBulletin bulletin =
                generateBulletinUseCase.execute(projectId, questionnaireId, actorName(currentUser));
        return pdfResponse(bulletin.content(), bulletin.fileName());
    }

    @GetMapping("/certificate")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<byte[]> viewCertificate(@PathVariable Long projectId,
                                                  @AuthenticationPrincipal User currentUser) {
        GenerateComplianceCertificateUseCase.GeneratedCertificate certificate =
                generateCertificateUseCase.execute(projectId, actorName(currentUser));
        return pdfResponse(certificate.content(), certificate.fileName());
    }

    @PostMapping("/questionnaires/{questionnaireId}/bulletin/emit")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<EmitNonComplianceBulletinUseCase.BulletinEmissionResult> emitBulletin(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @AuthenticationPrincipal User currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(
                emitBulletinUseCase.execute(projectId, questionnaireId, actorName(currentUser), userId));
    }

    @PostMapping("/questionnaires/{questionnaireId}/bulletin/register-emission")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<DocumentEmissionRecordDTO> registerBulletinEmission(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @RequestParam(required = false) String documentCode,
            @AuthenticationPrincipal User currentUser) {
        GenerateNonComplianceBulletinUseCase.BulletinMetadata meta =
                generateBulletinUseCase.prepareMetadata(projectId, questionnaireId, documentCode);
        Long userId = currentUser != null ? currentUser.getId() : null;

        var result = registerEmissionUseCase.execute(new RegisterDocumentEmissionUseCase.EmissionRequest(
                RegisterDocumentEmissionUseCase.TYPE_BULLETIN,
                meta.documentCode(),
                projectId,
                meta.projectName(),
                questionnaireId,
                meta.questionnaireName(),
                null, null, null, null,
                "Questionário",
                userId,
                actorName(currentUser),
                meta.isepValue(),
                meta.band(),
                Arrays.asList(projectId, questionnaireId, meta.band(), meta.isepValue())));

        if (!result.persisted()) {
            throw new BusinessException(
                    "Falha ao persistir o registro de emissão do boletim. Tente novamente.");
        }
        return ResponseEntity.ok(DocumentEmissionRecordDTO.from(result.record()));
    }

    @PostMapping("/certificate/register-emission")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<DocumentEmissionRecordDTO> registerCertificateEmission(
            @PathVariable Long projectId,
            @RequestParam(required = false) String certificateCode,
            @AuthenticationPrincipal User currentUser) {
        GenerateComplianceCertificateUseCase.CertificateMetadata meta =
                generateCertificateUseCase.prepareMetadata(projectId, certificateCode);
        Long userId = currentUser != null ? currentUser.getId() : null;

        var result = registerEmissionUseCase.execute(new RegisterDocumentEmissionUseCase.EmissionRequest(
                RegisterDocumentEmissionUseCase.TYPE_CERTIFICATE,
                meta.certificateCode(),
                projectId,
                meta.projectName(),
                null, null, null, null, null, null,
                "Projeto",
                userId,
                actorName(currentUser),
                meta.isepValue(),
                meta.band(),
                Arrays.asList(projectId, meta.band(), meta.isepValue())));

        if (!result.persisted()) {
            throw new BusinessException(
                    "Falha ao persistir o registro de emissão do certificado. Tente novamente.");
        }
        return ResponseEntity.ok(DocumentEmissionRecordDTO.from(result.record()));
    }

    @GetMapping("/documents/emissions")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public List<DocumentEmissionRecordDTO> listEmissions(@PathVariable Long projectId,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) Integer questionnaireId) {
        if (questionnaireId != null) {
            return registerEmissionUseCase.listByProjectAndQuestionnaire(projectId, questionnaireId)
                    .stream().map(DocumentEmissionRecordDTO::from).toList();
        }
        if (type != null && !type.isBlank()) {
            return registerEmissionUseCase.listByProjectAndType(projectId, type)
                    .stream().map(DocumentEmissionRecordDTO::from).toList();
        }
        return registerEmissionUseCase.listByProject(projectId)
                .stream().map(DocumentEmissionRecordDTO::from).toList();
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] content, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    private String actorName(User user) {
        if (user == null) {
            return "Analista de Qualidade";
        }
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Analista de Qualidade" : full;
    }
}
