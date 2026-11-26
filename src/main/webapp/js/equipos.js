import { api } from './api.js';
import { element, requireUser, setLoading, showEmpty, showError, roleLabel } from './ui.js';

const list = document.getElementById('teams-list');

function renderTeams(teams) {
    list.replaceChildren();
    if (!teams.length) {
        showEmpty(list, 'Aún no tienes equipos', 'Crea tu primer equipo o pide al entrenador que te añada.', 'Crear mi primer equipo', () => {
            window.location.assign('equipo-nuevo.html');
        });
        return;
    }
    teams.forEach((team) => {
        const card = element('article', 'card');
        const title = element('div', 'card-title');
        title.append(element('span', 'shield', 'H'));
        const details = element('div', 'column');
        details.append(element('h2', '', team.nombre), element('span', 'muted small', `Categoría: ${team.categoria}`));
        title.append(details);
        const role = element('span', `chip ${team.rol === 'ENTRENADOR' ? 'coach' : 'outline'}`, roleLabel(team.rol));
        const metrics = element('p', '', `${team.numeroJugadores} jugadores · ${team.eventosProximos} eventos próximos`);
        const open = element('a', 'btn sec', 'Abrir equipo');
        open.href = `equipo.html?id=${encodeURIComponent(team.id)}`;
        card.append(title, role, metrics, open);
        list.append(card);
    });
}

async function loadTeams() {
    setLoading(list, 'Cargando equipos…');
    try {
        renderTeams(await api.teams());
    } catch (error) {
        showError(list, error, loadTeams);
    }
}

const user = await requireUser();
if (user) await loadTeams();