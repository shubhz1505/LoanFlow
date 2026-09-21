package com.loanflow.commons.events;

import com.loanflow.commons.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DocumentVerifiedEvent extends BaseEvent {

    private String       documentId;
    private String       loanApplicationId;
    private String       userId;
    private DocumentType documentType;
    private Boolean      allDocumentsComplete;
    private String       extractedIncome;
    private LocalDateTime verifiedAt;
}