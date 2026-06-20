package com.gymmanager.dao;

import com.gymmanager.models.Membresia;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MembresiaDAO {
    List<Membresia> listar() throws SQLException;
    Optional<Membresia> buscarPorId(int id) throws SQLException;
}
