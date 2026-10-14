package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.UsuarioRegistroDTO;
import com.software.territoriodeninos.dto.UsuarioResponseDTO;
import com.software.territoriodeninos.dto.UsuarioUpdateDTO;
import com.software.territoriodeninos.entity.Rol;
import com.software.territoriodeninos.entity.Usuario;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.mapper.UsuarioMapper;
import com.software.territoriodeninos.repository.RolRepository;
import com.software.territoriodeninos.repository.UsuarioRepository;
import com.software.territoriodeninos.util.EstadoUsuario;
import com.software.territoriodeninos.util.NombreRol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl - Tests unitarios")
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private com.software.territoriodeninos.repository.AsignacionOperarioRepository asignacionRepository;

    @Mock
    private com.software.territoriodeninos.repository.PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Rol mockRol;
    private Usuario mockUsuario;
    private UsuarioResponseDTO mockResponseDTO;

    @BeforeEach
    void setUp() {
        mockRol = new Rol();
        mockRol.setId(1L);
        mockRol.setNombre(NombreRol.TAQUILLERO);

        mockUsuario = new Usuario();
        mockUsuario.setId(1L);
        mockUsuario.setNombre("Juan Pérez");
        mockUsuario.setCorreo("juan@test.com");
        mockUsuario.setPassword("hashedPass");
        mockUsuario.setEstado(EstadoUsuario.ACTIVO);
        mockUsuario.setRol(mockRol);
        mockUsuario.setFechaCreacion(LocalDateTime.now());

        mockResponseDTO = UsuarioResponseDTO.builder()
                .id(1L)
                .nombre("Juan Pérez")
                .correo("juan@test.com")
                .estado("ACTIVO")
                .rol("TAQUILLERO")
                .fechaCreacion(mockUsuario.getFechaCreacion())
                .build();
                
        org.springframework.security.core.Authentication authentication = mock(org.springframework.security.core.Authentication.class);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("juan@test.com");
        java.util.Collection authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        lenient().doReturn(authorities).when(authentication).getAuthorities();
        org.springframework.security.core.context.SecurityContext securityContext = new org.springframework.security.core.context.SecurityContextImpl(authentication);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
    }

    // =========================================================================
    // REGISTRAR
    // =========================================================================
    @Nested
    @DisplayName("Registrar usuario")
    class RegistrarTests {

        @Test
        @DisplayName("Registro exitoso devuelve UsuarioResponseDTO")
        void registrar_Exitoso() {
            UsuarioRegistroDTO dto = UsuarioRegistroDTO.builder()
                    .nombre("Juan Pérez")
                    .correo("juan@test.com")
                    .password("Password123")
                    .nombreRol("TAQUILLERO")
                    .build();

            when(usuarioRepository.existsByCorreo("juan@test.com")).thenReturn(false);
            when(rolRepository.findByNombre(NombreRol.TAQUILLERO)).thenReturn(Optional.of(mockRol));
            when(passwordEncoder.encode("Password123")).thenReturn("hashedPass");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(mockUsuario);
            when(usuarioMapper.toResponseDTO(mockUsuario)).thenReturn(mockResponseDTO);

            UsuarioResponseDTO result = usuarioService.registrar(dto);

            assertNotNull(result);
            assertEquals("Juan Pérez", result.getNombre());
            assertEquals("juan@test.com", result.getCorreo());
            assertEquals("TAQUILLERO", result.getRol());
            verify(usuarioRepository).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Registro con correo duplicado lanza ConflictoException")
        void registrar_CorreoDuplicado() {
            UsuarioRegistroDTO dto = UsuarioRegistroDTO.builder()
                    .nombre("Juan Pérez")
                    .correo("juan@test.com")
                    .password("Password123")
                    .nombreRol("TAQUILLERO")
                    .build();

            when(usuarioRepository.existsByCorreo("juan@test.com")).thenReturn(true);

            ConflictoException ex = assertThrows(
                    ConflictoException.class,
                    () -> usuarioService.registrar(dto)
            );

            assertTrue(ex.getMessage().contains("juan@test.com"));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Registro con rol inválido lanza ConflictoException")
        void registrar_RolInvalido() {
            UsuarioRegistroDTO dto = UsuarioRegistroDTO.builder()
                    .nombre("Juan Pérez")
                    .correo("juan@test.com")
                    .password("Password123")
                    .nombreRol("ROL_INEXISTENTE")
                    .build();

            when(usuarioRepository.existsByCorreo("juan@test.com")).thenReturn(false);

            ConflictoException ex = assertThrows(
                    ConflictoException.class,
                    () -> usuarioService.registrar(dto)
            );

            assertTrue(ex.getMessage().contains("Rol inválido"));
        }

        @Test
        @DisplayName("Registro con rol null lanza ConflictoException")
        void registrar_RolNull() {
            UsuarioRegistroDTO dto = UsuarioRegistroDTO.builder()
                    .nombre("Juan Pérez")
                    .correo("juan@test.com")
                    .password("Password123")
                    .nombreRol(null)
                    .build();

            when(usuarioRepository.existsByCorreo("juan@test.com")).thenReturn(false);

            ConflictoException ex = assertThrows(
                    ConflictoException.class,
                    () -> usuarioService.registrar(dto)
            );

            assertTrue(ex.getMessage().contains("Rol inválido"));
        }
    }

    // =========================================================================
    // OBTENER
    // =========================================================================
    @Nested
    @DisplayName("Obtener usuarios")
    class ObtenerTests {

        @Test
        @DisplayName("Obtener todos devuelve lista de usuarios")
        void obtenerTodos_DevuelveLista() {
            when(usuarioRepository.findAll()).thenReturn(List.of(mockUsuario));
            when(usuarioMapper.toResponseDTO(mockUsuario)).thenReturn(mockResponseDTO);

            List<UsuarioResponseDTO> result = usuarioService.obtenerTodos();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Juan Pérez", result.get(0).getNombre());
        }

        @Test
        @DisplayName("Obtener todos sin usuarios devuelve lista vacía")
        void obtenerTodos_SinUsuarios() {
            when(usuarioRepository.findAll()).thenReturn(List.of());

            List<UsuarioResponseDTO> result = usuarioService.obtenerTodos();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Obtener por ID existente y activo devuelve UsuarioResponseDTO")
        void obtenerPorId_Existente() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(usuarioMapper.toResponseDTO(mockUsuario)).thenReturn(mockResponseDTO);

            UsuarioResponseDTO result = usuarioService.obtenerPorId(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Obtener por ID inexistente lanza RecursoNoEncontradoException")
        void obtenerPorId_NoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> usuarioService.obtenerPorId(99L));
        }

        @Test
        @DisplayName("Obtener por ID de usuario inactivo lanza RecursoNoEncontradoException")
        void obtenerPorId_UsuarioInactivo() {
            mockUsuario.setEstado(EstadoUsuario.INACTIVO);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));

            RecursoNoEncontradoException ex = assertThrows(
                    RecursoNoEncontradoException.class,
                    () -> usuarioService.obtenerPorId(1L)
            );

            assertTrue(ex.getMessage().contains("inactivo"));
        }

        @Test
        @DisplayName("Obtener por correo existente y activo devuelve UsuarioResponseDTO")
        void obtenerPorCorreo_Existente() {
            when(usuarioRepository.findByCorreoAndEstado("juan@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.of(mockUsuario));
            when(usuarioMapper.toResponseDTO(mockUsuario)).thenReturn(mockResponseDTO);

            UsuarioResponseDTO result = usuarioService.obtenerPorCorreo("juan@test.com");

            assertNotNull(result);
            assertEquals("juan@test.com", result.getCorreo());
        }

        @Test
        @DisplayName("Obtener por correo inexistente lanza RecursoNoEncontradoException")
        void obtenerPorCorreo_NoExiste() {
            when(usuarioRepository.findByCorreoAndEstado("noexiste@test.com", EstadoUsuario.ACTIVO))
                    .thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> usuarioService.obtenerPorCorreo("noexiste@test.com"));
        }
    }

    // =========================================================================
    // ACTUALIZAR
    // =========================================================================
    @Nested
    @DisplayName("Actualizar usuario")
    class ActualizarTests {

        @Test
        @DisplayName("Actualizar nombre exitosamente")
        void actualizar_Nombre() {
            UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                    .nombre("Juan Actualizado")
                    .build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(usuarioMapper.toResponseDTO(mockUsuario)).thenReturn(mockResponseDTO);

            UsuarioResponseDTO result = usuarioService.actualizar(1L, dto);

            assertNotNull(result);
            assertEquals("Juan Actualizado", mockUsuario.getNombre());
        }

        @Test
        @DisplayName("Actualizar correo a uno ya en uso lanza ConflictoException")
        void actualizar_CorreoDuplicado() {
            UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                    .correo("otro@test.com")
                    .build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(usuarioRepository.existsByCorreo("otro@test.com")).thenReturn(true);

            assertThrows(ConflictoException.class,
                    () -> usuarioService.actualizar(1L, dto));
        }

        @Test
        @DisplayName("Actualizar correo al mismo valor no lanza excepción")
        void actualizar_MismoCorreo() {
            UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                    .correo("juan@test.com")
                    .build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(usuarioMapper.toResponseDTO(mockUsuario)).thenReturn(mockResponseDTO);

            UsuarioResponseDTO result = usuarioService.actualizar(1L, dto);

            assertNotNull(result);
            verify(usuarioRepository, never()).existsByCorreo(anyString());
        }

        @Test
        @DisplayName("Cambiar contraseña la codifica y actualiza correctamente")
        void cambiarPassword_Exito() {
            com.software.territoriodeninos.dto.ChangePasswordRequest dto = new com.software.territoriodeninos.dto.ChangePasswordRequest();
            dto.setPasswordActual("OldPassword123");
            dto.setNuevaContrasena("NewPassword123");

            mockUsuario.setPassword("oldHashedPass");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(passwordEncoder.matches("OldPassword123", "oldHashedPass")).thenReturn(true);
            when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPass");

            usuarioService.cambiarPassword(1L, dto);

            assertEquals("newHashedPass", mockUsuario.getPassword());
            verify(passwordEncoder).encode("NewPassword123");
        }

        @Test
        @DisplayName("Actualizar con estado inválido lanza ConflictoException")
        void actualizar_EstadoInvalido() {
            UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                    .estado("ESTADO_INVALIDO")
                    .build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));

            assertThrows(ConflictoException.class,
                    () -> usuarioService.actualizar(1L, dto));
        }

        @Test
        @DisplayName("Actualizar usuario inexistente lanza RecursoNoEncontradoException")
        void actualizar_NoExiste() {
            UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                    .nombre("Nuevo Nombre")
                    .build();

            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> usuarioService.actualizar(99L, dto));
        }

        @Test
        @DisplayName("Usuario no admin intentando cambiar rol lanza AccessDeniedException")
        void actualizar_NoAdminIntentandoCambiarRol() {
            UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                    .rol("ADMINISTRADOR")
                    .build();

            org.springframework.security.core.Authentication authNoAdmin = mock(org.springframework.security.core.Authentication.class);
            when(authNoAdmin.isAuthenticated()).thenReturn(true);
            when(authNoAdmin.getName()).thenReturn("juan@test.com");
            doReturn(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TAQUILLERO")))
                    .when(authNoAdmin).getAuthorities();
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authNoAdmin);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> usuarioService.actualizar(1L, dto));
        }

        @Test
        @DisplayName("Usuario no admin intentando cambiar estado lanza AccessDeniedException")
        void actualizar_NoAdminIntentandoCambiarEstado() {
            UsuarioUpdateDTO dto = UsuarioUpdateDTO.builder()
                    .estado("INACTIVO")
                    .build();

            org.springframework.security.core.Authentication authNoAdmin = mock(org.springframework.security.core.Authentication.class);
            when(authNoAdmin.isAuthenticated()).thenReturn(true);
            when(authNoAdmin.getName()).thenReturn("juan@test.com");
            doReturn(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TAQUILLERO")))
                    .when(authNoAdmin).getAuthorities();
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authNoAdmin);

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> usuarioService.actualizar(1L, dto));
        }
    }

    // =========================================================================
    // INACTIVAR
    // =========================================================================
    @Nested
    @DisplayName("Inactivar usuario")
    class InactivarTests {

        @Test
        @DisplayName("Inactivar usuario existente cambia estado a INACTIVO")
        void inactivar_Exitoso() {
            autenticarComo("admin@test.com");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(usuarioMapper.toResponseDTO(mockUsuario)).thenReturn(mockResponseDTO);

            usuarioService.inactivar(1L);

            assertEquals(EstadoUsuario.INACTIVO, mockUsuario.getEstado());
        }

        @Test
        @DisplayName("Inactivar usuario inexistente lanza RecursoNoEncontradoException")
        void inactivar_NoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> usuarioService.inactivar(99L));
        }
    }

    private void autenticarComo(String correo) {
        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        correo, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Usuario crearAdmin() {
        Rol rolAdmin = new Rol();
        rolAdmin.setId(2L);
        rolAdmin.setNombre(NombreRol.ADMINISTRADOR);
        Usuario admin = new Usuario();
        admin.setId(5L);
        admin.setCorreo("otro.admin@test.com");
        admin.setEstado(EstadoUsuario.ACTIVO);
        admin.setRol(rolAdmin);
        return admin;
    }

    @Nested
    @DisplayName("Guardas de integridad (último admin / auto-acción / historial)")
    class GuardasTests {

        @Test
        @DisplayName("Inactivar la propia cuenta lanza ConflictoException")
        void inactivar_PropiaCuenta() {
            autenticarComo("juan@test.com");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));

            assertThrows(ConflictoException.class, () -> usuarioService.inactivar(1L));
            assertEquals(EstadoUsuario.ACTIVO, mockUsuario.getEstado());
        }

        @Test
        @DisplayName("Inactivar al último administrador activo lanza ConflictoException")
        void inactivar_UltimoAdmin() {
            autenticarComo("admin@test.com");
            Usuario admin = crearAdmin();
            when(usuarioRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(usuarioRepository.countByRolNombreAndEstado(NombreRol.ADMINISTRADOR, EstadoUsuario.ACTIVO)).thenReturn(1L);

            assertThrows(ConflictoException.class, () -> usuarioService.inactivar(5L));
        }

        @Test
        @DisplayName("Inactivar un admin cuando hay otros admins activos es permitido")
        void inactivar_AdminConOtrosAdmins() {
            autenticarComo("admin@test.com");
            Usuario admin = crearAdmin();
            when(usuarioRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(usuarioRepository.countByRolNombreAndEstado(NombreRol.ADMINISTRADOR, EstadoUsuario.ACTIVO)).thenReturn(2L);

            usuarioService.inactivar(5L);

            assertEquals(EstadoUsuario.INACTIVO, admin.getEstado());
        }

        @Test
        @DisplayName("Eliminar usuario con historial de asignaciones lanza ConflictoException")
        void eliminar_ConHistorial() {
            autenticarComo("admin@test.com");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(asignacionRepository.existsByUsuarioId(1L)).thenReturn(true);

            assertThrows(ConflictoException.class, () -> usuarioService.eliminar(1L));
            verify(usuarioRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Eliminar usuario sin historial borra tokens y usuario")
        void eliminar_SinHistorial() {
            autenticarComo("admin@test.com");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(mockUsuario));
            when(asignacionRepository.existsByUsuarioId(1L)).thenReturn(false);

            usuarioService.eliminar(1L);

            verify(tokenRepository).deleteByUsuarioId(1L);
            verify(usuarioRepository).delete(mockUsuario);
        }

        @Test
        @DisplayName("Quitar rol ADMINISTRADOR al último admin lanza ConflictoException")
        void actualizar_DegradarUltimoAdmin() {
            autenticarComo("admin@test.com");
            Usuario admin = crearAdmin();
            when(usuarioRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(rolRepository.findByNombre(NombreRol.TAQUILLERO)).thenReturn(Optional.of(mockRol));
            when(usuarioRepository.countByRolNombreAndEstado(NombreRol.ADMINISTRADOR, EstadoUsuario.ACTIVO)).thenReturn(1L);
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
            dto.setRol("TAQUILLERO");

            assertThrows(ConflictoException.class, () -> usuarioService.actualizar(5L, dto));
            assertEquals(NombreRol.ADMINISTRADOR, admin.getRol().getNombre());
        }
    }
}
