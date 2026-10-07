package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.AsignacionRequestDTO;
import com.software.territoriodeninos.dto.AsignacionResponseDTO;
import com.software.territoriodeninos.dto.ReasignacionRequestDTO;
import com.software.territoriodeninos.entity.Atraccion;
import com.software.territoriodeninos.entity.AsignacionOperario;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.repository.AtraccionRepository;
import com.software.territoriodeninos.repository.AsignacionOperarioRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.util.EstadoAsignacion;
import com.software.territoriodeninos.util.EstadoAtraccion;
import com.software.territoriodeninos.util.EstadoUsuario;
import com.software.territoriodeninos.util.NombreRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsignacionOperarioServiceTest {

    @Mock
    private AsignacionOperarioRepository asignacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AtraccionRepository atraccionRepository;

    @InjectMocks
    private AsignacionOperarioServiceImpl asignacionService;

    private Usuario operarioMock;
    private Usuario adminMock;
    private Atraccion atraccionMock1;
    private Atraccion atraccionMock2;
    private AsignacionOperario asignacionActivaMock;
    private AsignacionRequestDTO requestAsignar;

    @BeforeEach
    void setUp() {
        Rol rolOperario = Rol.builder().id(3L).nombre(NombreRol.OPERARIO).build();
        Rol rolAdmin = Rol.builder().id(1L).nombre(NombreRol.ADMINISTRADOR).build();

        operarioMock = Usuario.builder()
                .id(5L)
                .nombre("Carlos Operario")
                .correo("carlos@parque.com")
                .estado(EstadoUsuario.ACTIVO)
                .rol(rolOperario)
                .build();

        adminMock = Usuario.builder()
                .id(1L)
                .nombre("Admin")
                .correo("admin@parque.com")
                .estado(EstadoUsuario.ACTIVO)
                .rol(rolAdmin)
                .build();

        atraccionMock1 = Atraccion.builder()
                .id(1L)
                .nombre("Montaña Rusa")
                .tarifaMinuto(new BigDecimal("100"))
                .estado(EstadoAtraccion.ACTIVO)
                .build();

        atraccionMock2 = Atraccion.builder()
                .id(2L)
                .nombre("Carros Chocones")
                .tarifaMinuto(new BigDecimal("80"))
                .estado(EstadoAtraccion.ACTIVO)
                .build();

        asignacionActivaMock = AsignacionOperario.builder()
                .id(100L)
                .usuario(operarioMock)
                .atraccion(atraccionMock1)
                .fechaAsignacion(LocalDate.now())
                .horaInicio(LocalDateTime.now())
                .estado(EstadoAsignacion.ACTIVA)
                .observaciones("Turno 1")
                .build();

        requestAsignar = new AsignacionRequestDTO(5L, 1L, "Turno mañana");
    }

    @Test
    @DisplayName("HU-10 Escenario 1: Asignar operario disponible exitosamente (201)")
    void asignar_Exito() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operarioMock));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock1));
        when(asignacionRepository.existsByUsuarioIdAndEstado(5L, EstadoAsignacion.ACTIVA)).thenReturn(false);
        when(asignacionRepository.save(any(AsignacionOperario.class))).thenReturn(asignacionActivaMock);

        AsignacionResponseDTO response = asignacionService.asignar(requestAsignar);

        assertNotNull(response);
        assertEquals(5L, response.usuarioId());
        assertEquals("Carlos Operario", response.nombreOperario());
        assertEquals("Montaña Rusa", response.nombreAtraccion());
        assertEquals(EstadoAsignacion.ACTIVA, response.estado());
        verify(asignacionRepository).save(any(AsignacionOperario.class));
    }

    @Test
    @DisplayName("HU-10 Escenario 2: Conflicto 409 si el operario ya tiene un turno activo")
    void asignar_ConflictoDobleTurnoActivo() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operarioMock));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock1));
        when(asignacionRepository.existsByUsuarioIdAndEstado(5L, EstadoAsignacion.ACTIVA)).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class, () -> asignacionService.asignar(requestAsignar));
        assertTrue(ex.getMessage().contains("ya cuenta con una asignación activa"));
        verify(asignacionRepository, never()).save(any(AsignacionOperario.class));
    }

    @Test
    @DisplayName("HU-10: Rechazar asignación si el usuario no tiene rol OPERARIO")
    void asignar_RolInvalido() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminMock));
        AsignacionRequestDTO requestInvalido = new AsignacionRequestDTO(1L, 1L, "Intento asignar admin");

        ConflictoException ex = assertThrows(ConflictoException.class, () -> asignacionService.asignar(requestInvalido));
        assertTrue(ex.getMessage().contains("no tiene el rol de OPERARIO"));
    }

    @Test
    @DisplayName("HU-10: Rechazar asignación si el operario está inactivo")
    void asignar_OperarioInactivo() {
        operarioMock.setEstado(EstadoUsuario.INACTIVO);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operarioMock));

        ConflictoException ex = assertThrows(ConflictoException.class, () -> asignacionService.asignar(requestAsignar));
        assertTrue(ex.getMessage().contains("inactivo"));
    }

    @Test
    @DisplayName("HU-10: Rechazar asignación si la atracción no está activa")
    void asignar_AtraccionInactiva() {
        atraccionMock1.setEstado(EstadoAtraccion.MANTENIMIENTO);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operarioMock));
        when(atraccionRepository.findById(1L)).thenReturn(Optional.of(atraccionMock1));

        ConflictoException ex = assertThrows(ConflictoException.class, () -> asignacionService.asignar(requestAsignar));
        assertTrue(ex.getMessage().contains("no se encuentra activa"));
    }

    @Test
    @DisplayName("HU-10 Escenario 3: Reasignación de operario (cierra anterior y crea nueva)")
    void reasignar_Exito() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operarioMock));
        when(atraccionRepository.findById(2L)).thenReturn(Optional.of(atraccionMock2));
        when(asignacionRepository.findByUsuarioIdAndEstado(5L, EstadoAsignacion.ACTIVA)).thenReturn(Optional.of(asignacionActivaMock));

        AsignacionOperario nuevaMock = AsignacionOperario.builder()
                .id(101L)
                .usuario(operarioMock)
                .atraccion(atraccionMock2)
                .fechaAsignacion(LocalDate.now())
                .horaInicio(LocalDateTime.now())
                .estado(EstadoAsignacion.ACTIVA)
                .build();

        when(asignacionRepository.save(any(AsignacionOperario.class))).thenReturn(nuevaMock);

        ReasignacionRequestDTO requestReasignar = new ReasignacionRequestDTO(5L, 2L, "Afluencia alta en pista");
        AsignacionResponseDTO response = asignacionService.reasignar(requestReasignar);

        assertNotNull(response);
        assertEquals(EstadoAsignacion.REASIGNADA, asignacionActivaMock.getEstado());
        assertNotNull(asignacionActivaMock.getHoraFin());
        assertEquals("Carros Chocones", response.nombreAtraccion());
    }

    @Test
    @DisplayName("HU-10 Escenario 4: Finalizar turno de operario")
    void finalizar_Exito() {
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacionActivaMock));

        AsignacionResponseDTO response = asignacionService.finalizar(100L);

        assertNotNull(response);
        assertEquals(EstadoAsignacion.FINALIZADA, response.estado());
        assertNotNull(asignacionActivaMock.getHoraFin());
    }

    @Test
    @DisplayName("HU-10: Error al finalizar asignación que ya no está activa")
    void finalizar_YaFinalizada() {
        asignacionActivaMock.setEstado(EstadoAsignacion.FINALIZADA);
        when(asignacionRepository.findById(100L)).thenReturn(Optional.of(asignacionActivaMock));

        assertThrows(ConflictoException.class, () -> asignacionService.finalizar(100L));
    }

    @Test
    @DisplayName("HU-10 Escenario 5: Consultar asignación activa por operario (App Móvil)")
    void obtenerActivaPorOperario_Exito() {
        when(usuarioRepository.existsById(5L)).thenReturn(true);
        when(asignacionRepository.findByUsuarioIdAndEstado(5L, EstadoAsignacion.ACTIVA)).thenReturn(Optional.of(asignacionActivaMock));

        AsignacionResponseDTO response = asignacionService.obtenerActivaPorOperario(5L);

        assertNotNull(response);
        assertEquals("Carlos Operario", response.nombreOperario());
        assertEquals(EstadoAsignacion.ACTIVA, response.estado());
    }

    @Test
    @DisplayName("HU-10: Operario sin asignación activa lanza 404")
    void obtenerActivaPorOperario_NoTieneAsignacion() {
        when(usuarioRepository.existsById(5L)).thenReturn(true);
        when(asignacionRepository.findByUsuarioIdAndEstado(5L, EstadoAsignacion.ACTIVA)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> asignacionService.obtenerActivaPorOperario(5L));
    }
}
