package com.smartpark.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    /**
     * Parse Render's DATABASE_URL (postgres://user:pass@host:port/db)
     * to JDBC URL format (jdbc:postgresql://host:port/db?user=xxx&password=xxx)
     */
    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            System.err.println("WARNING: DATABASE_URL environment variable is not set!");
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }
        
        try {
            System.out.println("Parsing DATABASE_URL: " + databaseUrl.replaceAll(":[^:@]+@", ":****@")); // Hide password in logs
            
            URI dbUri = new URI(databaseUrl);
            
            String[] userInfo = dbUri.getUserInfo().split(":");
            String username = userInfo[0];
            String password = userInfo.length > 1 ? userInfo[1] : "";
            String host = dbUri.getHost();
            int port = dbUri.getPort();
            String database = dbUri.getPath().substring(1); // Remove leading "/"
            
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            
            System.out.println("Connecting to database: " + jdbcUrl);
            
            return DataSourceBuilder.create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
                    
        } catch (URISyntaxException e) {
            System.err.println("ERROR: Invalid DATABASE_URL format: " + databaseUrl);
            throw new IllegalStateException("Invalid DATABASE_URL format: " + databaseUrl, e);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to create DataSource: " + e.getMessage());
            throw new IllegalStateException("Failed to create DataSource", e);
        }
    }
}
