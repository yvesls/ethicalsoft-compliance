package com.ethicalsoft.ethicalsoft_complience.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Garante que o banco de dados PostgreSQL existe antes que Flyway tente se conectar.
 * Executa via ApplicationListener, antes de qualquer bean ser criado.
 */
@Slf4j
public class DatabaseInitializerConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        String datasourceUrl = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");
        ensureDatabaseExists(datasourceUrl, username, password);
    }

    private void ensureDatabaseExists(String datasourceUrl, String username, String password) {
        try {
            String dbName = extractDatabaseName(datasourceUrl);
            if (dbName == null) {
                log.warn("[db-init] Não foi possível extrair o nome do banco da URL: {}", datasourceUrl);
                return;
            }

            String maintenanceUrl = buildMaintenanceUrl(datasourceUrl);
            log.info("[db-init] Verificando existência do banco '{}' via '{}'", dbName, maintenanceUrl);

            ensureDatabaseInMaintenanceUrl(maintenanceUrl, username, password, dbName);
        } catch (Exception e) {
            log.error("[db-init] Falha ao verificar/criar banco de dados: {}", e.getMessage(), e);
        }
    }

    private void ensureDatabaseInMaintenanceUrl(String maintenanceUrl, String username, String password, String dbName)
            throws Exception {
        try (Connection conn = DriverManager.getConnection(maintenanceUrl, username, password);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'"
            );

            if (!rs.next()) {
                log.info("[db-init] Banco '{}' não encontrado. Criando...", dbName);
                stmt.execute("CREATE DATABASE \"" + dbName + "\"");
                log.info("[db-init] Banco '{}' criado com sucesso.", dbName);
            } else {
                log.info("[db-init] Banco '{}' já existe.", dbName);
            }
        }
    }

    private String extractDatabaseName(String url) {
        if (url == null) return null;
        String clean = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == clean.length() - 1) return null;
        return clean.substring(lastSlash + 1);
    }

    private String buildMaintenanceUrl(String url) {
        String dbName = extractDatabaseName(url);
        if (dbName == null) return url;
        String clean = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String base = clean.substring(0, clean.lastIndexOf('/'));
        return base + "/postgres";
    }
}
