package com.gymmanager.dao;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Contrato de acceso a la tabla clave/valor de configuración.
 * Pensada para parámetros persistentes (tokens, números de teléfono, etc.).
 */
public interface ConfiguracionDAO {

    /** Devuelve el valor de la clave, o vacío si no existe. */
    Optional<String> get(String clave) throws SQLException;

    /** Inserta o reemplaza el valor (upsert). */
    void set(String clave, String valor) throws SQLException;
}