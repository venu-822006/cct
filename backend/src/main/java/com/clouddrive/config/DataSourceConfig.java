package com.clouddrive.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:}}")
    private String rawUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:}")
    private String username;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String password;

    @Value("${SPRING_DATASOURCE_DRIVER:}")
    private String driverClass;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (!StringUtils.hasText(rawUrl)) {
            rawUrl = "jdbc:h2:file:./data/clouddrivedb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE";
        }

        HikariConfig config = new HikariConfig();

        // Support Render / Heroku / Supabase postgres:// or postgresql:// URI format
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(rawUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    config.setUsername(parts[0]);
                    config.setPassword(parts[1]);
                } else {
                    if (StringUtils.hasText(username)) config.setUsername(username);
                    if (StringUtils.hasText(password)) config.setPassword(password);
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                config.setJdbcUrl(jdbcUrl);
                config.setDriverClassName("org.postgresql.Driver");
            } catch (Exception e) {
                config.setJdbcUrl(rawUrl);
            }
        } else {
            config.setJdbcUrl(rawUrl);
            if (StringUtils.hasText(username)) {
                config.setUsername(username);
            }
            if (StringUtils.hasText(password)) {
                config.setPassword(password);
            }
            if (StringUtils.hasText(driverClass)) {
                config.setDriverClassName(driverClass);
            } else if (rawUrl.startsWith("jdbc:postgresql:")) {
                config.setDriverClassName("org.postgresql.Driver");
            } else if (rawUrl.startsWith("jdbc:h2:")) {
                config.setDriverClassName("org.h2.Driver");
            } else if (rawUrl.startsWith("jdbc:mysql:")) {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);

        return new HikariDataSource(config);
    }
}
