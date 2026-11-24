package es.huddle.dao;

import es.huddle.model.Evento;

import java.util.List;
import java.util.Optional;

public interface EventoDao {
    Optional<Evento> findById(long id);

    List<Evento> findByEquipoId(long equipoId);

    List<Evento> findAll();

    Evento insert(Evento evento);

    int update(Evento evento);

    int deleteById(long id);
}