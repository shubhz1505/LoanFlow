package com.loanflow.commons.events;

import jdk.jfr.DataAmount;
import lombok.AllArgsConstructor;
import lombok.data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor

public abstract class BaseEvent {

    private String eventId;
    private String correlationId;
    private LocalDateTime occurredAt;
    private String sourceService;
    private String eventVersion;
}
