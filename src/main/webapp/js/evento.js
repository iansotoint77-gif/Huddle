import { api, HuddleApiError } from './api.js';
import {
    button, confirmDelete, element, emptyIllustration, errorMessage, eventTypeIcon, formatDateTime, requireId,
    requireUser, setButtonSaving, setLoading, showError, showToast, statusChip
} from './ui.js';

const eventId = requireId('id');
const page = document.getElementById('event-page');
const crumb = document.getElementById('event-crumb');
const mobileCrumb = document.getElementById('event-mobile-crumb');
let currentUser;

function eventHeading(event, team, role) {
    const card = element('section', 'card event-heading');
    const top = element('div', 'row-between mobile-stack');
    const type = element('span', `chip ${event.tipo === 'PARTIDO' ? 'match' : 'outline'}`);
    type.append(eventTypeIcon(event.tipo), document.createTextNode(event.tipo === 'PARTIDO' ? 'Partido' : 'Entrenamiento'));
    top.append(type);
    if (role === 'ENTRENADOR') {
        const actions = element('div', 'table-actions');
        const edit = element('a', 'btn sec small-btn', 'Editar evento');
        edit.href = `evento-nuevo.html?equipo=${encodeURIComponent(team.id)}&evento=${encodeURIComponent(event.id)}`;
        actions.append(edit, button('Eliminar evento', 'btn danger small-btn', async () => {
            if (!confirmDelete(`¿Eliminar «${event.titulo}» y todas sus respuestas?`)) return;
            try {
                await api.deleteEvent(event.id);
                showToast('Evento eliminado correctamente.');
                window.location.assign(`equipo.html?id=${encodeURIComponent(team.id)}`);
            } catch (error) {
                showToast(errorMessage(error), 'error');
            }
        }));
        top.append(actions);
    }
    const title = element('h1', '', event.titulo);
    const desktopMeta = element('p', 'muted desktop-event-meta', `${formatDateTime(event.fechaHora)} · ${event.lugar}`);
    const mobileDate = element('p', 'muted mobile-event-meta', formatDateTime(event.fechaHora, { short: true, year: false }));
    const mobilePlace = element('p', 'muted mobile-event-meta', event.lugar);
    card.append(top, title, desktopMeta, mobileDate, mobilePlace);
    return card;
}

function stats(summary) {
    const row = element('section', 'stats');
    [
        ['yes', summary.voy, 'Voy', 'Voy'],
        ['no', summary.noVoy, 'No voy', 'No voy'],
        ['maybe', summary.quizas, 'Quizás', 'Quizás'],
        ['', summary.sinResponder, 'Sin responder', 'Sin resp.']
    ].forEach(([style, count, label, shortLabel]) => {
        const stat = element('div', `stat${style ? ` ${style}` : ''}`);
        const caption = element('span');
        caption.append(element('span', 'desktop-label', label), element('span', 'mobile-label', shortLabel));
        stat.append(element('b', '', String(count)), caption);
        row.append(stat);
    });
    return row;
}

