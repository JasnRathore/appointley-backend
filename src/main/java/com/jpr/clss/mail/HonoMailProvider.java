package com.jpr.clss.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jpr.clss.entity.EmailJob;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Primary
@Service
public class HonoMailProvider implements MailProvider {

    private final RestTemplate restTemplate;
    private final String emailServiceUrl;

    public HonoMailProvider(
            @Value("${app.email-service.url:http://localhost:3000}") String emailServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.emailServiceUrl = emailServiceUrl;
    }

    @Override
    public void send(EmailJob emailJob) {
        log.info("Sending email to {} via Hono service at {}", emailJob.getRecipient(), emailServiceUrl);
        
        try {
            EmailRequest request = new EmailRequest();
            request.setRecipient(emailJob.getRecipient());
            request.setSenderName(emailJob.getSenderName());
            request.setSubject(emailJob.getSubject());
            request.setBody(emailJob.getBody());

            Map<String, Object> response = restTemplate.postForObject(
                    emailServiceUrl + "/send",
                    request,
                    Map.class
            );

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("Email sent successfully via Hono. MessageId: {}", response.get("messageId"));
            } else {
                log.error("Hono service returned error: {}", response);
                throw new RuntimeException("Email service error: " + response);
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorResponse = e.getResponseBodyAsString();
            log.error("Hono service failed with status {}: {}", e.getStatusCode(), errorResponse);
            throw new RuntimeException("Email service failed: " + errorResponse, e);
        } catch (Exception e) {
            log.error("Failed to connect to Hono service at {}", emailServiceUrl, e);
            throw new RuntimeException("Email service connection failed", e);
        }
    }

    @Data
    private static class EmailRequest {
        private String recipient;
        private String senderName;
        private String subject;
        private String body;
    }
}
