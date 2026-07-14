package com.gymmanager.services;

import com.gymmanager.dao.MembresiaDAO;
import com.gymmanager.dao.MembresiaDAOImpl;
import com.gymmanager.dao.SocioDAO;
import com.gymmanager.dao.SocioDAOImpl;
import com.gymmanager.models.Membresia;
import com.gymmanager.models.Socio;
import com.gymmanager.utils.FechaUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para socios.
 * Fase 4: alta() y renovar() ahora registran el pago automáticamente.
 */
public class SocioService {

    private static SocioService instancia;
    private final SocioDAO     socioDAO;
    private final MembresiaDAO membresiaDAO; // para consultar precio al registrar pago

    private SocioService() {
        this.socioDAO     = new SocioDAOImpl();
        this.membresiaDAO = new MembresiaDAOImpl();
    }

    public static synchronized SocioService getInstance() {
        if (instancia == null) instancia = new SocioService();
        return instancia;
    }

    /**
     * Da de alta al socio y registra el pago correspondiente.
     * Requiere que SocioDAOImpl.guardar() asigne el ID generado al objeto.
     */
    public void alta(Socio socio) throws IllegalArgumentException, SQLException {
        validar(socio);
        socioDAO.guardar(socio);
        registrarPagoAutomatico(socio.getId(), socio.getTipoMembresia());
    }

    public void actualizar(Socio socio) throws IllegalArgumentException, SQLException {
        validar(socio);
        socioDAO.actualizar(socio);
        // Edición no genera pago; solo alta y renovación lo hacen
    }

    /** Renueva la membresía y registra el pago de inmediato. */
    public void renovar(Socio socio, Membresia nuevaMembresia, LocalDate fechaInicio)
            throws SQLException {
        LocalDate fechaFin = FechaUtils.calcularFechaFin(fechaInicio, nuevaMembresia.getDuracionDias());
        socio.setTipoMembresia(nuevaMembresia.getId());
        socio.setFechaInicio  (fechaInicio.toString());
        socio.setFechaFin     (fechaFin.toString());
        socio.setActivo       (true);
        socioDAO.actualizar(socio);

        PagoService.getInstance().registrarPago(
                socio.getId(), nuevaMembresia.getPrecio(), nuevaMembresia.getId());
    }

    public void darDeBaja(int id) throws SQLException {
        socioDAO.cambiarEstado(id, false);
    }

    public List<Socio> listar() throws SQLException {
        return socioDAO.listar();
    }

    public List<Socio> listarActivos() throws SQLException {
        return socioDAO.listar().stream()
                .filter(s -> s.isActivo() && !FechaUtils.estaVencida(s.getFechaFin()))
                .collect(Collectors.toList());
    }

    public List<Socio> listarVencidos() throws SQLException {
        return socioDAO.listar().stream()
                .filter(s -> s.isActivo() && FechaUtils.estaVencida(s.getFechaFin()))
                .collect(Collectors.toList());
    }

    /** Socios activos cuya membresía vence dentro de los próximos {@code dias} días (inclusive hoy). */
    public List<Socio> listarPorVencer(int dias) throws SQLException {
        LocalDate hoy    = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        return socioDAO.listar().stream()
                .filter(s -> s.isActivo() && s.getFechaFin() != null)
                .filter(s -> {
                    try {
                        LocalDate fin = LocalDate.parse(s.getFechaFin());
                        return !fin.isBefore(hoy) && !fin.isAfter(limite);
                    } catch (java.time.format.DateTimeParseException e) {
                        // Una fecha corrupta no debe tumbar el listado completo
                        System.err.println("[SocioService] Fecha inválida en socio ID "
                                + s.getId() + ": " + s.getFechaFin());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    public List<Socio> buscar(String nombre) throws SQLException {
        return (nombre == null || nombre.isBlank())
                ? socioDAO.listar()
                : socioDAO.buscarPorNombre(nombre);
    }

    /**
     * Consulta el precio de la membresía y delega en PagoService.
     * Si el pago no se puede registrar, lanza SQLException: el fallo debe
     * llegar a la UI (antes se perdía en consola y los reportes de
     * ganancias quedaban incompletos sin que nadie lo notara).
     */
    private void registrarPagoAutomatico(int socioId, int tipoMembresiaId) throws SQLException {
        if (socioId <= 0) {
            throw new SQLException("El socio se guardó pero no se obtuvo su ID; " +
                    "el pago no fue registrado.");
        }
        Membresia m = membresiaDAO.buscarPorId(tipoMembresiaId)
                .orElseThrow(() -> new SQLException(
                        "El socio se guardó, pero la membresía (ID " + tipoMembresiaId +
                                ") no existe; el pago no fue registrado."));
        try {
            PagoService.getInstance().registrarPago(socioId, m.getPrecio(), m.getId());
        } catch (SQLException e) {
            throw new SQLException("El socio se guardó, pero el pago no se pudo registrar. " +
                    "Regístralo renovando su membresía. Detalle: " + e.getMessage(), e);
        }
    }

    private void validar(Socio socio) {
        if (socio.getNombre() == null || socio.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio.");
        if (socio.getTipoMembresia() <= 0)
            throw new IllegalArgumentException("Debes seleccionar una membresía.");
        if (socio.getFechaInicio() == null || socio.getFechaInicio().isBlank())
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
    }
}