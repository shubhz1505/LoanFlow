package com.loanflow.document.ocr;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OcrExtractionService {

    private final AutoDetectParser parser = new AutoDetectParser();

    private static final Pattern[] INCOME_PATTERNS = {
            Pattern.compile(
                    "(?i)net\\s*(?:salary|pay|income|amount)[:\\s₹Rs.]*([0-9,]+)",
                    Pattern.MULTILINE),
            Pattern.compile(
                    "(?i)gross\\s*(?:salary|pay|income)[:\\s₹Rs.]*([0-9,]+)",
                    Pattern.MULTILINE),
            Pattern.compile(
                    "(?i)take\\s*home[:\\s₹Rs.]*([0-9,]+)",
                    Pattern.MULTILINE),
            Pattern.compile(
                    "(?i)ctc[:\\s₹Rs.]*([0-9,]+)",
                    Pattern.MULTILINE)
    };

    public String extractText(InputStream inputStream, String mimeType) {
        try {
            BodyContentHandler handler =
                    new BodyContentHandler(10 * 1024 * 1024);
            Metadata metadata = new Metadata();
            if (mimeType != null) {
                metadata.set(Metadata.CONTENT_TYPE, mimeType);
            }
            parser.parse(inputStream, handler, metadata, new ParseContext());
            String text = handler.toString();
            log.debug("Extracted {} characters from document", text.length());
            return text;
        } catch (Exception e) {
            log.error("OCR extraction failed: {}", e.getMessage());
            return "";
        }
    }

    public Optional<BigDecimal> extractIncome(String text) {
        if (text == null || text.isBlank()) return Optional.empty();

        for (Pattern pattern : INCOME_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    String raw = matcher.group(1).replaceAll(",", "").trim();
                    BigDecimal amount = new BigDecimal(raw);
                    if (amount.compareTo(BigDecimal.valueOf(5000)) > 0
                            && amount.compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
                        log.info("Extracted income: {}", amount);
                        return Optional.of(amount);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse income: {}", matcher.group(1));
                }
            }
        }
        return Optional.empty();
    }

    public boolean verifyIncome(BigDecimal extracted, BigDecimal declared) {
        if (extracted == null || declared == null) return false;
        BigDecimal tolerance = declared.multiply(BigDecimal.valueOf(0.15));
        BigDecimal lower = declared.subtract(tolerance);
        BigDecimal upper = declared.add(tolerance);
        boolean match = extracted.compareTo(lower) >= 0
                && extracted.compareTo(upper) <= 0;
        log.info("Income verification: declared={} extracted={} match={}",
                declared, extracted, match);
        return match;
    }
}