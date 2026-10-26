package com.software.territoriodeninos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostgresConstraintInitializer - Tests unitarios")
class PostgresConstraintInitializerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private PostgresConstraintInitializer initializer;

    @Test
    @DisplayName("Cuando la base de datos es PostgreSQL, ejecuta creación de índices condicionales")
    void testRun_Postgres_EjecutaIndices() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        initializer.run(args);

        verify(jdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    @DisplayName("Cuando la base de datos no es PostgreSQL (ej. H2), omite ejecución")
    void testRun_NoPostgres_OmiteEjecucion() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        initializer.run(args);

        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("Cuando ocurre excepción en conexión, no propaga el error (tolerante a fallos)")
    void testRun_SQLException_NoPropaga() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Conexión fallida"));

        initializer.run(args);

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
