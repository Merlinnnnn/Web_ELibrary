package com.spkt.libraSys.service.email;


import java.util.concurrent.CompletableFuture;

/**
 * Service interface for handling email operations in the library system.
 * Provides methods for sending different types of emails (text, HTML, with attachments)
 * both synchronously and asynchronously.
 */
public interface EmailService {
    /**
     * Sends a text email asynchronously.
     * @param email Email entity containing the email details
     * @return CompletableFuture<Boolean> indicating the success of the email sending operation
     */
    CompletableFuture<Boolean> sendTextEmail(EmailEntity email);

    /**
     * Sends an HTML email synchronously.
     * @param email Email entity containing the email details
     * @return String containing the result of the email sending operation
     */
    String sendHtmlEmail(EmailEntity email);

    /**
     * Sends an email with attachments synchronously.
     * @param email Email entity containing the email details and attachments
     * @return String containing the result of the email sending operation
     */
    String sendAttachmentsEmail(EmailEntity email);

    /**
     * Sends an email asynchronously with basic email parameters.
     * @param toEmail Recipient's email address
     * @param subject Email subject
     * @param body Email body content
     * @return CompletableFuture<Boolean> indicating the success of the email sending operation
     */
    CompletableFuture<Boolean> sendEmailAsync(String toEmail, String subject, String body);
}