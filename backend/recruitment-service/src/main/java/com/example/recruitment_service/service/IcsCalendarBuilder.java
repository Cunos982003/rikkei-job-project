package com.example.recruitment_service.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class IcsCalendarBuilder {
    private static final DateTimeFormatter ICS_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private IcsCalendarBuilder() {
    }

    public static String buildInterviewInvite(
            UUID scheduleId,
            String summary,
            String description,
            String meetUrl,
            Instant startAt,
            Instant endAt,
            Instant stampAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Rikkei Jobs//Interview Scheduler UT-06//VI\r\n");
        sb.append("METHOD:REQUEST\r\n");
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(scheduleId).append("@rikkeijobs.vn\r\n");
        sb.append("DTSTAMP:").append(ICS_UTC.format(stampAt)).append("\r\n");
        sb.append("DTSTART:").append(ICS_UTC.format(startAt)).append("\r\n");
        sb.append("DTEND:").append(ICS_UTC.format(endAt)).append("\r\n");
        sb.append("SUMMARY:").append(escape(summary)).append("\r\n");
        sb.append("DESCRIPTION:").append(escape(description)).append("\r\n");
        sb.append("LOCATION:").append(escape(meetUrl)).append("\r\n");
        sb.append("URL:").append(escape(meetUrl)).append("\r\n");
        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }
}
