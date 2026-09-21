package com.loanflow.document.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.commons.enums.DocumentType;
import com.loanflow.document.entity.LoanDocument;
import com.loanflow.document.service.DocumentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/loan/{loanId}/upload")
    public ResponseEntity<ApiResponse<LoanDocument>> upload(
            @PathVariable String loanId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "declaredMonthlyIncome",
                    required = false) BigDecimal declaredIncome,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Correlation-ID",
                    required = false) String correlationId)
            throws Exception {

        LoanDocument doc = documentService.uploadDocument(
                loanId, userId, documentType,
                file, declaredIncome, correlationId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(doc, "Document uploaded successfully"));
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<ApiResponse<List<LoanDocument>>> getDocuments(
            @PathVariable String loanId) {

        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocumentsForLoan(loanId)));
    }

    @PostMapping("/{documentId}/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @PathVariable String documentId,
            @RequestParam(required = false) String note,
            @RequestHeader("X-User-Id") String officerId) {

        documentService.verifyDocument(documentId, officerId, note);
        return ResponseEntity.ok(ApiResponse.success(null, "Document verified"));
    }

    @PostMapping("/{documentId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable String documentId,
            @RequestParam String reason,
            @RequestHeader("X-User-Id") String officerId) {

        documentService.rejectDocument(documentId, officerId, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Document rejected"));
    }

    @GetMapping("/{documentId}/url")
    public ResponseEntity<ApiResponse<String>> getPresignedUrl(
            @PathVariable String documentId) throws Exception {

        return ResponseEntity.ok(ApiResponse.success(
                documentService.getPresignedUrl(documentId)));
    }

    @GetMapping("/loan/{loanId}/complete")
    public ResponseEntity<ApiResponse<Boolean>> areDocumentsComplete(
            @PathVariable String loanId) {

        return ResponseEntity.ok(ApiResponse.success(
                documentService.areAllDocumentsVerified(loanId)));
    }
}