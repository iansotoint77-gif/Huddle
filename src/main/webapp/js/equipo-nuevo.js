import { api, HuddleApiError } from './api.js';
import { requireUser, clearFormError, setButtonSaving, showFormError, showError, setLoading, element } from './ui.js';

const form = document.getElementById('team-form');
const alertBox = document.getElementById('form-alert');
const saveButton = document.getElementById('save-team');
const teamId = new URLSearchParams(location.search).get('equipo');
const title = document.getElementById('form-title');
const crumb = document.getElementById('team-crumb');
const info = document.getElementById('team-info');
const loading = document.getElementById('loading-state');

if (teamId && (!/^\d+$/.test(teamId) || Number(teamId) < 1)) {
    window.location.replace('equipos.html');
}

async function loadTeam() {
    try {
        setLoading(loading, 'Cargando equipo…');
        const detail = await api.team(teamId);
        if (detail.rol !== 'ENTRENADOR') {
            throw new HuddleApiError(403, 'Solo el entrenador puede editar este equipo.');
        }
        const team = detail.equipo;
        title.textContent = 'Editar equipo';
        document.title = 'Huddle · Editar equipo';
        crumb.replaceChildren(
            element('a', '', 'Mis equipos'), document.createTextNode(' / '),
            Object.assign(element('a', '', team.nombre), { href: `equipo.html?id=${encodeURIComponent(team.id)}` }),
            document.createTextNode(' / Editar equipo')
        );
        document.getElementById('team-name').value = team.nombre;
        const category = document.getElementById('team-category');
        if (![...category.options].some((option) => option.value === team.categoria)) {
            category.add(new Option(team.categoria, team.categoria));
        }
        category.value = team.categoria;
        info.hidden = true;
        saveButton.textContent = 'Guardar cambios';
        loading.hidden = true;
        form.hidden = false;
    } catch (error) {
        loading.hidden = false;
        showError(loading, error, loadTeam);
    }
}

form.addEventListener('input', () => clearFormError(alertBox, form));
form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFormError(alertBox, form);
    setButtonSaving(saveButton, true);
    try {
        const body = {
            nombre: document.getElementById('team-name').value.trim(),
            categoria: document.getElementById('team-category').value.trim()
        };
        const team = teamId ? await api.updateTeam(teamId, body) : await api.createTeam(body);
        window.location.assign(`equipo.html?id=${encodeURIComponent(team.id)}`);
    } catch (error) {
        showFormError(alertBox, error, form);
    } finally {
        setButtonSaving(saveButton, false, '', teamId ? 'Guardar cambios' : 'Crear equipo');
    }
});

const user = await requireUser();
if (user && teamId) {
    form.hidden = true;
    loading.hidden = false;
    await loadTeam();
}