package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.EmitNonComplianceBulletinUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.GenerateComplianceCertificateUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.GenerateNonComplianceBulletinUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.RegisterDocumentEmissionUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.DocumentEmissionRecordDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/projects/{projectId}")
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
            @AuthenticationPrincipal User currentUser) {
        GenerateNonComplianceBulletinUseCase.GeneratedBulletin bulletin =
                generateBulletinUseCase.execute(projectId, questionnaireId, actorName(currentUser));
        Long userId = currentUser != null ? currentUser.getId() : null;

        var emission = registerEmissionUseCase.execute(new RegisterDocumentEmissionUseCase.EmissionRequest(
                RegisterDocumentEmissionUseCase.TYPE_BULLETIN,
                bulletin.documentCode(),
                projectId,
                bulletin.projectName(),
                questionnaireId,
                bulletin.questionnaireName(),
                null, null, null, null,
                "Questionário",
                userId,
                actorName(currentUser),
                null,
                bulletin.band(),
                List.of(projectId, questionnaireId, bulletin.band(), bulletin.isepPercent())));

        return ResponseEntity.ok(DocumentEmissionRecordDTO.from(emission));
    }

    @PostMapping("/certificate/register-emission")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<DocumentEmissionRecordDTO> registerCertificateEmission(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {
        GenerateComplianceCertificateUseCase.GeneratedCertificate certificate =
                generateCertificateUseCase.execute(projectId, actorName(currentUser));
        Long userId = currentUser != null ? currentUser.getId() : null;

        var emission = registerEmissionUseCase.execute(new RegisterDocumentEmissionUseCase.EmissionRequest(
                RegisterDocumentEmissionUseCase.TYPE_CERTIFICATE,
                certificate.certificateCode(),
                projectId,
                certificate.projectName(),
                null, null, null, null, null, null,
                "Projeto",
                userId,
                actorName(currentUser),
                null,
                certificate.band(),
                List.of(projectId, certificate.band(), certificate.isepPercent())));

        return ResponseEntity.ok(DocumentEmissionRecordDTO.from(emission));
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
