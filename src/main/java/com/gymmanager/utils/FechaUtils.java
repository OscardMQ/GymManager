package com.gymmanager.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilidades de fecha y hora para uso en toda la aplicación.
 *
 * Convenio: la BD almacena fechas en formato ISO (yyyy-MM-dd).
 * La UI muestra fechas en formato local (dd/MM/yyyy).
 * Esta clase centraliza las conversiones para no duplicar lógica.
 */
public class FechaUtils {

    private static final DateTimeFormatter FMT_ISO     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_HORA    = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FMT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Fecha de hoy en formato ISO — para guardar en BD */
    public static String hoyISO() {
        return LocalDate.now().format(FMT_ISO);
    }

    /** Hora actual en formato HH:mm:ss — para la bitácora */
    public static String horaActual() {
        return LocalTime.now().format(FMT_HORA);
    }

    /**
     * Calcula la fecha de vencimiento de una membresía.
     *
     * @param fechaInicioISO Fecha de inicio en formato yyyy-MM-dd
     * @param dias           Duración de la membresía en días
     * @return Fecha de fin en formato yyyy-MM-dd
     */
    public static String calcularFechaFin(String fechaInicioISO, int dias) {
        return LocalDate.parse(fechaInicioISO, FMT_ISO)
                        .plusDays(dias)
                        .format(FMT_ISO);
    }

    /** Convierte de formato BD (yyyy-MM-dd) a formato de pantalla (dd/MM/yyyy) */
    public static String aFormatoDisplay(String fechaISO) {
        if (fechaISO == null || fechaISO.isBlank()) return "—";
        try {
            return LocalDate.parse(fechaISO, FMT_ISO).format(FMT_DISPLAY);
        } catch (DateTimeParseException e) {
            return fechaISO; // mostrar el dato crudo antes que reventar la vista
        }
    }

    /**
     * Verifica si una membresía ya venció.
     *
     * @param fechaFinISO Fecha de vencimiento en formato yyyy-MM-dd
     * @return true si la fecha ya pasó, o si es null/vacía/ilegible
     */
    public static boolean estaVencida(String fechaFinISO) {
        if (fechaFinISO == null || fechaFinISO.isBlank()) return true;
        try {
            return LocalDate.parse(fechaFinISO, FMT_ISO).isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return true; // fecha corrupta = tratar como vencida, no tumbar el listado
        }
    }

    /**
     * Días restantes hasta el vencimiento de la membresía.
     * Retorna 0 si ya venció o la fecha es ilegible.
     */
    public static long diasRestantes(String fechaFinISO) {
        if (fechaFinISO == null || fechaFinISO.isBlank()) return 0;
        try {
            LocalDate fin = LocalDate.parse(fechaFinISO, FMT_ISO);
            long diff = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fin);
            return Math.max(0, diff);
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    /**
     * Sobrecarga que acepta LocalDate directamente.
     * Usada por controllers que trabajan con DatePicker.
     */
    public static LocalDate calcularFechaFin(LocalDate inicio, int dias) {
        return inicio.plusDays(dias);
    }

    private FechaUtils() {}
}
