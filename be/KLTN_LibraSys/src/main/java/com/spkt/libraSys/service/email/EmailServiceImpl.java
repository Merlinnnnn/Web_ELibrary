package com.spkt.libraSys.service.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailServiceImpl implements EmailService {

    private static String EMAIL_HOST = "quy2003@wuy.id.vn";
    @Autowired
    private JavaMailSender mailSender;
//    @Autowired
//    EmailRepository emailRepository;

    @Override
    @Async
    public CompletableFuture<Boolean> sendTextEmail(EmailEntity email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email.getToEmail());
        message.setSubject(email.getSubject());
        message.setText(email.getBody());
        message.setFrom(EMAIL_HOST);
        try{
            mailSender.send(message);
            System.out.println("SendTextEmail sent thanh cong ");
        }catch (Exception e) {
           // throw new RuntimeException(e);
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(true);

    }

    @Override
    public String sendHtmlEmail(EmailEntity email) {
        try{
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(EMAIL_HOST);
            helper.setTo(email.getToEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), true);
            mailSender.send(message);
            System.out.println("Email sent successfully");
            return "Email sent successfully";
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public String sendAttachmentsEmail(EmailEntity email) {
        return "";
    }
    @Async
    public CompletableFuture<Boolean> sendEmailAsync(String toEmail, String subject, String body) {
        // Tạo email để lưu vào database
        EmailEntity email = EmailEntity.builder()
                .toEmail(toEmail)
                .subject(subject)
                .body(body)
                .createdAt(LocalDateTime.now())
                .build();

        return CompletableFuture.supplyAsync(() -> {
            try {
                sendHtmlEmail(email); // Gửi email qua service
                email.setStatus("SUCCESS"); // Đánh dấu trạng thái gửi thành công
            } catch (Exception e) {
                email.setStatus("FAILED"); // Đánh dấu trạng thái thất bại
            }
            // Lưu email vào database
           // emailRepository.save(email);
            return email.getStatus().equals("SUCCESS");
        });
    }

}