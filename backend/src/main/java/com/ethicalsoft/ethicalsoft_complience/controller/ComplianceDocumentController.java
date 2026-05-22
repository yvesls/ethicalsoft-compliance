package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.EmitNonComplianceBulletinUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.GenerateComplianceCertificateUseCase;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.document.GenerateNonComplianceBulletinUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/projects/{projectId}")
@RequiredArgsConstructor
public class ComplianceDocumentController {

    private final GenerateNonComplianceBulletinUseCase generateBulletinUseCase;
    private final GenerateComplianceCertificateUseCase generateCertificateUseCase;
    private final EmitNonComplianceBulletinUseCase emitBulletinUseCase;

    @GetMapping("/questionnaires/{questionnaireId}/bulletin")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<byte[]> generateBulletin(@PathVariable Long projectId,
                                                   @PathVariable Integer questionnaireId,
                                                   @AuthenticationPrincipal User currentUser) {
        GenerateNonComplianceBulletinUseCase.GeneratedBulletin bulletin =
                generateBulletinUseCase.execute(projectId, questionnaireId, actorName(currentUser));
        return pdfResponse(bulletin.content(), bulletin.fileName());
    }

    @PostMapping("/questionnaires/{questionnaireId}/bulletin/emit")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<EmitNonComplianceBulletinUseCase.BulletinEmissionResult> emitBulletin(
            @PathVariable Long projectId,
            @PathVariable Integer questionnaireId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                emitBulletinUseCase.execute(projectId, questionnaireId, actorName(currentUser)));
    }

    @GetMapping("/certificate")
    @PreAuthorize("@projectAccessAuthorizationEvaluator.canAccess(authentication)")
    public ResponseEntity<byte[]> generateCertificate(@PathVariable Long projectId,
                                                      @AuthenticationPrincipal User currentUser) {
        GenerateComplianceCertificateUseCase.GeneratedCertificate certificate =
                generateCertificateUseCase.execute(projectId, actorName(currentUser));
        return pdfResponse(certificate.content(), certificate.fileName());
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
