package com.jpr.clss.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpr.clss.entity.EmailJob;
import com.jpr.clss.entity.EmailJobStatus;
import com.jpr.clss.entity.EmailType;
import com.jpr.clss.mail.MailProvider;
import com.jpr.clss.repository.EmailJobRepository;

@Service
public class EmailQueueService {

    private final EmailJobRepository emailJobRepository;
    private final MailProvider mailProvider;

    public EmailQueueService(EmailJobRepository emailJobRepository, MailProvider mailProvider) {
        this.emailJobRepository = emailJobRepository;
        this.mailProvider = mailProvider;
    }

    public void queue(EmailType type, String recipient, String senderName, String subject, String body) {
        EmailJob emailJob = new EmailJob();
        emailJob.setType(type);
        emailJob.setRecipient(recipient);
        emailJob.setSenderName(senderName);
        emailJob.setSubject(subject);
        emailJob.setBody(body);
        emailJobRepository.save(emailJob);
    }

    @Transactional
    @Scheduled(fixedDelay = 30000)
    public void processQueue() {
        for (EmailJob emailJob : emailJobRepository.findTop10ByStatusOrderByCreatedAtAsc(EmailJobStatus.PENDING)) {
            try {
                mailProvider.send(emailJob);
                emailJob.setStatus(EmailJobStatus.SENT);
                emailJob.setSentAt(Instant.now());
                emailJob.setAttempts(emailJob.getAttempts() + 1);
                emailJob.setLastError(null);
            } catch (Exception exception) {
                emailJob.setStatus(EmailJobStatus.FAILED);
                emailJob.setAttempts(emailJob.getAttempts() + 1);
                emailJob.setLastError(exception.getMessage());
            }
        }
    }
}
