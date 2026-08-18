package com.vitalair;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the full Spring application context loads with the required
 * config present. Run with: mvn test
 */
@SpringBootTest
@TestPropertySource(properties = {
        "vitalair.jwt.secret=test-secret-at-least-32-characters-long-ok"
})
class VitalAirApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to start, this test fails - covers wiring
        // mistakes across config/security/service/repository beans.
    }
}
