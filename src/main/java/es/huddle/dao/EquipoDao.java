package es.huddle.dao;

import es.huddle.model.Equipo;

import java.util.List;
import java.util.Optional;

public interface EquipoDao {
    Optional<Equipo> findById(long id);

    List<Equipo> findByCreadorId(long creadorId);

    List<Equipo> findByUsuarioId(long usuarioId);

    List<Equipo> findAll();

    Equipo insert(Equipo equipo);

    int update(Equipo equipo);

    int deleteById(long id);
}