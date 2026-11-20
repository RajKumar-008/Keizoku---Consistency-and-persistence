package com.dayforge;

import com.dayforge.config.DatabaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class SupabaseConnectionTest {

    @Test
    @DisplayName("Supabase URL Normalization: Converts standard URI and enforces SSL")
    void testUrlNormalization() {
        // Standard URI format copied from Supabase dashboard
        String rawSupabaseUri = "postgresql://postgres.myproject:secret@aws-0-ap-south-1.pooler.supabase.com:5432/postgres";
        String normalized = DatabaseConfig.normalizeJdbcUrl(rawSupabaseUri);

        assertTrue(normalized.startsWith("jdbc:postgresql://"), "Must begin with jdbc:postgresql://");
        assertTrue(normalized.contains("sslmode=require"), "Must enforce sslmode=require for cloud security");
        assertFalse(normalized.contains("secret@"), "Embedded credentials must be removed from the JDBC URL");
    }

    @Test
    @DisplayName("Supabase URL Normalization: Preserves existing sslmode if already present")
    void testUrlNormalizationWithExistingSsl() {
        String rawJdbc = "jdbc:postgresql://db.abcdef.supabase.co:5432/postgres?sslmode=require";
        String normalized = DatabaseConfig.normalizeJdbcUrl(rawJdbc);

        assertEquals("jdbc:postgresql://db.abcdef.supabase.co:5432/postgres?sslmode=require", normalized);
    }

    @Test
    @DisplayName("Security: Credential Masking in Database Logging")
    void testCredentialSanitization() {
        String sensitiveUrl = "jdbc:postgresql://postgres.abcdef:MySuperSecret123@aws-0.pooler.supabase.com:5432/postgres";
        String sanitized = DatabaseConfig.sanitizeUrlForLogging(sensitiveUrl);

        assertFalse(sanitized.contains("MySuperSecret123"), "Password must never be logged");
        assertTrue(sanitized.contains("***:***"), "Credentials must be masked with placeholder");
    }

    @Test
    @DisplayName("Live Supabase Connectivity Probe: Executed when environment variables are supplied")
    void testLiveSupabaseConnectivityIfConfigured() {
        String dbUrl = System.getenv("SUPABASE_DB_URL");
        String dbUser = System.getenv("SUPABASE_DB_USERNAME");
        String dbPass = System.getenv("SUPABASE_DB_PASSWORD");

        if (dbUrl == null || dbUrl.isBlank()) {
            System.out.println("[INFO] SUPABASE_DB_URL is not set in environment. Skipping live network probe.");
            return;
        }

        String jdbcUrl = DatabaseConfig.normalizeJdbcUrl(dbUrl);
        assertDoesNotThrow(() -> {
            try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                assertTrue(rs.next(), "Supabase query should return at least 1 record");
                assertEquals(1, rs.getInt(1), "Supabase query should return 1");
                System.out.println("[INFO] Successfully connected to live Supabase PostgreSQL database: " + conn.getMetaData().getDatabaseProductVersion());
            }
        }, "Supabase database connection failed with provided credentials");
    }
}
