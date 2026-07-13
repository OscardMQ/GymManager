package com.gymmanager.services;

import com.gymmanager.dao.NotificacionDAO;
import com.gymmanager.dao.NotificacionDAOImpl;
import com.gymmanager.models.Notificacion;
import com.gymmanager.models.Socio;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Orquesta la verificación de vencimientos y el envío de notificaciones WhatsApp.
 * Nunca lanza excepción al exterior: todos los fallos quedan en stderr y en BD.
 */
public class NotificacionService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static NotificacionService instancia;

    private final NotificacionDAO  notificacionDAO;
    private final SocioService     socioService;
    private final WhatsAppService  whatsApp;

    /** Evita ejecuciones simultáneas si se llama desde init() y desde el dashboard. */
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    private NotificacionService() {
        this.notificacionDAO = new NotificacionDAOImpl();
        this.socioService    = SocioService.getInstance();
        this.whatsApp        = WhatsAppService.getInstance();
    }

    public static synchronized NotificacionService getInstance() {
        if (instancia == null) instancia = new NotificacionService();
        return instancia;
    }

    // ── Punto de entrada principal ────────────────────────────────────────────

    /**
     * Verifica socios con vencimiento próximo (3 días) y ya vencidos.
     * Si un socio ya fue notificado hoy, se omite para no spam.
     * Envía un resumen al dueño por WhatsApp.
     */
    public void verificarYNotificarVencimientos() {
        if (!ejecutando.compareAndSet(false, true)) {
            System.out.println("[NotificacionService] Verificación ya en curso, omitiendo.");
            return;
        }

        try {
            // IDs de socios que ya recibieron notificación hoy → no volver a incluirlos
            Set<Integer> yaNotificadosHoy = obtenerYaNotificadosHoy();

            List<Socio> porVencer = socioService.listarPorVencer(3).stream()
                    .filter(s -> !yaNotificadosHoy.contains(s.getId()))
                    .collect(Collectors.toList());

            List<Socio> vencidos = socioService.listarVencidos().stream()
                    .filter(s -> !yaNotificadosHoy.contains(s.getId()))
                    .collect(Collectors.toList());

            if (porVencer.isEmpty() && vencidos.isEmpty()) {
                System.out.println("[NotificacionService] Nada nuevo que notificar hoy.");
                return;
            }

            StringBuilder resumen = new StringBuilder("📊 *Gen Fit* — Reporte de membresías:\n");

            if (!porVencer.isEmpty()) {
                resumen.append("\n⏰ *Por vencer (próximos 3 días):*\n");
                for (Socio socio : porVencer) {
                    resumen.append(String.format("• %s — vence: %s%n",
                            socio.getNombre(), socio.getFechaFin()));
                }
            }

            if (!vencidos.isEmpty()) {
                resumen.append("\n🔴 *Ya vencidos:*\n");
                for (Socio socio : vencidos) {
                    resumen.append(String.format("• %s — venció: %s%n",
                            socio.getNombre(), socio.getFechaFin()));
                }
            }

            // Primero se envía el resumen al dueño; las filas por socio heredan
            // el estado REAL del envío (antes decían ENVIADO aunque no saliera nada)
            String estadoEnvio = notificarDueno(resumen.toString());

            for (Socio socio : porVencer) registrarSocio(socio, "VENCIMIENTO", estadoEnvio);
            for (Socio socio : vencidos)  registrarSocio(socio, "VENCIDO",     estadoEnvio);

        } catch (Exception e) {
            System.err.println("[NotificacionService] Error inesperado: " + e.getMessage());
        } finally {
            ejecutando.set(false);
        }
    }

    /** Retorna la lista de notificaciones recientes para la vista historial. */
    public List<Notificacion> obtenerRecientes(int dias) throws SQLException {
        return notificacionDAO.listarRecientes(dias);
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    /**
     * Devuelve los IDs de socios que ya fueron reportados con éxito hoy.
     * Solo cuentan las filas ENVIADO: si el resumen al dueño falló,
     * la siguiente verificación vuelve a incluir a esos socios.
     */
    private Set<Integer> obtenerYaNotificadosHoy() {
        String hoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        try {
            return notificacionDAO.listarRecientes(1).stream()
                    .filter(n -> n.getSocioId() > 0)               // excluir resúmenes del dueño
                    .filter(n -> "ENVIADO".equals(n.getEstado()))  // fallidos se reintentan
                    .filter(n -> n.getFechaEnvio() != null
                            && n.getFechaEnvio().startsWith(hoy)) // solo notificaciones de hoy
                    .map(Notificacion::getSocioId)
                    .collect(Collectors.toSet());
        } catch (SQLException e) {
            System.err.println("[NotificacionService] Error al leer notificaciones de hoy: " + e.getMessage());
            return Set.of(); // si falla, proceder sin filtrar
        }
    }

    /**
     * Deja constancia de que el socio fue incluido en el resumen al dueño.
     * A los socios nunca se les envía WhatsApp (decisión de negocio);
     * el estado refleja si el resumen al dueño salió o no.
     */
    private void registrarSocio(Socio socio, String tipo, String estado) {
        String mensaje = String.format(
                "%s — membresía vence el %s. Incluido en el resumen al dueño.",
                socio.getNombre(), socio.getFechaFin());
        Notificacion registro = crearRegistro(socio.getId(), tipo, mensaje);
        registro.setEstado(estado);
        guardarSilencioso(registro);
    }

    /**
     * Envía el resumen al dueño y lo registra. socio_id = 0 → sistema.
     * @return el estado resultante: "ENVIADO" o "ERROR"
     */
    private String notificarDueno(String mensaje) {
        Notificacion registro = crearRegistro(0, "RESUMEN_DUENO", mensaje);
        try {
            whatsApp.enviarAlDueno(mensaje);
            registro.setEstado("ENVIADO");
        } catch (WhatsAppException e) {
            registro.setEstado("ERROR");
            registro.setMensaje(mensaje + " [Error: " + e.getMessage() + "]");
            System.err.println("[NotificacionService] No se pudo notificar al dueño: " + e.getMessage());
        }
        guardarSilencioso(registro);
        return registro.getEstado();
    }

    private Notificacion crearRegistro(int socioId, String tipo, String mensaje) {
        Notificacion n = new Notificacion();
        n.setSocioId(socioId);
        n.setTipo(tipo);
        n.setMensaje(mensaje);
        n.setFechaEnvio(LocalDateTime.now().format(FMT));
        return n;
    }

    private void guardarSilencioso(Notificacion n) {
        try {
            notificacionDAO.guardar(n);
        } catch (SQLException e) {
            System.err.println("[NotificacionService] Error al guardar en BD: " + e.getMessage());
        }
    }
}