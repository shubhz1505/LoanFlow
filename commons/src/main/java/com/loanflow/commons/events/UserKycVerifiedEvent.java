package com.loanflow.commons.events;

import com.loanflow.commons.enums.KycStatus;
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
@EqualsAndHashCode(callSuper = true)
public class UserKycVerifiedEvent extends BaseEvent {

    private String     userId;
    private String     email;
    private String     fullName;
    private KycStatus  kycStatus;
    private LocalDateTime verifiedAt;
}