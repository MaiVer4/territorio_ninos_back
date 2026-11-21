package com.software.territoriodeninos.util;

import com.software.territoriodeninos.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FechaParametroUtilTest - Parseo flexible de parámetros fechaInicio/fechaFin")
class FechaParametroUtilTest {

    @Test
    @DisplayName("Debe retornar null cuando el valor es nulo o vacío")
    void debeRetornarNullSiVacioONulo() {
        assertThat(FechaParametroUtil.parseInicio(null, "fechaInicio")).isNull();
        assertThat(FechaParametroUtil.parseInicio("   ", "fechaInicio")).isNull();
        assertThat(FechaParametroUtil.parseFin(null, "fechaFin")).isNull();
        assertThat(FechaParametroUtil.parseFin("", "fechaFin")).isNull();
    }

    @Test
    @DisplayName("Debe convertir fecha YYYY-MM-DD en inicio del día y fin del día")
    void debeConvertirSoloFechaAInicioYFinDeDia() {
        LocalDateTime inicio = FechaParametroUtil.parseInicio("2026-11-21", "fechaInicio");
        LocalDateTime fin = FechaParametroUtil.parseFin("2026-11-21", "fechaFin");

        assertThat(inicio).isEqualTo(LocalDateTime.of(2026, 11, 21, 0, 0, 0));
        assertThat(fin).isEqualTo(LocalDateTime.of(2026, 11, 21, 23, 59, 59, LocalTime.MAX.getNano()));
    }

    @Test
    @DisplayName("Debe aceptar ISO LocalDateTime e ISO OffsetDateTime")
    void debeAceptarIsoLocalDateTimeYOffsetDateTime() {
        LocalDateTime local = FechaParametroUtil.parseInicio("2026-11-21T08:30:00", "fechaInicio");
        LocalDateTime offset = FechaParametroUtil.parseFin("2026-11-21T08:30:00Z", "fechaFin");

        assertThat(local).isEqualTo(LocalDateTime.of(2026, 11, 21, 8, 30, 0));
        assertThat(offset).isEqualTo(LocalDateTime.of(2026, 11, 21, 8, 30, 0));
    }

    @Test
    @DisplayName("Debe lanzar ReglaNegocioException si el formato es inválido")
    void debeLanzarExcepcionConFormatoInvalido() {
        assertThatThrownBy(() -> FechaParametroUtil.parseInicio("fecha-invalida", "fechaInicio"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("fechaInicio")
                .hasMessageContaining("fecha-invalida");

        assertThatThrownBy(() -> FechaParametroUtil.parseFin("2026-99-99", "fechaFin"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("fechaFin");
    }
}
