package com.software.territoriodeninos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Inicializador de índices únicos condicionales para PostgreSQL.
 * Garantiza a nivel de base de datos que:
 * 1. Un operario solo tenga como máximo 1 asignación activa a la vez.
 * 2. Un cajero/taquillero solo tenga como máximo 1 turno de caja abierto a la vez.
 * Es tolerante a fallos y compatible con otros motores (como H2 para tests).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresConstraintInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName();
            if (productName != null && productName.toLowerCase().contains("postgres")) {
                log.info("Motor PostgreSQL detectado. Inicializando índices únicos parciales de concurrencia...");
                jdbcTemplate.execute(
                        "CREATE UNIQUE INDEX IF NOT EXISTS ux_asignacion_operario_activa " +
                        "ON asignaciones_operario (usuario_id) WHERE estado = 'ACTIVA';"
                );
                jdbcTemplate.execute(
                        "CREATE UNIQUE INDEX IF NOT EXISTS ux_turno_caja_usuario_abierto " +
                        "ON turnos_caja (usuario_id) WHERE estado = 'ABIERTO';"
                );
                log.info("Índices únicos parciales de PostgreSQL inicializados con éxito.");
            } else {
                log.debug("Base de datos '{}' no es PostgreSQL; omitiendo índices parciales.", productName);
            }
        } catch (Exception e) {
            log.warn("No se pudieron inicializar los índices condicionales de PostgreSQL: {}", e.getMessage());
        }
    }
}
