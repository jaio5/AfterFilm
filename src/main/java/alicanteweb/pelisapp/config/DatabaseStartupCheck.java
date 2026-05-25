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
                statement.executeUpdate("ALTER TABLE review ALTER COLUMN stars TYPE NUMERIC(2,1) USING stars::numeric");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS review_reply (
                            id BIGSERIAL PRIMARY KEY,
                            review_id BIGINT NOT NULL REFERENCES review(id) ON DELETE CASCADE,
                            user_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                            text VARCHAR(1000) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NULL
                        )
                        """);
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_review_reply_review ON review_reply(review_id)");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_review_reply_user ON review_reply(user_id)");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_review_reply_created ON review_reply(created_at)");
            } else {
                try {
                    statement.executeUpdate("ALTER TABLE usuario ADD COLUMN profile_image_path VARCHAR(1000) NULL");
                } catch (Exception ignored) {
                    // Column already exists, or this schema is managed externally.
                }
                try {
                    statement.executeUpdate("ALTER TABLE review MODIFY COLUMN stars DECIMAL(2,1) NOT NULL");
                } catch (Exception ignored) {
                    // Column is already decimal, or this schema is managed externally.
                }
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS review_reply (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            review_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            text VARCHAR(1000) NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NULL,
                            INDEX idx_review_reply_review (review_id),
                            INDEX idx_review_reply_user (user_id),
                            INDEX idx_review_reply_created (created_at),
                            CONSTRAINT fk_review_reply_review FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE,
                            CONSTRAINT fk_review_reply_user FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE
                        )
                        """);
            }
            log.info("Database lightweight migrations applied");
        } catch (Exception e) {
            log.warn("Could not apply lightweight database migrations: {}", e.getMessage());
        }
    }
}
