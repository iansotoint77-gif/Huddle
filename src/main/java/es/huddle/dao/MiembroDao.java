package es.huddle.dao;

import es.huddle.model.MiembroEquipo;

import java.util.List;
import java.util.Optional;

public interface MiembroDao {
    Optional<MiembroEquipo> findById(long equipoId, long usuarioId);

    List<MiembroEquipo> findByEquipoId(long equipoId);

    List<MiembroEquipo> findByUsuarioId(long usuarioId);

    boolean existsById(long equipoId, long usuarioId);

    int insert(MiembroEquipo miembro);

    int updateDorsal(long equipoId, long usuarioId, Integer dorsal);

    int deleteById(long equipoId, long usuarioId);
}