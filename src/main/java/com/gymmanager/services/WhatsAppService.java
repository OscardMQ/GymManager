package com.gymmanager.services;

import com.gymmanager.dao.ConfiguracionDAO;
import com.gymmanager.dao.ConfiguracionDAOImpl;
import com.gymmanager.models.Socio;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;

/**
 * Envía mensajes WhatsApp usando la API gratuita de CallMeBot.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  CÓMO OBTENER TU TOKEN DE CALLMEBOT                              │
 * │                                                                  │
 * │  1. Agrega el número +34 644 59 21 91 a tus contactos           │
 * │     de WhatsApp con el nombre "CallMeBot".                       │
 * │                                                                  │
 * │  2. Envía el mensaje:  allow_callmebot                           │
 * │     directamente a ese contacto desde WhatsApp.                  │
 * │                                                                  │
 * │  3. Recibirás una respuesta automática con tu apikey:            │
 * │     "Your ApiKey is abc123xyz"                                   │
 * │                                                                  │
 * │  4. Registra esa apikey en:                                      │
 * │     GymManager → Menú Notificaciones → Configuración            │
 * │                                                                  │
 * │  LÍMITE: 1 mensaje por minuto por número. No reintentar.        │
 * │  URL:    https://api.callmebot.com/whatsapp.php                  │
 * │          ?phone=NUM&text=MSG_URL_ENCODED&apikey=TOKEN            │
 * └──────────────────────────────────────────────────────────────────┘
 */
public class WhatsAppService {

    // ── Constantes ────────────────────────────────────────────────────────────

    private static final String API_URL          = "https://api.callmebot.com/whatsapp.php";
    private static final String CLAVE_TOKEN      = "whatsapp.apikey";
    private static final String CLAVE_NUM_DUENO  = "whatsapp.dueno";
    private static final Duration TIMEOUT        = Duration.ofSeconds(15);

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static WhatsAppService instancia;

    private final ConfiguracionDAO configDAO;
    private final HttpClient       httpClient;

    private WhatsAppService() {
        this.configDAO  = new ConfiguracionDAOImpl();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    public static synchronized WhatsAppService getInstance() {
        if (instancia == null) instancia = new WhatsAppService();
        return instancia;
    }

    // ── API pública de envío ──────────────────────────────────────────────────

    /**
     * Envía un mensaje al número de WhatsApp del socio.
     *
     * @throws WhatsAppException si el socio no tiene número, el token no está
     *                           configurado, o falla la llamada HTTP.
     */
    public void enviarAlSocio(Socio socio, String mensaje) throws WhatsAppException {
        String numero = socio.getWhatsapp();
        if (numero == null || numero.isBlank()) {
            throw new WhatsAppException(
                    "El socio '" + socio.getNombre() + "' no tiene número de WhatsApp registrado.");
        }
        String token = leerToken();
        realizarEnvio(numero.trim(), mensaje, token);
    }

    /**
     * Envía un mensaje al número del dueño del gimnasio.
     *
     * @throws WhatsAppException si no hay número/token configurado, o falla el envío.
     */
    public void enviarAlDueno(String mensaje) throws WhatsAppException {
        String token  = leerToken();
        String numero = leerNumeroDueno();
        realizarEnvio(numero, mensaje, token);
    }

    // ── Configuración (usada por NotificacionesController) ────────────────────

    public Optional<String> obtenerConfiguracion(String clave) {
        try {
            return configDAO.get(clave);
        } catch (SQLException e) {
            System.err.println("[WhatsAppService] Error al leer configuración '" + clave + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Retorna true si la operación tuvo éxito. */
    public boolean guardarConfiguracion(String clave, String valor) {
        try {
            configDAO.set(clave, valor);
            return true;
        } catch (SQLException e) {
            System.err.println("[WhatsAppService] Error al guardar configuración: " + e.getMessage());
            return false;
        }
    }

    // Expone las claves para que el controller no las hardcodee
    public String getClaveToken()       { return CLAVE_TOKEN; }
    public String getClaveNumeroDueno() { return CLAVE_NUM_DUENO; }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private String leerToken() throws WhatsAppException {
        try {
            return configDAO.get(CLAVE_TOKEN)
                    .filter(t -> !t.isBlank())
                    .orElseThrow(() -> new WhatsAppException(
                            "API key de CallMeBot no configurada. " +
                                    "Ve a Notificaciones → Configuración."));
        } catch (SQLException e) {
            throw new WhatsAppException("Error al leer el token de BD.", e);
        }
    }

    private String leerNumeroDueno() throws WhatsAppException {
        try {
            return configDAO.get(CLAVE_NUM_DUENO)
                    .filter(n -> !n.isBlank())
                    .orElseThrow(() -> new WhatsAppException(
                            "Número del dueño no configurado. " +
                                    "Ve a Notificaciones → Configuración."));
        } catch (SQLException e) {
            throw new WhatsAppException("Error al leer el número del dueño de BD.", e);
        }
    }

    /**
     * Construye la URL, dispara el GET y valida la respuesta de CallMeBot.
     * CallMeBot retorna HTTP 200 tanto en éxito como en algunos errores;
     * por eso también se inspecciona el cuerpo.
     */
    private void realizarEnvio(String numero, String mensaje, String token)
            throws WhatsAppException {
        try {
            String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
            String url = String.format("%s?phone=%s&text=%s&apikey=%s",
                    API_URL, numero, mensajeCodificado, token);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new WhatsAppException(
                        "CallMeBot respondió HTTP " + response.statusCode()
                                + ": " + response.body());
            }

            // La API puede devolver 200 con texto de error en el cuerpo
            String cuerpo = response.body().toLowerCase();
            if (cuerpo.contains("error") || cuerpo.contains("blocked")
                    || cuerpo.contains("not registered")) {
                throw new WhatsAppException("CallMeBot rechazó el mensaje: " + response.body());
            }

        } catch (WhatsAppException e) {
            throw e; // re-lanzar sin envolver
        } catch (Exception e) {
            throw new WhatsAppException("Error de red al contactar CallMeBot: " + e.getMessage(), e);
        }
    }
}