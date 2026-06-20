package com.gymmanager.services;

/**
 * Excepción de negocio para errores de envío WhatsApp vía CallMeBot.
 * Se usa como checked exception para que el compilador obligue a manejarla.
 */
public class WhatsAppException extends Exception {

    public WhatsAppException(String mensaje) {
        super(mensaje);
    }

    public WhatsAppException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}