function renderPlayers(summary) {
    const card = element('section', 'card');
    card.append(element('h2', '', 'Asistencia del equipo'));
    const filters = element('div', 'filter-row');
    const wrap = element('div', 'table-wrap');
    const table = element('table', 'table-mobile-cards');
    const head = element('thead');
    const headings = element('tr');
    ['Jugador', 'Dorsal', 'Respuesta', 'Comentario'].forEach((label) => headings.append(element('th', '', label)));
    head.append(headings);
    const body = element('tbody');
    const filtersData = [
        ['TODOS', `Todos ${summary.jugadores.length}`, 'all', null],
        ['VOY', `Voy ${summary.voy}`, 'outline', 'VOY'],
        ['NO_VOY', `No voy ${summary.noVoy}`, 'outline', 'NO_VOY'],
        ['QUIZAS', `Quizás ${summary.quizas}`, 'outline', 'QUIZAS'],
        ['SIN_RESPONDER', `Sin responder ${summary.sinResponder}`, 'unanswered', 'SIN_RESPONDER']
    ];
    let activeFilter = 'TODOS';
    const renderRows = () => {
        body.replaceChildren();
        const visiblePlayers = activeFilter === 'TODOS' ? summary.jugadores :
            summary.jugadores.filter((player) => player.estado === activeFilter);
        visiblePlayers.forEach((player) => {
            const row = element('tr');
            const name = element('td', 'name-cell', player.nombre);
            name.dataset.label = 'Jugador';
            const number = element('td', '', player.dorsal == null ? '—' : String(player.dorsal));
            number.dataset.label = 'Dorsal';
            const state = element('td');
            state.dataset.label = 'Respuesta';
            state.append(statusChip(player.estado));
            const comment = element('td', 'muted', player.comentario || '—');
            comment.dataset.label = 'Comentario';
            row.append(name, number, state, comment);
            body.append(row);
        });
        if (!visiblePlayers.length) {
            const emptyRow = element('tr');
            const emptyCell = element('td', 'muted', 'No hay jugadores en este estado.');
            emptyCell.colSpan = 4;
            emptyRow.append(emptyCell);
            body.append(emptyRow);
        }
    };
    filtersData.forEach(([key, label, style]) => {
        const filter = button(label, `chip filter-option ${style}`, () => {
            activeFilter = key;
            filters.querySelectorAll('button').forEach((node) => node.setAttribute('aria-pressed', String(node === filter)));
            renderRows();
        });
        filter.setAttribute('aria-pressed', String(key === activeFilter));
        filters.append(filter);
    });
    renderRows();
    card.append(filters);
    table.append(head, body);
    wrap.append(table);
    if (!summary.jugadores.length) card.append(emptyIllustration(), element('p', 'muted', 'Este equipo todavía no tiene jugadores añadidos.'));
    else card.append(wrap);
    return card;
}

function renderPlayerAttendance(summary) {
    const panel = element('section', 'card attendance-summary');
    panel.append(element('h2', 'desktop-title', 'Asistencia del equipo'), stats(summary));
    const players = element('section', 'player-list');
    players.append(element('h2', 'mobile-title', 'Jugadores'));
    const wrap = element('div', 'table-wrap');
    const table = element('table', 'player-mobile-table');
    const head = element('thead');
    const headings = element('tr');
    ['Jugador', 'Dorsal', 'Respuesta', 'Comentario'].forEach((label) => headings.append(element('th', '', label)));
    head.append(headings);
    const body = element('tbody');
    summary.jugadores.forEach((player) => {
        const row = element('tr');
        const isCurrent = Number(player.usuarioId) === Number(currentUser.id);
        const name = element('td', 'name-cell', `${player.nombre}${isCurrent ? ' (tú)' : ''}`);
        name.dataset.label = 'Jugador';
        const number = element('td', '', player.dorsal == null ? '—' : String(player.dorsal));
        number.dataset.label = 'Dorsal';
        const state = element('td');
        state.dataset.label = 'Respuesta';
        state.append(statusChip(player.estado));
        const comment = element('td', 'muted', player.comentario || '—');
        comment.dataset.label = 'Comentario';
        row.append(name, number, state, comment);
        body.append(row);
    });
    table.append(head, body);
    wrap.append(table);
    if (!summary.jugadores.length) players.append(emptyIllustration(), element('p', 'muted', 'Este equipo todavía no tiene jugadores añadidos.'));
    else players.append(wrap);
    panel.append(players);
    return panel;
}

