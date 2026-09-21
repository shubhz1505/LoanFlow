package com.loanflow.user.dto.request;

import com.loanflow.commons.enums.KycStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycReviewRequest {

    @NotBlank
    private String userId;

    @NotNull
    private KycStatus decision;

    private String rejectionReason;
}