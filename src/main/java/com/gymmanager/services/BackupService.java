package com.gymmanager.services;

import com.gymmanager.dao.ConfiguracionDAO;
import com.gymmanager.dao.ConfiguracionDAOImpl;
import com.gymmanager.database.DatabaseConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Respaldos de la base de datos.
 *
 * Estrategia (acordada jul 2026):
 *  - Al arrancar la app se crea un respaldo diario en ~/.gymmanager/backups
 *    (deduplicado por fecha: si el de hoy ya existe, no se repite).
 *  - VACUUM INTO produce una copia íntegra y compacta, segura con WAL.
 *  - Se escribe primero a un .tmp y se renombra: un cierre a media escritura
 *    nunca deja un respaldo corrupto con nombre válido.
 *  - Si hay carpeta secundaria configurada (p. ej. una carpeta sincronizada
 *    con Google Drive u OneDrive), el respaldo también se copia ahí.
 *  - Retención: se conservan los últimos 30 respaldos locales.
 */
public class BackupService {

    private static final String CLAVE_CARPETA_SECUNDARIA = "backup.carpeta_secundaria";
    private static final int    RETENCION               = 30;

    private static BackupService instancia;

    private final ConfiguracionDAO configDAO;

    private BackupService() {
        this.configDAO = new ConfiguracionDAOImpl();
    }

    public static synchronized BackupService getInstance() {
        if (instancia == null) instancia = new BackupService();
        return instancia;
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Respaldo diario al arrancar. Si el de hoy ya existe, no hace nada.
     * Nunca lanza: un fallo de respaldo no debe impedir abrir la app.
     */
    public void respaldarSiTocaHoy() {
        try {
            Path destino = rutaRespaldoHoy();
            if (Files.exists(destino)) return;
            crearRespaldoLocal(destino);
            try {
                copiarACarpetaSecundaria(destino);
            } catch (IOException e) {
                // El respaldo local ya está a salvo; solo falló la copia externa
                System.err.println("[BackupService] Copia a carpeta secundaria falló: " + e.getMessage());
            }
            System.out.println("[BackupService] Respaldo diario creado: " + destino);
        } catch (Exception e) {
            System.err.println("[BackupService] Respaldo automático falló: " + e.getMessage());
        }
    }

    /**
     * Respaldo manual inmediato; reemplaza el de hoy si ya existía.
     * @return ruta del respaldo local creado
     * @throws Exception si falla el respaldo local o la copia secundaria
     */
    public Path respaldarAhora() throws Exception {
        Path destino = rutaRespaldoHoy();
        Files.deleteIfExists(destino);
        crearRespaldoLocal(destino);
        copiarACarpetaSecundaria(destino);
        return destino;
    }

    /** Carpeta local donde se guardan los respaldos diarios. */
    public Path getCarpetaLocal() {
        return DatabaseConnection.getRutaBaseDatos().getParent().resolve("backups");
    }

    public Optional<String> obtenerCarpetaSecundaria() {
        try {
            return configDAO.get(CLAVE_CARPETA_SECUNDARIA).filter(c -> !c.isBlank());
        } catch (SQLException e) {
            System.err.println("[BackupService] Error al leer carpeta secundaria: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Guarda (o limpia, con "") la carpeta secundaria. @return true si tuvo éxito. */
    public boolean guardarCarpetaSecundaria(String carpeta) {
        try {
            configDAO.set(CLAVE_CARPETA_SECUNDARIA, carpeta == null ? "" : carpeta);
            return true;
        } catch (SQLException e) {
            System.err.println("[BackupService] Error al guardar carpeta secundaria: " + e.getMessage());
            return false;
        }
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private Path rutaRespaldoHoy() {
        return getCarpetaLocal().resolve("gymmanager-" + LocalDate.now() + ".db");
    }

    private void crearRespaldoLocal(Path destino) throws SQLException, IOException {
        Files.createDirectories(destino.getParent());
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.deleteIfExists(temporal);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            // VACUUM INTO no acepta parámetros; se escapan comillas simples
            st.execute("VACUUM INTO '" + temporal.toString().replace("'", "''") + "'");
        }
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        aplicarRetencion();
    }

    private void copiarACarpetaSecundaria(Path respaldo) throws IOException {
        Optional<String> carpeta = obtenerCarpetaSecundaria();
        if (carpeta.isEmpty()) return;
        Path dir = Path.of(carpeta.get());
        Files.createDirectories(dir);
        Files.copy(respaldo, dir.resolve(respaldo.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
    }

    /** Borra los respaldos locales más viejos, conservando los últimos {@value RETENCION}. */
    private void aplicarRetencion() throws IOException {
        try (Stream<Path> archivos = Files.list(getCarpetaLocal())) {
            List<Path> respaldos = archivos
                    .filter(p -> p.getFileName().toString().matches("gymmanager-\\d{4}-\\d{2}-\\d{2}\\.db"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString())) // fecha ISO = orden lexicográfico
                    .toList();
            for (int i = 0; i < respaldos.size() - RETENCION; i++) {
                Files.deleteIfExists(respaldos.get(i));
            }
        }
    }
}
