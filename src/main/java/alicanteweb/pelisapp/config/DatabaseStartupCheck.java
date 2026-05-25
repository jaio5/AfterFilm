package alicanteweb.pelisapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DatabaseStartupCheck {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CommandLineRunner checkDataSource(DataSource dataSource) {
        return args -> {
            try (Connection c = dataSource.getConnection()) {
                applyLightweightMigrations(c);
                boolean valid = c.isValid(2);
                log.info("Database connection valid={} (catalog={})", valid, c.getCatalog());
            } catch (Exception e) {
                log.error("Failed to obtain a database connection at startup: {}", e, e);
                // don't rethrow: let the app continue to start if configured to do so in dev-mode
            }
        };
    }

    private void applyLightweightMigrations(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            String database = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (database.contains("postgres")) {
                statement.executeUpdate("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS profile_image_path VARCHAR(1000)");
            } else {
                try {
                    statement.executeUpdate("ALTER TABLE usuario ADD COLUMN profile_image_path VARCHAR(1000) NULL");
                } catch (Exception ignored) {
                    // Column already exists, or this schema is managed externally.
                }
            }
            log.info("Database lightweight migrations applied");
        } catch (Exception e) {
            log.warn("Could not apply lightweight database migrations: {}", e.getMessage());
        }
    }
}
