package com.gymmanager.services;

import com.gymmanager.dao.NotificacionDAO;
import com.gymmanager.dao.NotificacionDAOImpl;
import com.gymmanager.models.Notificacion;
import com.gymmanager.models.Socio;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * Verifica socios con vencimiento en 3 días y les envía WhatsApp.
     * Registra cada intento en la tabla notificaciones.
     * Se llama al iniciar la app y al navegar al Dashboard.
     */
    public void verificarYNotificarVencimientos() {
        if (!ejecutando.compareAndSet(false, true)) {
            System.out.println("[NotificacionService] Verificación ya en curso, omitiendo.");
            return;
        }

        try {
            List<Socio> porVencer = socioService.listarPorVencer(3);
            if (porVencer.isEmpty()) return;

            int enviados = 0, errores = 0;
            StringBuilder resumen = new StringBuilder("📊 *Gen Fit* — Vencimientos en 3 días:\n");

            for (Socio socio : porVencer) {
                ResultadoEnvio resultado = notificarSocio(socio);
                if (resultado == ResultadoEnvio.ENVIADO)  enviados++;
                if (resultado == ResultadoEnvio.ERROR)    errores++;

                resumen.append(String.format("• %s — vence: %s%n",
                        socio.getNombre(), socio.getFechaFin()));
            }

            // Resumen al dueño con totales
            String mensajeDueno = resumen
                    + String.format("\n✅ Notificados: %d  |  ❌ Errores: %d  |  Total: %d",
                    enviados, errores, porVencer.size());
            notificarDueno(mensajeDueno);

        } catch (Exception e) {
            // Captura inesperada: no debe romper el arranque de la app
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

    /** Intenta notificar a un socio y persiste el resultado. */
    private ResultadoEnvio notificarSocio(Socio socio) {
        String mensaje = String.format(
                "🏋️ *Gen Fit* — Hola %s, tu membresía vence el %s. " +
                        "Pásate a renovarla para no perder tu acceso. ¡Te esperamos!",
                socio.getNombre(), socio.getFechaFin());

        Notificacion registro = crearRegistro(socio.getId(), "VENCIMIENTO", mensaje);

        if (socio.getWhatsapp() == null || socio.getWhatsapp().isBlank()) {
            registro.setEstado("SIN_WHATSAPP");
            guardarSilencioso(registro);
            return ResultadoEnvio.SIN_WHATSAPP;
        }

        try {
            whatsApp.enviarAlSocio(socio, mensaje);
            registro.setEstado("ENVIADO");
            guardarSilencioso(registro);
            return ResultadoEnvio.ENVIADO;
        } catch (WhatsAppException e) {
            // Adjunta el error al mensaje para tener contexto en el historial
            registro.setEstado("ERROR");
            registro.setMensaje(mensaje + " [Error: " + e.getMessage() + "]");
            guardarSilencioso(registro);
            System.err.println("[NotificacionService] Fallo al notificar a "
                    + socio.getNombre() + ": " + e.getMessage());
            return ResultadoEnvio.ERROR;
        }
    }

    /** Envía el resumen diario al dueño y lo registra. socio_id = 0 → sistema. */
    private void notificarDueno(String mensaje) {
        Notificacion registro = crearRegistro(0, "RESUMEN_DUENO", mensaje);
        try {
            whatsApp.enviarAlDueno(mensaje);
            registro.setEstado("ENVIADO");
        } catch (WhatsAppException e) {
            registro.setEstado("ERROR");
            System.err.println("[NotificacionService] No se pudo notificar al dueño: " + e.getMessage());
        }
        guardarSilencioso(registro);
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

    private enum ResultadoEnvio { ENVIADO, ERROR, SIN_WHATSAPP }
}