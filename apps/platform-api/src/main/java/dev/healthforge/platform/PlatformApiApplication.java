package dev.healthforge.platform;

import dev.healthforge.platform.enterprise.AuditPolicyProperties;
import dev.healthforge.platform.integration.IntegrationProperties;
import dev.healthforge.platform.ingestion.MvpSourceProperties;
import dev.healthforge.platform.ingestion.WorkspaceProperties;
import dev.healthforge.platform.model.ModelProperties;
import dev.healthforge.platform.auth.AuthProperties;
import dev.healthforge.platform.ingestion.ArtifactStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({MvpSourceProperties.class, ArtifactStorageProperties.class, WorkspaceProperties.class, ModelProperties.class, AuthProperties.class, AuditPolicyProperties.class, IntegrationProperties.class})
public class PlatformApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
