package com.loanflow.document.service;

import com.loanflow.commons.enums.DocumentType;
import com.loanflow.commons.events.DocumentVerifiedEvent;
import com.loanflow.commons.kafka.EventPublisher;
import com.loanflow.commons.utils.CorrelationIdUtils;
import com.loanflow.document.entity.LoanDocument;
import com.loanflow.document.ocr.OcrExtractionService;
import com.loanflow.document.repository.LoanDocumentRepository;
import com.loanflow.document.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final LoanDocumentRepository documentRepository;
    private final MinioStorageService    storageService;
    private final OcrExtractionService   ocrService;
    private final EventPublisher         eventPublisher;

    private static final String TOPIC_DOCUMENT_VERIFIED = "document.verified";
    private static final String TOPIC_DOCUMENT_REJECTED = "document.rejected";

    private static final Set<DocumentType> REQUIRED_DOCS = Set.of(
            DocumentType.AADHAR_CARD,
            DocumentType.PAN_CARD,
            DocumentType.SALARY_SLIP,
            DocumentType.BANK_STATEMENT);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/jpg");

    @Transactional
    public LoanDocument uploadDocument(
            String loanApplicationId,
            String userId,
            DocumentType documentType,
            MultipartFile file,
            BigDecimal declaredMonthlyIncome,
            String correlationId) throws Exception {

        validateFile(file);

        documentRepository
                .findByLoanApplicationIdAndDocumentType(
                        loanApplicationId, documentType)
                .ifPresent(existing -> {
                    try {
                        storageService.delete(existing.getObjectKey());
                    } catch (Exception e) {
                        log.warn("Failed to delete old object: {}",
                                e.getMessage());
                    }
                    documentRepository.delete(existing);
                });

        String objectKey = storageService.upload(
                loanApplicationId, documentType.name(), file);

        String corrId = correlationId != null
                ? correlationId : CorrelationIdUtils.generate();

        LoanDocument document = LoanDocument.builder()
                .loanApplicationId(loanApplicationId)
                .userId(userId)
                .documentType(documentType)
                .bucketName("loanflow-documents")
                .objectKey(objectKey)
                .originalFileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .correlationId(corrId)
                .build();

        if (documentType == DocumentType.SALARY_SLIP
                || documentType == DocumentType.BANK_STATEMENT) {
            try {
                String text = ocrService.extractText(
                        storageService.getObject(objectKey),
                        file.getContentType());
                document.setExtractedText(
                        text.length() > 5000
                                ? text.substring(0, 5000) : text);

                Optional<BigDecimal> income = ocrService.extractIncome(text);
                income.ifPresent(amt -> {
                    document.setExtractedIncome(amt.toPlainString());
                    if (declaredMonthlyIncome != null) {
                        document.setIncomeVerified(
                                ocrService.verifyIncome(
                                        amt, declaredMonthlyIncome));
                    }
                });
            } catch (Exception e) {
                log.error("OCR failed for {}: {}", objectKey, e.getMessage());
            }
        }

        documentRepository.save(document);
        log.info("[{}] Document uploaded: {} for loan {}",
                corrId, documentType, loanApplicationId);
        return document;
    }

    @Transactional
    public void verifyDocument(String documentId,
                               String officerId,
                               String note) {
        LoanDocument doc = getDocument(documentId);
        doc.setVerified(true);
        doc.setRejected(false);
        doc.setVerificationNote(note);
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setVerifiedBy(officerId);
        documentRepository.save(doc);

        log.info("Document {} verified by {}", documentId, officerId);

        boolean allComplete = areAllDocumentsVerified(
                doc.getLoanApplicationId());

        publishVerifiedEvent(doc, allComplete);
    }

    @Transactional
    public void rejectDocument(String documentId,
                               String officerId,
                               String reason) {
        LoanDocument doc = getDocument(documentId);
        doc.setVerified(false);
        doc.setRejected(true);
        doc.setRejectionReason(reason);
        doc.setVerifiedBy(officerId);
        documentRepository.save(doc);

        log.info("Document {} rejected by {}: {}", documentId, officerId, reason);
        eventPublisher.publish(TOPIC_DOCUMENT_REJECTED,
                doc.getLoanApplicationId(), doc, doc.getCorrelationId());
    }

    public List<LoanDocument> getDocumentsForLoan(String loanApplicationId) {
        return documentRepository.findByLoanApplicationId(loanApplicationId);
    }

    public String getPresignedUrl(String documentId) throws Exception {
        LoanDocument doc = getDocument(documentId);
        return storageService.generatePresignedUrl(doc.getObjectKey());
    }

    public boolean areAllDocumentsVerified(String loanApplicationId) {
        return REQUIRED_DOCS.stream().allMatch(type ->
                documentRepository
                        .existsByLoanApplicationIdAndDocumentTypeAndVerifiedTrue(
                                loanApplicationId, type));
    }

    private void publishVerifiedEvent(LoanDocument doc, boolean allComplete) {
        DocumentVerifiedEvent event = DocumentVerifiedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(doc.getCorrelationId())
                .occurredAt(LocalDateTime.now())
                .sourceService("document-service")
                .eventVersion("1.0")
                .documentId(doc.getId())
                .loanApplicationId(doc.getLoanApplicationId())
                .userId(doc.getUserId())
                .documentType(doc.getDocumentType())
                .allDocumentsComplete(allComplete)
                .extractedIncome(doc.getExtractedIncome())
                .verifiedAt(doc.getVerifiedAt())
                .build();

        eventPublisher.publish(TOPIC_DOCUMENT_VERIFIED,
                doc.getLoanApplicationId(), event, doc.getCorrelationId());
        log.info("Published document.verified for loan {} allComplete={}",
                doc.getLoanApplicationId(), allComplete);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds 10MB limit");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + file.getContentType());
        }
    }

    private LoanDocument getDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException(
                        "Document not found: " + documentId));
    }
}