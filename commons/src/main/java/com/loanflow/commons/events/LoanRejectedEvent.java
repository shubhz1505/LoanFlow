package com.loanflow.commons.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LoanRejectedEvent extends BaseEvent {

    private String       loanApplicationId;
    private String       userId;
    private String       userEmail;
    private List<String> rejectionReasons;
    private String       rejectedByOfficerId;
    private LocalDateTime rejectedAt;
}