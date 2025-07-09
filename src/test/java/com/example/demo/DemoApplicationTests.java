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

    public static final String REALM_NAME = "demo";

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
    void getAndUpdateUserProfile() {
        Keycloak keycloak = KeycloakBuilder.builder()
            .serverUrl("http://localhost:" + container.getMappedPort(8080))
            .username("admin")
            .password("admin")
            .realm("master")
            .clientId("admin-cli")
            .build();

        // Create a new realm
        RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(REALM_NAME);
        keycloak.realms().create(newRealm);

        // Print current profile
        LOGGER.info("CREATED REALM PROFILE");
        printProfile(keycloak, REALM_NAME);

        // Update profile
        UPConfig upConfig = new UPConfig();
        upConfig.setAttributes(
            List.of(
                new UPAttribute("username"),
                new UPAttribute("email"),
                new UPAttribute("custom")
            )
        );

        LOGGER.info("UPDATE USER PROFILE");
        keycloak.realm(REALM_NAME).users().userProfile().update(upConfig);

        // Print updated profile
        LOGGER.info("CHECK USER PROFILE");
        printProfile(keycloak, REALM_NAME);
    }

    private void printProfile(Keycloak keycloak, String realmName) {
        UserProfileResource userProfileResource = keycloak.realm(realmName).users().userProfile();
        for (UPAttribute attribute : userProfileResource.getConfiguration().getAttributes()) {
            LOGGER.info("attribute: {}", attribute.getName());
        }
    }
}
