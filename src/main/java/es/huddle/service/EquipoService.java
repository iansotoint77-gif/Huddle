package es.huddle.service;

import es.huddle.dao.EquipoDao;
import es.huddle.dao.EventoDao;
import es.huddle.dao.MiembroDao;
import es.huddle.dao.RespuestaDao;
import es.huddle.dao.UsuarioDao;
import es.huddle.dto.AnadirMiembroDto;
import es.huddle.dto.ActualizarDorsalDto;
import es.huddle.dto.EquipoDetalleDto;
import es.huddle.dto.EquipoDto;
import es.huddle.dto.EquipoResumenDto;
import es.huddle.dto.EventoResumenDto;
import es.huddle.dto.GuardarEquipoDto;
import es.huddle.dto.MiembroEquipoDetalleDto;
import es.huddle.dto.MiembroEquipoDto;
import es.huddle.dto.mapper.EquipoMapper;
import es.huddle.dto.mapper.EventoMapper;
import es.huddle.dto.mapper.MiembroEquipoMapper;
import es.huddle.exception.DuplicateException;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.model.Equipo;
import es.huddle.model.EstadoAsistencia;
import es.huddle.model.MiembroEquipo;
import es.huddle.model.Rol;
import es.huddle.util.TransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class EquipoService {
    private final UsuarioDao usuarioDao;
    private final EquipoDao equipoDao;
    private final MiembroDao miembroDao;
    private final EventoDao eventoDao;
    private final RespuestaDao respuestaDao;
    private final TransactionManager transactionManager;

    public EquipoService(UsuarioDao usuarioDao, EquipoDao equipoDao, MiembroDao miembroDao, EventoDao eventoDao,
                         RespuestaDao respuestaDao, TransactionManager transactionManager) {
        this.usuarioDao = Objects.requireNonNull(usuarioDao);
        this.equipoDao = Objects.requireNonNull(equipoDao);
        this.miembroDao = Objects.requireNonNull(miembroDao);
        this.eventoDao = Objects.requireNonNull(eventoDao);
        this.respuestaDao = Objects.requireNonNull(respuestaDao);
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }

    public List<EquipoResumenDto> listarEquipos(long usuarioId) {
        if (usuarioDao.findById(usuarioId).isEmpty()) {
            throw new NotFoundException("No existe el usuario.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        return equipoDao.findByUsuarioId(usuarioId).stream().map(equipo -> {
            Rol rol = miembroDao.findById(equipo.id(), usuarioId)
                    .orElseThrow(() -> new NotFoundException("El usuario ya no pertenece al equipo."))
                    .rol();
            int jugadores = (int) miembroDao.findByEquipoId(equipo.id()).stream()
                    .filter(miembro -> miembro.rol() == Rol.JUGADOR).count();
            int eventosProximos = (int) eventoDao.findByEquipoId(equipo.id()).stream()
                    .filter(evento -> evento.fechaHora().isAfter(ahora)).count();
            return new EquipoResumenDto(equipo.id(), equipo.nombre(), equipo.categoria(), equipo.creadorId(), rol,
                    jugadores, eventosProximos);
        }).toList();
    }

    public EquipoDto crearEquipo(long usuarioId, GuardarEquipoDto dto) {
        if (dto == null) {
            throw new ValidationException("Los datos del equipo son obligatorios.");
        }
        String nombre = ServiceValidation.required(dto.nombre(), "nombre", 120);
        String categoria = ServiceValidation.required(dto.categoria(), "categoría", 80);
        var creador = usuarioDao.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("No existe el usuario creador."));

        transactionManager.begin();
        try {
            Equipo equipo = equipoDao.insert(new Equipo(null, nombre, categoria, creador.id()));
            miembroDao.insert(new MiembroEquipo(equipo.id(), usuarioId, Rol.ENTRENADOR, null));
            transactionManager.commit();
            return EquipoMapper.toDto(equipo);
        } catch (RuntimeException exception) {
            try {
                transactionManager.rollback();
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    public EquipoDto actualizarEquipo(long usuarioId, long equipoId, GuardarEquipoDto dto) {
        Equipo equipo = requireTrainer(equipoId, usuarioId);
        if (dto == null) {
            throw new ValidationException("Los datos del equipo son obligatorios.");
        }
        String nombre = ServiceValidation.required(dto.nombre(), "nombre", 120);
        String categoria = ServiceValidation.required(dto.categoria(), "categoría", 80);
        Equipo actualizado = new Equipo(equipo.id(), nombre, categoria, equipo.creadorId());
        if (equipoDao.update(actualizado) == 0 && equipoDao.findById(equipoId).isEmpty()) {
            throw new NotFoundException("No existe el equipo.");
        }
        return EquipoMapper.toDto(actualizado);
    }

    public void eliminarEquipo(long usuarioId, long equipoId) {
        requireTrainer(equipoId, usuarioId);
        if (equipoDao.deleteById(equipoId) == 0) {
            throw new NotFoundException("No existe el equipo.");
        }
    }

    public EquipoDetalleDto obtenerEquipo(long usuarioId, long equipoId) {
        Equipo equipo = equipoDao.findById(equipoId)
                .orElseThrow(() -> new NotFoundException("No existe el equipo."));
        MiembroEquipo miembro = miembroDao.findById(equipoId, usuarioId)
                .orElseThrow(() -> new ForbiddenException("No perteneces a este equipo."));
        return new EquipoDetalleDto(EquipoMapper.toDto(equipo), miembro.rol());
    }

    public List<MiembroEquipoDetalleDto> listarMiembros(long usuarioId, long equipoId) {
        requireMember(equipoId, usuarioId);
        return miembroDao.findByEquipoId(equipoId).stream().map(miembro -> {
            var usuario = usuarioDao.findById(miembro.usuarioId())
                    .orElseThrow(() -> new NotFoundException("No existe un usuario de la plantilla."));
            return new MiembroEquipoDetalleDto(miembro.equipoId(), miembro.usuarioId(), usuario.nombre(),
                    usuario.email(), miembro.rol(), miembro.dorsal());
        }).toList();
    }

    public MiembroEquipoDto agregarJugador(long usuarioId, long equipoId, AnadirMiembroDto dto) {
        requireTrainer(equipoId, usuarioId);
        if (dto == null) {
            throw new ValidationException("Los datos del miembro son obligatorios.");
        }
        String email = ServiceValidation.email(dto.email());
        int dorsal = ServiceValidation.dorsal(dto.dorsal());
        var jugador = usuarioDao.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe un usuario con ese email."));
        if (miembroDao.existsById(equipoId, jugador.id())) {
            throw new DuplicateException("El usuario ya pertenece al equipo.");
        }
        ensureDorsalDisponible(equipoId, jugador.id(), dorsal);
        MiembroEquipo miembro = new MiembroEquipo(equipoId, jugador.id(), Rol.JUGADOR, dorsal);
        try {
            miembroDao.insert(miembro);
        } catch (DuplicateException exception) {
            throw new DuplicateException("El usuario o el dorsal ya está asignado en este equipo.", exception);
        }
        return MiembroEquipoMapper.toDto(miembro);
    }

    public MiembroEquipoDto cambiarDorsal(long usuarioId, long equipoId, long miembroId, ActualizarDorsalDto dto) {
        requireTrainer(equipoId, usuarioId);
        if (dto == null) {
            throw new ValidationException("El dorsal es obligatorio.");
        }
        int dorsal = ServiceValidation.dorsal(dto.dorsal());
        MiembroEquipo objetivo = miembroDao.findById(equipoId, miembroId)
                .orElseThrow(() -> new NotFoundException("El miembro no pertenece al equipo."));
        ensureDorsalDisponible(equipoId, miembroId, dorsal);
        try {
            if (miembroDao.updateDorsal(equipoId, miembroId, dorsal) == 0 &&
                    !miembroDao.existsById(equipoId, miembroId)) {
                throw new NotFoundException("El miembro no pertenece al equipo.");
            }
        } catch (DuplicateException exception) {
            throw new DuplicateException("El dorsal ya está asignado en este equipo.", exception);
        }
        return MiembroEquipoMapper.toDto(new MiembroEquipo(equipoId, miembroId, objetivo.rol(), dorsal));
    }

    public void eliminarMiembro(long usuarioId, long equipoId, long miembroId) {
        Equipo equipo = requireTrainer(equipoId, usuarioId);
        miembroDao.findById(equipoId, miembroId)
                .orElseThrow(() -> new NotFoundException("El miembro no pertenece al equipo."));
        if (miembroId == usuarioId) {
            throw new ValidationException("No puedes quitarte a ti mismo del equipo.");
        }
        if (miembroId == equipo.creadorId()) {
            throw new ValidationException("No se puede eliminar al creador del equipo.");
        }
        if (miembroDao.deleteById(equipoId, miembroId) == 0) {
            throw new NotFoundException("El miembro no pertenece al equipo.");
        }
    }

    public List<EventoResumenDto> listarEventos(long usuarioId, long equipoId, String tiempo) {
        requireMember(equipoId, usuarioId);
        if (!"proximos".equals(tiempo) && !"pasados".equals(tiempo)) {
            throw new ValidationException("El filtro tiempo debe ser 'proximos' o 'pasados'.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<MiembroEquipo> jugadores = miembroDao.findByEquipoId(equipoId).stream()
                .filter(miembro -> miembro.rol() == Rol.JUGADOR).toList();
        return eventoDao.findByEquipoId(equipoId).stream()
                .filter(evento -> "proximos".equals(tiempo) ? evento.fechaHora().isAfter(ahora) :
                        !evento.fechaHora().isAfter(ahora))
                .map(evento -> {
                    List<es.huddle.model.Respuesta> respuestas = respuestaDao.findByEventoId(evento.id());
                    int voy = contar(respuestas, EstadoAsistencia.VOY, jugadores);
                    int noVoy = contar(respuestas, EstadoAsistencia.NO_VOY, jugadores);
                    int quizas = contar(respuestas, EstadoAsistencia.QUIZAS, jugadores);
                    int respondidos = voy + noVoy + quizas;
                    return new EventoResumenDto(EventoMapper.toDto(evento), voy, noVoy, quizas,
                            Math.max(0, jugadores.size() - respondidos));
                }).toList();
    }

    private Equipo requireTrainer(long equipoId, long usuarioId) {
        Equipo equipo = equipoDao.findById(equipoId)
                .orElseThrow(() -> new NotFoundException("No existe el equipo."));
        MiembroEquipo miembro = miembroDao.findById(equipoId, usuarioId)
                .orElseThrow(() -> new ForbiddenException("No perteneces a este equipo."));
        if (miembro.rol() != Rol.ENTRENADOR) {
            throw new ForbiddenException("Solo el entrenador puede gestionar este equipo.");
        }
        return equipo;
    }

    private void requireMember(long equipoId, long usuarioId) {
        if (equipoDao.findById(equipoId).isEmpty()) {
            throw new NotFoundException("No existe el equipo.");
        }
        if (miembroDao.findById(equipoId, usuarioId).isEmpty()) {
            throw new ForbiddenException("No perteneces a este equipo.");
        }
    }

    private void ensureDorsalDisponible(long equipoId, long usuarioId, int dorsal) {
        boolean ocupado = miembroDao.findByEquipoId(equipoId).stream()
                .anyMatch(miembro -> miembro.usuarioId() != usuarioId &&
                        miembro.dorsal() != null && miembro.dorsal() == dorsal);
        if (ocupado) {
            throw new DuplicateException("El dorsal ya está asignado en este equipo.");
        }
    }

    private int contar(List<es.huddle.model.Respuesta> respuestas, EstadoAsistencia estado,
                       List<MiembroEquipo> jugadores) {
        return (int) respuestas.stream().filter(respuesta -> respuesta.estado() == estado)
                .filter(respuesta -> jugadores.stream().anyMatch(jugador ->
                        jugador.usuarioId().longValue() == respuesta.usuarioId().longValue()))
                .count();
    }
}