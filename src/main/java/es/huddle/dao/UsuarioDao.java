package es.huddle.dao;

import es.huddle.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioDao {
    Optional<Usuario> findById(long id);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findAll();

    Usuario insert(Usuario usuario);

    int update(Usuario usuario);

    int deleteById(long id);
}