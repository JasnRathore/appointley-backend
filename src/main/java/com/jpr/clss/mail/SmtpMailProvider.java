package com.jpr.clss.mail;

import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.jpr.clss.entity.EmailJob;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Primary
@Service
public class SmtpMailProvider implements MailProvider {

    private final JavaMailSender mailSender;

    public SmtpMailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(EmailJob emailJob) {
        log.info("Sending email to {} with subject: {}", emailJob.getRecipient(), emailJob.getSubject());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailJob.getSenderName() + " <no-reply@appointley.com>");
            message.setTo(emailJob.getRecipient());
            message.setSubject(emailJob.getSubject());
            message.setText(emailJob.getBody());
            mailSender.send(message);
            log.info("Email sent successfully to {}", emailJob.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send email to {}", emailJob.getRecipient(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
