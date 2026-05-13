package com.jpr.clss.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
    private final EmailTemplateCatalog emailTemplateCatalog;

    public EmailTemplateService(
        EmailTemplateRepository emailTemplateRepository,
        EmailQueueService emailQueueService,
        EmailTemplateCatalog emailTemplateCatalog
    ) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailQueueService = emailQueueService;
        this.emailTemplateCatalog = emailTemplateCatalog;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateDto> getTemplatesForTeam(Team team) {
        Map<EmailType, EmailTemplate> templatesByType = emailTemplateRepository.findByTeamId(team.getId()).stream()
            .collect(java.util.stream.Collectors.toMap(EmailTemplate::getType, Function.identity()));

        return emailTemplateCatalog.getTeamManagedDefinitions().stream()
            .map(definition -> toDto(definition, templatesByType.get(definition.type())))
            .toList();
    }

    @Transactional
    public EmailTemplateDto updateTemplate(Team team, EmailType type, UpdateEmailTemplateRequest request) {
        EmailTemplateCatalog.EmailTemplateDefinition definition = emailTemplateCatalog.requireDefinition(type);
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
        return toDto(definition, template);
    }

    @Transactional
    public void resetTemplate(Team team, EmailType type) {
        emailTemplateCatalog.requireDefinition(type);
        emailTemplateRepository.findByTeamIdAndType(team.getId(), type)
            .ifPresent(emailTemplateRepository::delete);
    }

    public void queueWithTemplate(EmailType type, Team team, String recipient, Map<String, String> variables) {
        EmailTemplateCatalog.EmailTemplateDefinition definition = emailTemplateCatalog.requireDefinition(type);
        Optional<EmailTemplate> optionalTemplate = emailTemplateRepository.findByTeamIdAndType(team.getId(), type);
        Map<String, String> mergedVariables = new LinkedHashMap<>();
        mergedVariables.put("teamName", team.getName());
        mergedVariables.put("senderName", team.getSenderName());
        mergedVariables.putAll(variables);

        String subject = definition.defaultSubjectTemplate();
        String body = definition.defaultBodyTemplate();

        if (optionalTemplate.isPresent() && optionalTemplate.get().isActive()) {
            EmailTemplate template = optionalTemplate.get();
            subject = template.getSubjectTemplate();
            body = template.getBodyTemplate();
        }

        subject = interpolate(subject, mergedVariables);
        body = interpolate(body, mergedVariables);
        
        String finalSenderName = mergedVariables.getOrDefault("senderName", team.getSenderName());
        emailQueueService.queue(type, recipient, finalSenderName, subject, body);
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

    private EmailTemplateDto toDto(EmailTemplateCatalog.EmailTemplateDefinition definition, EmailTemplate template) {
        return new EmailTemplateDto(
            definition.type(),
            definition.displayName(),
            definition.description(),
            template != null ? template.getSubjectTemplate() : definition.defaultSubjectTemplate(),
            template != null ? template.getBodyTemplate() : definition.defaultBodyTemplate(),
            definition.defaultSubjectTemplate(),
            definition.defaultBodyTemplate(),
            template != null && template.isActive(),
            template != null,
            definition.variables()
        );
    }
}
