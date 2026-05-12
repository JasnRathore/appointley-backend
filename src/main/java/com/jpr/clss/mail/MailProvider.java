package com.jpr.clss.mail;

import com.jpr.clss.entity.EmailJob;

public interface MailProvider {
    void send(EmailJob emailJob);
}
