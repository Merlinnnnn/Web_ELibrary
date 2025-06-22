package com.spkt.libraSys.service.dialogflow;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class DialogflowConfig {

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try {
            return GoogleCredentials.fromStream(new ClassPathResource("secrets/dialogflow-key.json").getInputStream())
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google credentials", e);
        }
    }

    @Bean
    public SessionsClient sessionsClient() throws IOException {
        try {
            GoogleCredentials credentials = googleCredentials();
            return SessionsClient.create(SessionsSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SessionsClient", e);
        }
    }
} 