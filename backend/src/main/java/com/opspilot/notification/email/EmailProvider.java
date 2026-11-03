package com.opspilot.notification.email;

public interface EmailProvider {
    void sendEmail(String toAddress, String subject, String body);
}
