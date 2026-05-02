package com.example.healthcare.service.mail;

public interface EmailSender {
    void send(String toEmail, String subject, String body);
}
