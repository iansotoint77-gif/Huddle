import { api, HuddleApiError } from './api.js';
import {
    clearFormError, element, requireId, requireUser, setButtonSaving, setLoading, showError, showFormError, showToast
} from './ui.js';

const teamId = requireId('equipo');
const eventId = new URLSearchParams(location.search).get('evento');
const form = document.getElementById('event-form');
const loading = document.getElementById('loading-state');
const crumb = document.getElementById('event-crumb');
const formTitle = document.getElementById('form-title');
const alertBox = document.getElementById('form-alert');
const saveButton = document.getElementById('save-event');
const typeInput = document.getElementById('event-type');
const rivalField = document.getElementById('rival-field');
const rivalInput = document.getElementById('event-rival');
const trainingButton = document.getElementById('type-training');
const matchButton = document.getElementById('type-match');

if (eventId && (!/^\d+$/.test(eventId) || Number(eventId) < 1)) {
    window.location.replace('equipos.html');
}

function renderCrumb(team) {
    const link = element('a', '', 'Mis equipos');
    link.href = 'equipos.html';
    const teamLink = element('a', '', team.nombre);
    teamLink.href = `equipo.html?id=${encodeURIComponent(team.id)}`;
    crumb.replaceChildren(link, document.createTextNode(' / '), teamLink, document.createTextNode(eventId ? ' / Editar evento' : ' / Nuevo evento'));
}

function setRivalVisibility() {
    const show = typeInput.value === 'PARTIDO';
    rivalField.hidden = !show;
    rivalInput.required = show;
    trainingButton.classList.toggle('active', !show);
    matchButton.classList.toggle('active', show);
    trainingButton.setAttribute('aria-pressed', String(!show));
    matchButton.setAttribute('aria-pressed', String(show));
    if (!show) rivalInput.value = '';
}

trainingButton.addEventListener('click', () => {
    typeInput.value = 'ENTRENAMIENTO';
    setRivalVisibility();
});
matchButton.addEventListener('click', () => {
    typeInput.value = 'PARTIDO';
    setRivalVisibility();
});
form.addEventListener('input', () => clearFormError(alertBox, form));

async function loadForm() {
    setLoading(loading, 'Cargando formulario…');
    try {
        const detail = await api.team(teamId);
        if (detail.rol !== 'ENTRENADOR') {
            throw new HuddleApiError(403, 'Solo el entrenador puede crear o editar eventos.');
        }
        const team = detail.equipo;
        renderCrumb(team);
        document.getElementById('cancel-event').href = `equipo.html?id=${encodeURIComponent(teamId)}`;
        if (eventId) {
            const event = await api.event(eventId);
            if (String(event.equipoId) !== String(teamId)) {
                throw new HuddleApiError(404, 'El evento no pertenece a este equipo.');
            }
            typeInput.value = event.tipo;
            document.getElementById('event-title').value = event.titulo;
            document.getElementById('event-date').value = event.fechaHora.slice(0, 10);
            document.getElementById('event-time').value = event.fechaHora.slice(11, 16);
            document.getElementById('event-place').value = event.lugar;
            rivalInput.value = event.rival || '';
            formTitle.textContent = 'Editar evento';
            saveButton.textContent = 'Guardar cambios';
            document.title = `Huddle · Editar evento`;
        }
        setRivalVisibility();
        loading.replaceChildren();
        form.hidden = false;
    } catch (error) {
        showError(loading, error, loadForm);
    }
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFormError(alertBox, form);
    const body = {
        tipo: typeInput.value,
        titulo: document.getElementById('event-title').value.trim(),
        fechaHora: `${document.getElementById('event-date').value}T${document.getElementById('event-time').value}:00`,
        lugar: document.getElementById('event-place').value.trim(),
        rival: typeInput.value === 'PARTIDO' ? rivalInput.value.trim() : null
    };
    setButtonSaving(saveButton, true);
    try {
        const saved = eventId ? await api.updateEvent(eventId, body) : await api.createEvent(teamId, body);
        showToast(eventId ? 'Evento actualizado correctamente.' : 'Evento creado correctamente.');
        window.location.assign(`evento.html?id=${encodeURIComponent(saved.id)}`);
    } catch (error) {
        showFormError(alertBox, error, form);
    } finally {
        setButtonSaving(saveButton, false, '', eventId ? 'Guardar cambios' : 'Guardar evento');
    }
});

const user = await requireUser();
if (user && teamId) await loadForm();