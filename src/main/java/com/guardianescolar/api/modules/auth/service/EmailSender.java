package com.guardianescolar.api.modules.auth.service;

public interface EmailSender {

    void send(String recipient, String from, String subject, String text, String html);
}
