package com.jpr.clss.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpr.clss.dto.settings.EmailTemplateDto;
import com.jpr.clss.dto.settings.UpdateEmailTemplateRequest;
import com.jpr.clss.entity.EmailTemplate;
import com.jpr.clss.entity.EmailType;
import com.jpr.clss.entity.Team;
import com.jpr.clss.repository.EmailTemplateRepository;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailQueueService emailQueueService;

    public EmailTemplateService(EmailTemplateRepository emailTemplateRepository, EmailQueueService emailQueueService) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailQueueService = emailQueueService;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateDto> getTemplatesForTeam(String teamId) {
        return emailTemplateRepository.findByTeamId(teamId).stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public EmailTemplateDto updateTemplate(Team team, EmailType type, UpdateEmailTemplateRequest request) {
        EmailTemplate template = emailTemplateRepository.findByTeamIdAndType(team.getId(), type)
            .orElseGet(() -> {
                EmailTemplate newTemplate = new EmailTemplate();
                newTemplate.setTeam(team);
                newTemplate.setType(type);
                return newTemplate;
            });
        
        template.setSubjectTemplate(request.subjectTemplate());
        template.setBodyTemplate(request.bodyTemplate());
        template.setActive(request.active());
        
        emailTemplateRepository.save(template);
        return toDto(template);
    }

    public void queueWithTemplate(EmailType type, Team team, String recipient, Map<String, String> variables, String defaultSubject, String defaultBody) {
        Optional<EmailTemplate> optionalTemplate = emailTemplateRepository.findByTeamIdAndType(team.getId(), type);
        
        String subject = defaultSubject;
        String body = defaultBody;
        
        if (optionalTemplate.isPresent() && optionalTemplate.get().isActive()) {
            EmailTemplate template = optionalTemplate.get();
            subject = interpolate(template.getSubjectTemplate(), variables);
            body = interpolate(template.getBodyTemplate(), variables);
        } else {
            // Also interpolate defaults just in case they have variables
            subject = interpolate(subject, variables);
            body = interpolate(body, variables);
        }
        
        emailQueueService.queue(type, recipient, team.getSenderName(), subject, body);
    }

    private String interpolate(String template, Map<String, String> variables) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private EmailTemplateDto toDto(EmailTemplate template) {
        return new EmailTemplateDto(
            template.getId(),
            template.getType(),
            template.getSubjectTemplate(),
            template.getBodyTemplate(),
            template.isActive()
        );
    }
}
