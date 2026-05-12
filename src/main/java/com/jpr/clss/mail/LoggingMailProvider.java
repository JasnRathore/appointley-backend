package com.jpr.clss.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jpr.clss.entity.EmailJob;

@Component
public class LoggingMailProvider implements MailProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMailProvider.class);

    @Override
    public void send(EmailJob emailJob) {
        LOGGER.info("Sending email [{}] to {} with subject {}", emailJob.getType(), emailJob.getRecipient(), emailJob.getSubject());
    }
}
