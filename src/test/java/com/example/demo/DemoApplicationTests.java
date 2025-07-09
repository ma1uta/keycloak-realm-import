package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

@SpringBootTest
@Testcontainers
class DemoApplicationTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoApplicationTests.class);

    public GenericContainer<?> container = new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
        .withExposedPorts(8080)
        .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
        .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
        .withEnv("KC_LOG_LEVEL", "INFO")
        .waitingFor(Wait.forHttp("/"))
        .withStartupTimeout(Duration.ofSeconds(60));

    public ToStringConsumer toStringConsumer = new ToStringConsumer();

    @BeforeEach
    public void before() {
        container.setCommand("start-dev");
        container.start();
        container.followOutput(toStringConsumer);
    }

    @AfterEach
    public void after() {
        container.stop();
        LOGGER.info("================== KEYCLOAK STDOUT ==================");
        LOGGER.info(toStringConsumer.toUtf8String());
    }

    @Test
    void contextLoads() {
        Keycloak keycloak = KeycloakBuilder.builder()
            .serverUrl("http://localhost:" + container.getMappedPort(8080))
            .username("admin")
            .password("admin")
            .realm("master")
            .clientId("admin-cli")
            .build();

        RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm("demo");
        keycloak.realms().create(newRealm);

        LOGGER.info("CREATED REALM PROFILE");
        printProfile(keycloak);

        UPConfig upConfig = new UPConfig();
        upConfig.setAttributes(
            List.of(
                new UPAttribute("username"),
                new UPAttribute("email"),
                new UPAttribute("custom")
            )
        );

        LOGGER.info("UPDATE USER PROFILE");
        keycloak.realm("demo").users().userProfile().update(upConfig);

        LOGGER.info("CHECK USER PROFILE");
        printProfile(keycloak);
    }

    private void printProfile(Keycloak keycloak) {
        UserProfileResource userProfileResource = keycloak.realm("demo").users().userProfile();
        for (UPAttribute attribute : userProfileResource.getConfiguration().getAttributes()) {
            LOGGER.info("attribute: {}", attribute.getName());
        }
    }
}
