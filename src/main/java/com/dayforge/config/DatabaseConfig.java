package com.dayforge.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    private final Environment env;

    public DatabaseConfig(Environment env) {
        this.env = env;
    }

    /**
     * Primary DataSource bean with Supabase URL normalization, SSL enforcement,
     * and HikariCP connection pool optimization for the Supabase Session Pooler.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        String activeProfile = String.join(",", env.getActiveProfiles());
        log.info("Initializing DataSource for active profiles: [{}]", activeProfile.isEmpty() ? "default/dev" : activeProfile);

        // Normalize URL (converts postgresql:// to jdbc:postgresql:// and ensures sslmode=require)
        String jdbcUrl = normalizeJdbcUrl(rawUrl);

        // Check if credentials are embedded in URL (e.g. postgresql://user:pass@host:port/db)
        ParsedCredentials creds = extractCredentialsIfEmbedded(rawUrl, username, password);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(creds.username);
        config.setPassword(creds.password);
        config.setDriverClassName(driverClassName);

        // HikariCP pool tuning for Supabase Session Pooler
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(300000);         // 5 minutes
        config.setMaxLifetime(900000);         // 15 minutes
        config.setConnectionTimeout(20000);     // 20 seconds
        config.setKeepaliveTime(300000);       // 5 minutes keepalive
        config.setPoolName("DayForgeHikariPool");

        // SSL & TCP parameters for PostgreSQL / Supabase
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "require");
            config.addDataSourceProperty("tcpKeepAlive", "true");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
        }

        log.info("HikariCP DataSource configured successfully for target host: {}", sanitizeUrlForLogging(jdbcUrl));
        return new HikariDataSource(config);
    }

    /**
     * Converts standard URI formats (postgresql:// or postgres://) to JDBC (jdbc:postgresql://)
     * and guarantees sslmode=require for PostgreSQL targets.
     */
    public static String normalizeJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        String normalized = url.trim();

        // Handle raw postgres:// or postgresql:// schemes
        if (normalized.startsWith("postgres://")) {
            normalized = "jdbc:postgresql://" + normalized.substring("postgres://".length());
        } else if (normalized.startsWith("postgresql://")) {
            normalized = "jdbc:postgresql://" + normalized.substring("postgresql://".length());
        }

        // If credentials are in URL (jdbc:postgresql://user:pass@host), remove credentials from JDBC url
        if (normalized.startsWith("jdbc:postgresql://") && normalized.contains("@")) {
            int atIndex = normalized.indexOf('@');
            normalized = "jdbc:postgresql://" + normalized.substring(atIndex + 1);
        }

        // Enforce sslmode=require for PostgreSQL endpoints if not already specified
        if (normalized.startsWith("jdbc:postgresql:") && !normalized.contains("sslmode=")) {
            normalized += (normalized.contains("?") ? "&" : "?") + "sslmode=require";
        }

        return normalized;
    }

    /**
     * Extracts embedded username and password if user provided standard URI with credentials
     */
    private ParsedCredentials extractCredentialsIfEmbedded(String url, String fallbackUser, String fallbackPass) {
        if (url != null && (url.startsWith("postgres://") || url.startsWith("postgresql://") || url.startsWith("jdbc:postgresql://"))) {
            try {
                String cleanUrl = url.startsWith("jdbc:") ? url.substring(5) : url;
                URI uri = URI.create(cleanUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    String user = (parts[0] != null && !parts[0].isBlank()) ? parts[0] : fallbackUser;
                    String pass = (parts.length > 1 && parts[1] != null) ? parts[1] : fallbackPass;
                    return new ParsedCredentials(user, pass);
                }
            } catch (Exception ignored) {
                // Fall back to configured properties
            }
        }
        return new ParsedCredentials(fallbackUser, fallbackPass);
    }

    /**
     * Safe URL logger that strips any sensitive information.
     */
    public static String sanitizeUrlForLogging(String url) {
        if (url == null) return "null";
        // Strip credentials if present
        return url.replaceAll("://.*?:.*?@", "://***:***@");
    }

    private static class ParsedCredentials {
        final String username;
        final String password;

        ParsedCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
