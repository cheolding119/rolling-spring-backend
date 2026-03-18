package com.rolling.api.global.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseAdminConfigTest {

    @TempDir
    Path tempDir;

    private final FirebaseAdminConfig firebaseAdminConfig = new FirebaseAdminConfig();

    @AfterEach
    void tearDown() {
        FirebaseApp.getApps().stream()
                .filter(app -> "rolling-api".equals(app.getName()))
                .findFirst()
                .ifPresent(FirebaseApp::delete);
    }

    @Test
    @DisplayName("Firebase 서비스 계정 파일이 있으면 FirebaseApp을 초기화한다")
    void firebaseApp_initializesWithServiceAccountFile() throws Exception {
        Path credentialsPath = writeServiceAccountJson(tempDir.resolve("firebase-service-account.json"));

        FirebaseApp firebaseApp = firebaseAdminConfig.firebaseApp(
                new FirebaseProperties(true, "rolling-test-project", credentialsPath.toString())
        );

        assertThat(firebaseApp.getName()).isEqualTo("rolling-api");
        assertThat(firebaseApp.getOptions().getProjectId()).isEqualTo("rolling-test-project");
        assertThat(firebaseAdminConfig.firebaseMessaging(firebaseApp)).isNotNull();
    }

    @Test
    @DisplayName("동일 이름의 FirebaseApp이 이미 있으면 기존 인스턴스를 재사용한다")
    void firebaseApp_reusesExistingNamedApp() throws Exception {
        Path credentialsPath = writeServiceAccountJson(tempDir.resolve("firebase-service-account-reuse.json"));
        FirebaseProperties firebaseProperties = new FirebaseProperties(true, "rolling-test-project", credentialsPath.toString());

        FirebaseApp firstApp = firebaseAdminConfig.firebaseApp(firebaseProperties);
        FirebaseApp secondApp = firebaseAdminConfig.firebaseApp(firebaseProperties);

        assertThat(secondApp).isSameAs(firstApp);
    }

    private Path writeServiceAccountJson(Path path) throws Exception {
        String privateKeyPem = generatePrivateKeyPem();
        String json = """
                {
                  "type": "service_account",
                  "project_id": "rolling-test-project",
                  "private_key_id": "test-private-key-id",
                  "private_key": "%s",
                  "client_email": "firebase-adminsdk-test@rolling-test-project.iam.gserviceaccount.com",
                  "client_id": "123456789012345678901",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token",
                  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-test%%40rolling-test-project.iam.gserviceaccount.com"
                }
                """.formatted(escapeJson(privateKeyPem));

        Files.writeString(path, json);
        return path;
    }

    private String generatePrivateKeyPem() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(keyPair.getPrivate().getEncoded());

        return "-----BEGIN PRIVATE KEY-----\n"
                + base64
                + "\n-----END PRIVATE KEY-----\n";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
