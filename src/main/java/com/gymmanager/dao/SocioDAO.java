package com.gymmanager.dao;

import com.gymmanager.models.Socio;
import java.sql.SQLException;
import java.util.List;

public interface SocioDAO {
    List<Socio> listar() throws SQLException;
    List<Socio> buscarPorNombre(String nombre) throws SQLException;
    void guardar(Socio socio) throws SQLException;
    void actualizar(Socio socio) throws SQLException;
    void cambiarEstado(int id, boolean activo) throws SQLException;
}