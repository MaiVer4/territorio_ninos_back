package com.software.territoriodeninos.util;

import com.software.territoriodeninos.dto.ConfiguracionParqueDTO;
import com.software.territoriodeninos.dto.TicketPrintDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TicketPrintFormatterTest - Pruebas unitarias para formateo ESC/POS de boletas térmicas (HU-21.01, HU-21.02)")
class TicketPrintFormatterTest {

    private TicketPrintFormatter formatter;
    private TicketPrintDataDTO testTicket;
    private ConfiguracionParqueDTO testConfig;
    private UUID testUuid;

    @BeforeEach
    void setUp() {
        formatter = new TicketPrintFormatter();
        testUuid = UUID.fromString("93957dbe-823c-4b57-b5e5-629857541d06");

        testTicket = TicketPrintDataDTO.builder()
                .uuidTicket(testUuid)
                .nombreAtraccion("PISCINA DE PELOTAS")
                .nombreMenor("Sofia Restrepo")
                .tiempoOModalidad("30 min")
                .precioUnitario(new BigDecimal("30000.00"))
                .fechaEmision(LocalDateTime.of(2026, 11, 2, 14, 30, 0))
                .build();

        testConfig = ConfiguracionParqueDTO.builder()
                .id(1L)
                .nombreParque("Territorio de Niños")
                .nombreComercial("Territorio de Niños")
                .razonSocial("Territorio de Niños S.A.S.")
                .nit("901.458.789-2")
                .direccion("Av. Bolívar # 14N-55")
                .ciudad("Armenia, Quindío")
                .telefono("+57 310 456 7890")
                .anchoPapelMm(80)
                .mensajePie("¡Gracias por su visita!")
                .pieTicketLegal("El cuidado de los menores es responsabilidad del acudiente.")
                .build();
    }

    @Test
    @DisplayName("CA-02: Debe inicializar la impresora (ESC @) y centrar los encabezados (ESC a 1)")
    void formatear_debeIncluirInicializacionYCentradoDeEncabezados() {
        String payload = formatter.formatear(testTicket, testConfig);

        assertThat(payload).startsWith(TicketPrintFormatter.ESC_INIT);
        assertThat(payload).contains(TicketPrintFormatter.ESC_ALIGN_CENTER);
        
        int idxCenter = payload.indexOf(TicketPrintFormatter.ESC_ALIGN_CENTER);
        int idxNombre = payload.indexOf("TERRITORIO DE NIÑOS");
        int idxNit = payload.indexOf("NIT: 901.458.789-2");

        assertThat(idxCenter).isLessThan(idxNombre);
        assertThat(idxNombre).isLessThan(idxNit);
    }

    @Test
    @DisplayName("CA-02: Debe finalizar con la secuencia exacta de corte automático de papel GS V 66 0")
    void formatear_debeFinalizarConComandoDeCorteAutomaticoGS_V_66_0() {
        String payload = formatter.formatear(testTicket, testConfig);

        assertThat(payload).endsWith(TicketPrintFormatter.GS_AUTO_CUT);
        // Verificar representación en bytes: 0x1D, 0x56, 0x42, 0x00
        byte[] bytes = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int len = bytes.length;
        assertThat(bytes[len - 4]).isEqualTo((byte) 0x1D);
        assertThat(bytes[len - 3]).isEqualTo((byte) 0x56);
        assertThat(bytes[len - 2]).isEqualTo((byte) 0x42);
        assertThat(bytes[len - 1]).isEqualTo((byte) 0x00);
    }

    @Test
    @DisplayName("CA-01: Debe inyectar correctamente los datos institucionales y operativos del ticket")
    void formatear_debeInyectarDatosInstitucionalesYDelTicket() {
        String payload = formatter.formatear(testTicket, testConfig);

        assertThat(payload).contains("TERRITORIO DE NIÑOS");
        assertThat(payload).contains("Territorio de Niños S.A.S.");
        assertThat(payload).contains("NIT: 901.458.789-2");
        assertThat(payload).contains("Av. Bolívar # 14N-55");
        assertThat(payload).contains("Armenia, Quindío");
        assertThat(payload).contains("PISCINA DE PELOTAS");
        assertThat(payload).contains("Sofia Restrepo");
        assertThat(payload).contains("30 min");
        assertThat(payload).contains("30.000");
        assertThat(payload).contains(testUuid.toString());
        assertThat(payload).contains("¡Gracias por su visita!");
    }

    @Test
    @DisplayName("CA-01: Debe procesar e imprimir íntegro un pie legal de hasta 2000 caracteres")
    void formatear_debeProcesarPieLegalDeHasta2000Caracteres() {
        StringBuilder pieLargo = new StringBuilder();
        while (pieLargo.length() < 2000) {
            pieLargo.append("Condiciones de seguridad de Territorio de Niños. ");
        }
        String texto2000 = pieLargo.substring(0, 2000);
        testConfig.setPieTicketLegal(texto2000);

        String payload = formatter.formatear(testTicket, testConfig);

        assertThat(payload).contains("TÉRMINOS Y CONDICIONES:");
        assertThat(payload).contains("Condiciones de seguridad");
        assertThat(payload).endsWith(TicketPrintFormatter.GS_AUTO_CUT);

        // Extraer el bloque del pie legal y verificar que la cantidad de caracteres es íntegra (2000 caracteres)
        int idxInicio = payload.indexOf("TÉRMINOS Y CONDICIONES:") + "TÉRMINOS Y CONDICIONES:".length();
        int idxFin = payload.lastIndexOf("-".repeat(80));
        if (idxFin == -1) {
            idxFin = payload.indexOf("¡Gracias por su visita!");
        }
        String bloqueLegal = payload.substring(idxInicio, idxFin).replace("\n", "").trim();
        // El texto envuelto por palabras preserva el contenido total
        assertThat(bloqueLegal.length()).isGreaterThanOrEqualTo(1980);
    }

    @Test
    @DisplayName("CA-01: Truncamiento defensivo si el pie legal supera 2000 caracteres")
    void formatear_debeTruncarPieLegalSiSupera2000Caracteres() {
        String texto2500 = "Z".repeat(2500);
        testConfig.setPieTicketLegal(texto2500);

        String payload = formatter.formatear(testTicket, testConfig);

        // Cada línea de 48 columnas debe contener exactamente 48 'Z'
        assertThat(payload).contains("Z".repeat(48));
        // El conteo total de caracteres 'Z' en el payload debe estar limitado estrictamente a 2000
        long countZ = payload.chars().filter(ch -> ch == 'Z').count();
        assertThat(countZ).isEqualTo(2000);
    }

    @Test
    @DisplayName("Adaptación del ancho de papel: 58mm (32 cols) vs 80mm (48 cols)")
    void formatear_debeAdaptarAnchoColumnasPara58mmY80mm() {
        testConfig.setAnchoPapelMm(58);
        String payload58 = formatter.formatear(testTicket, testConfig);
        assertThat(payload58).contains("=".repeat(32));
        assertThat(payload58).doesNotContain("=".repeat(48));

        testConfig.setAnchoPapelMm(80);
        String payload80 = formatter.formatear(testTicket, testConfig);
        assertThat(payload80).contains("=".repeat(48));
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException si el ticket es nulo")
    void formatear_lanzarExcepcionSiTicketEsNulo() {
        assertThatThrownBy(() -> formatter.formatear(null, testConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pueden ser nulos");
    }
}
