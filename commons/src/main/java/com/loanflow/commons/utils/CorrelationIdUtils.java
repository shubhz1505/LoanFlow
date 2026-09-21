package com.loanflow.commons.utils;

import java.util.UUID;

public class CorrelationIdUtils {

    public static final String HEADER_NAME = "X-Correlation-ID";

    private CorrelationIdUtils() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static String generateWithPrefix(String prefix) {
        return prefix + "-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();
    }
}