function renderVote(summary, event) {
    const card = element('section', 'card event-vote');
    card.append(element('h2', '', 'Tu respuesta'));
    const own = summary.jugadores.find((player) => Number(player.usuarioId) === Number(currentUser.id));
    if (new Date(event.fechaHora).getTime() <= Date.now()) {
        card.append(element('div', 'banner info', 'Este evento ya ha comenzado o ha pasado. Ya no se puede votar.'));
        return card;
    }
    if (!own) {
        card.append(element('div', 'banner', 'No se encontró tu usuario en la plantilla del equipo.'));
        return card;
    }
    let selected = own.estado === 'SIN_RESPONDER' ? '' : own.estado;
    const options = element('div', 'vote-options');
    const values = [['VOY', 'Voy'], ['NO_VOY', 'No voy'], ['QUIZAS', 'Quizás']];
    const voteButtons = values.map(([state, label]) => {
        const vote = button(label, 'btn vote-option', () => {
            selected = state;
            voteButtons.forEach(([value, node]) => node.setAttribute('aria-pressed', String(value === selected)));
        });
        vote.setAttribute('aria-pressed', String(selected === state));
        options.append(vote);
        return [state, vote];
    });
    const commentField = element('div', 'field');
    const label = element('label', '', 'Comentario (opcional)');
    label.htmlFor = 'vote-comment';
    const comment = element('textarea');
    comment.id = 'vote-comment';
    comment.name = 'comentario';
    comment.maxLength = 500;
    comment.value = own.comentario || '';
    comment.placeholder = 'Añade un comentario si lo necesitas';
    commentField.append(label, comment);
    const alert = element('div', 'banner');
    alert.hidden = true;
    alert.setAttribute('role', 'alert');
    const save = button('Guardar respuesta', 'btn full', async () => {
        alert.hidden = true;
        if (!selected) {
            alert.hidden = false;
            alert.textContent = 'Selecciona Voy, No voy o Quizás antes de guardar.';
            return;
        }
        setButtonSaving(save, true);
        try {
            await api.saveResponse(event.id, { estado: selected, comentario: comment.value.trim() || null });
            showToast('Respuesta guardada correctamente.');
            await loadEvent();
        } catch (error) {
            alert.hidden = false;
            alert.replaceChildren(element('span', '', errorMessage(error)));
            if (error instanceof HuddleApiError && (error.status === 0 || error.status >= 500)) {
                alert.append(button('Reintentar', 'btn sec small-btn', () => save.click()));
            }
        } finally {
            setButtonSaving(save, false, '', 'Guardar respuesta');
        }
    });
    card.append(options, commentField, alert, save,
        element('p', 'muted small', 'Puedes cambiar tu respuesta hasta que empiece el evento.'));
    return card;
}

function renderEvent(event, teamDetail, summary) {
    const team = teamDetail.equipo;
    const role = teamDetail.rol;
    const home = element('a', '', 'Mis equipos');
    home.href = 'equipos.html';
    const link = element('a', '', team.nombre);
    link.href = `equipo.html?id=${encodeURIComponent(team.id)}`;
    crumb.replaceChildren(home, document.createTextNode(' / '));
    crumb.append(link, document.createTextNode(` / ${event.titulo}`));
    mobileCrumb.replaceChildren();
    const back = element('a', '', `‹ ${team.nombre}`);
    back.href = `equipo.html?id=${encodeURIComponent(team.id)}`;
    mobileCrumb.append(back);

    const content = element('div', 'column');
    content.style.gap = '20px';
    if (role === 'JUGADOR') {
        const attendance = element('div', 'attendance-layout');
        attendance.append(renderVote(summary, event), renderPlayerAttendance(summary));
        content.append(eventHeading(event, team, role), attendance);
    } else {
        content.append(eventHeading(event, team, role), stats(summary), renderPlayers(summary));
    }
    page.replaceChildren(content);
    document.title = `Huddle · ${event.titulo}`;
}

async function loadEvent() {
    setLoading(page, 'Cargando asistencia…');
    try {
        const event = await api.event(eventId);
        const [teamDetail, summary] = await Promise.all([
            api.team(event.equipoId),
            api.attendance(eventId)
        ]);
        renderEvent(event, teamDetail, summary);
    } catch (error) {
        showError(page, error, loadEvent);
    }
}

currentUser = await requireUser();
if (currentUser && eventId) await loadEvent();