package com.example.common.logging;

import ch.qos.logback.core.pattern.CompositeConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback Converter to automatically mask PII (Passwords, JWT Tokens, Emails, Phone Numbers, CV Content)
 * before logs are written to stdout / Loki / ELK.
 */
public class PiiMaskingConverter extends CompositeConverter<ILoggingEvent> {

    // 1. Password in JSON, query params, or plain text: "password": "...", password=..., password: ...
    private static final Pattern PASSWORD_COLON_PATTERN = Pattern.compile(
            "(?i)(\"?password\"?\\s*[:=]\\s*\"?)([^\\r\\n\",;\\s]+)(\"?)");
    private static final Pattern PASSWORD_PARAM_PATTERN = Pattern.compile(
            "(?i)(password=)([^&,\\s]+)");

    // 2. JWT Tokens: Bearer eyJ... or standalone eyJ...
    private static final Pattern BEARER_JWT_PATTERN = Pattern.compile(
            "(?i)(Bearer\\s+)[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*");
    private static final Pattern RAW_JWT_PATTERN = Pattern.compile(
            "\\beyJ[A-Za-z0-9-_=]{10,}\\.[A-Za-z0-9-_=]{10,}\\.?[A-Za-z0-9-_.+/=]*\\b");

    // 3. Email addresses: candidate.alex@gmail.com -> c***@gmail.com
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

    // 4. Phone numbers (VN standard): 0987654321, +84987654321 (boundaries prevent matching UUIDs)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<![a-zA-Z0-9-])(?:\\+?84|0)(3[2-9]|5[689]|7[06-9]|8[1-9]|9[0-9])(\\d{4})(\\d{3})(?![a-zA-Z0-9-])");

    // 5. CV / Resume Raw Content
    private static final Pattern CV_CONTENT_PATTERN = Pattern.compile(
            "(?i)(\"?(?:cvContent|resumeText|cvData|cvSnapshot|rawCv|resumeContent)\"?\\s*[:=]\\s*\")([^\"]*)(\")");

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null || in.isEmpty()) {
            return in;
        }
        return mask(in);
    }

    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // Mask Passwords
        result = PASSWORD_COLON_PATTERN.matcher(result).replaceAll("$1******$3");
        result = PASSWORD_PARAM_PATTERN.matcher(result).replaceAll("$1******");

        // Mask JWT
        result = BEARER_JWT_PATTERN.matcher(result).replaceAll("$1[JWT_MASKED]");
        result = RAW_JWT_PATTERN.matcher(result).replaceAll("[JWT_MASKED]");

        // Mask CV Content
        result = CV_CONTENT_PATTERN.matcher(result).replaceAll("$1[CV_CONTENT_MASKED]$3");

        // Mask Email
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1***@$2");

        // Mask Phone Number: 0987654321 -> 098****321
        Matcher phoneMatcher = PHONE_PATTERN.matcher(result);
        if (phoneMatcher.find()) {
            StringBuilder sb = new StringBuilder();
            do {
                String full = phoneMatcher.group();
                String prefix = full.startsWith("+84") ? "+84" : "0";
                String middle = "****";
                String end = phoneMatcher.group(3);
                phoneMatcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + phoneMatcher.group(1) + middle + end));
            } while (phoneMatcher.find());
            phoneMatcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }
}
