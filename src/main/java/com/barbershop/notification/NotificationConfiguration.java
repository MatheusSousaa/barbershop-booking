package com.barbershop.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
public class NotificationConfiguration {

    @Bean
    public NotificationService notificationService(Environment environment) {
        String host = environment.getProperty("SMTP_HOST", "").trim();
        String username = environment.getProperty("SMTP_USERNAME", "").trim();
        String password = environment.getProperty("SMTP_PASSWORD", "").trim();
        String recipient = environment.getProperty("SMTP_RECIPIENT", "").trim();
        String portValue = environment.getProperty("SMTP_PORT", "").trim();

        if (host.isEmpty() || username.isEmpty() || password.isEmpty() || recipient.isEmpty() || portValue.isEmpty()) {
            return new LogNotificationService();
        }

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("SMTP_PORT deve ser uma porta numérica válida.", exception);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("SMTP_PORT deve estar entre 1 e 65535.");
        }

        JavaMailSender mailSender = createMailSender(host, port, username, password);
        return new EmailNotificationService(mailSender, username, recipient);
    }

    private JavaMailSender createMailSender(String host, int port, String username, String password) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties mailProperties = mailSender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        mailProperties.put("mail.smtp.connectiontimeout", "5000");
        mailProperties.put("mail.smtp.timeout", "5000");
        mailProperties.put("mail.smtp.writetimeout", "5000");
        return mailSender;
    }
}
