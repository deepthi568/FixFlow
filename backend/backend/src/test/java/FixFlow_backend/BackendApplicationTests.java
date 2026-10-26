package FixFlow_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the Spring application context loads successfully.
 * Uses an embedded H2 database so no MySQL connection is required.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // passes if the context starts without throwing
    }
}
