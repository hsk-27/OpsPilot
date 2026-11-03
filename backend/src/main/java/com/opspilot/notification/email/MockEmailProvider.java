package com.opspilot.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MockEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(MockEmailProvider.class);

    public record SentEmail(String toAddress, String subject, String body, Instant sentAt) {}

    private final List<SentEmail> sentEmails = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendEmail(String toAddress, String subject, String body) {
        SentEmail email = new SentEmail(toAddress, subject, body, Instant.now());
        sentEmails.add(email);
        log.info("[MOCK EMAIL] Delivered to='{}', subject='{}', size={} chars", toAddress, subject, body != null ? body.length() : 0);
    }

    public List<SentEmail> getSentEmails() {
        return Collections.unmodifiableList(sentEmails);
    }

    public void clear() {
        sentEmails.clear();
    }
}
