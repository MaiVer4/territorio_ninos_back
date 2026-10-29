package com.software.territoriodeninos.service;

import com.software.territoriodeninos.dto.*;
import com.software.territoriodeninos.entity.*;
import com.software.territoriodeninos.exception.ConflictoException;
import com.software.territoriodeninos.exception.RecursoNoEncontradoException;
import com.software.territoriodeninos.exception.ReglaNegocioException;
import com.software.territoriodeninos.repository.*;
import com.software.territoriodeninos.util.EstadoTurnoCaja;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import com.software.territoriodeninos.util.NombreRol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final ClienteRepository clienteRepository;
    private final MenorRepository menorRepository;
    private final AtraccionRepository atraccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaCalculoService ventaCalculoService;
    private final AtraccionService atraccionService;

    @Override
    @Transactional
    public VentaResponseDTO procesarVenta(VentaProcesarRequestDTO request, Authentication auth) {
        if (request == null) {
            throw new ReglaNegocioException("La solicitud de venta no puede ser nula");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ReglaNegocioException("La venta debe incluir al menos un ítem detallado");
        }

        Usuario cajero = obtenerUsuarioAutenticado(auth);

        // 1. Resolver y validar turno de caja
        TurnoCaja turnoCaja = resolverTurnoCaja(request.turnoCajaId(), cajero);

        // 2. Validar cliente titular
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente titular no encontrado con ID: " + request.clienteId()));

        // 3. Validar atracciones, menores y preparar ítems para cotización
        List<ItemCotizacionDTO> itemsCotizacion = new ArrayList<>();
        List<DetalleVenta> detallesParaPersistir = new ArrayList<>();

        for (DetalleVentaRequestDTO itemDTO : request.items()) {
            if (itemDTO == null) {
                throw new ReglaNegocioException("Los ítems de la venta no pueden ser nulos");
            }
            Atraccion atraccion = atraccionRepository.findById(itemDTO.atraccionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Atracción no encontrada con ID: " + itemDTO.atraccionId()));

            Menor menor = null;
            if (itemDTO.menorId() != null) {
                menor = menorRepository.findById(itemDTO.menorId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Menor no encontrado con ID: " + itemDTO.menorId()));
                if (menor.getCliente() == null || !cliente.getId().equals(menor.getCliente().getId())) {
                    throw new ReglaNegocioException("El menor '" + menor.getNombreCompleto() +
                            "' (ID " + menor.getId() + ") no se encuentra vinculado al cliente titular ID " + cliente.getId());
                }
            }

            BigDecimal precioUnitario = itemDTO.precioUnitario().setScale(2, RoundingMode.HALF_UP);
            int cantidad = (itemDTO.cantidad() != null && itemDTO.cantidad() > 0) ? itemDTO.cantidad() : 1;
            BigDecimal subtotalCalculado = precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);

            itemsCotizacion.add(new ItemCotizacionDTO(
                    itemDTO.atraccionId(),
                    itemDTO.tarifaTiempoId(),
                    itemDTO.tarifaEventoId(),
                    atraccion.getNombre(),
                    cantidad,
                    precioUnitario,
                    subtotalCalculado
            ));

            DetalleVenta detalle = DetalleVenta.builder()
                    .atraccion(atraccion)
                    .menor(menor)
                    .tipoTarifa(itemDTO.tipoTarifa())
                    .tarifaTiempoId(itemDTO.tarifaTiempoId())
                    .tarifaEventoId(itemDTO.tarifaEventoId())
                    .cantidad(cantidad)
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotalCalculado)
                    .build();

            detallesParaPersistir.add(detalle);
        }

        // 4. Cotización oficial y validaciones estrictas (aforo y anti-manipulación)
        BigDecimal descuento = request.descuento() != null ? request.descuento() : BigDecimal.ZERO;
        CotizacionResponseDTO cotizacion = ventaCalculoService.cotizarVenta(
                new CotizacionRequestDTO(itemsCotizacion, null, descuento)
        );

        BigDecimal totalNeto = cotizacion.totalNeto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoRecibido = request.montoRecibido().setScale(2, RoundingMode.HALF_UP);
        BigDecimal cambio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 5. Validación de pago según método
        boolean esEfectivo = MetodoPago.EFECTIVO.equals(request.metodoPago());
        if (esEfectivo) {
            if (montoRecibido.compareTo(totalNeto) < 0) {
                throw new ReglaNegocioException("Monto en efectivo recibido ($" + montoRecibido +
                        ") insuficiente para cubrir el total de la venta ($" + totalNeto + ")");
            }
            cambio = montoRecibido.subtract(totalNeto).setScale(2, RoundingMode.HALF_UP);

            // Incrementar ventas de efectivo en el turno de caja
            BigDecimal ventasPrevias = turnoCaja.getVentasEfectivoSistema() != null
                    ? turnoCaja.getVentasEfectivoSistema()
                    : BigDecimal.ZERO;
            turnoCaja.setVentasEfectivoSistema(ventasPrevias.add(totalNeto));
            turnoCajaRepository.save(turnoCaja);
        } else {
            if (montoRecibido.compareTo(totalNeto) < 0) {
                throw new ReglaNegocioException("El monto procesado en " + request.metodoPago() +
                        " ($" + montoRecibido + ") debe cubrir el total de la venta ($" + totalNeto + ")");
            }
            cambio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // 6. Construir y guardar entidad Venta con sus detalles
        Venta venta = Venta.builder()
                .cliente(cliente)
                .turnoCaja(turnoCaja)
                .montoTotal(totalNeto)
                .subtotal(cotizacion.subtotal())
                .descuento(cotizacion.descuento())
                .impuestos(cotizacion.impuestos())
                .montoRecibido(montoRecibido)
                .cambio(cambio)
                .metodoPago(request.metodoPago())
                .estadoVenta(EstadoVenta.COMPLETADA)
                .observaciones(request.observaciones())
                .build();

        for (DetalleVenta detalle : detallesParaPersistir) {
            venta.addDetalle(detalle);
        }

        Venta guardada = ventaRepository.save(venta);
        log.info("Venta procesada exitosamente con ID {} por un total de ${} (Cajero: {}, Turno: #{})",
                guardada.getId(), guardada.getMontoTotal(), cajero.getNombre(), turnoCaja.getId());

        // 7. Invalidar caché del catálogo de ventas para actualizar aforo
        atraccionService.invalidarCacheCatalogo();

        return VentaResponseDTO.fromEntity(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerVentaPorId(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ID de la venta es obligatorio");
        }
        return ventaRepository.findById(id)
                .map(VentaResponseDTO::fromEntity)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listarVentasPorTurno(Long turnoId, Authentication auth) {
        if (turnoId == null) {
            throw new ReglaNegocioException("El ID del turno de caja es obligatorio");
        }

        Usuario usuario = obtenerUsuarioAutenticado(auth);
        TurnoCaja turno = turnoCajaRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja no encontrado con ID: " + turnoId));

        NombreRol rol = usuario.getRol() != null ? usuario.getRol().getNombre() : null;
        if (rol == NombreRol.TAQUILLERO && !turno.getUsuarioId().equals(usuario.getId())) {
            throw new AccessDeniedException("No tiene permisos para ver las ventas de otro cajero.");
        }

        return ventaRepository.findByTurnoCajaIdOrderByFechaVentaDesc(turnoId).stream()
                .map(VentaResponseDTO::fromEntity)
                .toList();
    }

    private TurnoCaja resolverTurnoCaja(Long turnoCajaId, Usuario cajero) {
        if (turnoCajaId != null) {
            TurnoCaja turno = turnoCajaRepository.findById(turnoCajaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Turno de caja no encontrado con ID: " + turnoCajaId));

            if (!EstadoTurnoCaja.ABIERTO.equals(turno.getEstado())) {
                throw new ConflictoException("El turno de caja especificado (ID " + turnoCajaId + ") no se encuentra en estado ABIERTO.");
            }

            NombreRol rol = cajero.getRol() != null ? cajero.getRol().getNombre() : null;
            if (rol == NombreRol.TAQUILLERO && !turno.getUsuarioId().equals(cajero.getId())) {
                throw new AccessDeniedException("No puede registrar ventas en el turno de caja de otro usuario.");
            }
            return turno;
        }

        return turnoCajaRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(cajero.getId(), EstadoTurnoCaja.ABIERTO)
                .orElseThrow(() -> new ConflictoException("No existe un turno de caja ABIERTO para el cajero en sesión. Realice la apertura de caja antes de registrar ventas."));
    }

    private Usuario obtenerUsuarioAutenticado(Authentication auth) {
        if (auth == null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        String correo = auth.getName();
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado con correo: " + correo));
    }
}
