package es.huddle.config;

import es.huddle.dao.EquipoDao;
import es.huddle.dao.EquipoDaoJdbc;
import es.huddle.dao.EventoDao;
import es.huddle.dao.EventoDaoJdbc;
import es.huddle.dao.MiembroDao;
import es.huddle.dao.MiembroDaoJdbc;
import es.huddle.dao.RespuestaDao;
import es.huddle.dao.RespuestaDaoJdbc;
import es.huddle.dao.UsuarioDao;
import es.huddle.dao.UsuarioDaoJdbc;
import es.huddle.service.EquipoService;
import es.huddle.service.EventoService;
import es.huddle.service.RespuestaService;
import es.huddle.service.UsuarioService;
import es.huddle.util.TransactionManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import javax.sql.DataSource;

@WebListener
public final class AppConfig implements ServletContextListener {
    public static final String USUARIO_SERVICE = "es.huddle.usuarioService";
    public static final String EQUIPO_SERVICE = "es.huddle.equipoService";
    public static final String EVENTO_SERVICE = "es.huddle.eventoService";
    public static final String RESPUESTA_SERVICE = "es.huddle.respuestaService";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        DataSource dataSource = DataSourceProvider.getDataSource();
        TransactionManager transactionManager = new TransactionManager(dataSource);
        UsuarioDao usuarioDao = new UsuarioDaoJdbc(transactionManager);
        EquipoDao equipoDao = new EquipoDaoJdbc(transactionManager);
        MiembroDao miembroDao = new MiembroDaoJdbc(transactionManager);
        EventoDao eventoDao = new EventoDaoJdbc(transactionManager);
        RespuestaDao respuestaDao = new RespuestaDaoJdbc(transactionManager);

        ServletContext context = event.getServletContext();
        context.setAttribute(USUARIO_SERVICE, new UsuarioService(usuarioDao));
        context.setAttribute(EQUIPO_SERVICE, new EquipoService(usuarioDao, equipoDao, miembroDao, eventoDao,
                respuestaDao, transactionManager));
        context.setAttribute(EVENTO_SERVICE, new EventoService(equipoDao, eventoDao, miembroDao));
        context.setAttribute(RESPUESTA_SERVICE, new RespuestaService(eventoDao, miembroDao, respuestaDao, usuarioDao));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        DataSourceProvider.close();
    }
}