import { api } from './api.js';
import {
    button, confirmDelete, countsRow, dateParts, element, emptyIllustration, errorMessage, eventTypeIcon,
    formatDateTime, requireId, requireUser, roleLabel, setLoading, showError, showToast, userAvatar
} from './ui.js';

const teamId = requireId('id');
const page = document.getElementById('team-page');
const crumb = document.getElementById('team-crumb');
const tab = new URLSearchParams(location.search).get('tab') === 'plantilla' ? 'plantilla' : 'eventos';

function renderCrumb(team) {
    crumb.replaceChildren();
    const home = element('a', '', 'Mis equipos');
    home.href = 'equipos.html';
    crumb.append(home, document.createTextNode(` / ${team.nombre}`));
}

function createHeader(team, role) {
    const heading = element('div', 'row');
    heading.append(element('h1', '', team.nombre));
    heading.append(element('span', `chip ${role === 'ENTRENADOR' ? 'coach' : 'outline'}`, roleLabel(role)));
    return heading;
}

function createTabs() {
    const tabs = element('nav', 'tabs');
    tabs.setAttribute('aria-label', 'Secciones del equipo');
    [['eventos', 'Eventos'], ['plantilla', 'Plantilla']].forEach(([key, label]) => {
        const link = element('a', `tab${tab === key ? ' active' : ''}`, label);
        link.href = `equipo.html?id=${encodeURIComponent(teamId)}&tab=${key}`;
        if (tab === key) link.setAttribute('aria-current', 'page');
        tabs.append(link);
    });
    return tabs;
}

function eventCard(summary, role) {
    const event = summary.evento;
    const parts = dateParts(event.fechaHora);
    const card = element('article', 'event-card');
    const date = element('div', 'date-badge');
    date.append(element('span', '', parts.weekday), element('b', '', parts.day), element('small', '', parts.month));
    const details = element('div', 'event-main');
    const type = element('span', `chip ${event.tipo === 'PARTIDO' ? 'match' : 'outline'}`);
    type.append(eventTypeIcon(event.tipo), document.createTextNode(event.tipo === 'PARTIDO' ? 'Partido' : 'Entrenamiento'));
    details.append(type);
    details.append(element('strong', 'event-title', event.titulo));
    details.append(element('span', 'event-meta', `${formatDateTime(event.fechaHora, { short: true })} · ${event.lugar}`));
    const actions = element('div', 'event-actions table-actions');
    const attendance = element('a', 'btn sec small-btn', 'Ver asistencia');
    attendance.href = `evento.html?id=${encodeURIComponent(event.id)}`;
    actions.append(attendance);
    if (role === 'ENTRENADOR') {
        const edit = element('a', 'btn sec small-btn', 'Editar');
        edit.href = `evento-nuevo.html?equipo=${encodeURIComponent(teamId)}&evento=${encodeURIComponent(event.id)}`;
        const remove = button('Eliminar', 'btn danger small-btn', async () => {
            if (!confirmDelete(`¿Eliminar el evento «${event.titulo}» y sus respuestas?`)) return;
            try {
                await api.deleteEvent(event.id);
                showToast('Evento eliminado correctamente.');
                await loadTeam();
            } catch (error) {
                showToast(errorMessage(error), 'error');
            }
        });
        actions.append(edit, remove);
    }
    card.append(date, details, countsRow(summary), actions);
    return card;
}

function renderEvents(role, upcoming, past) {
    const section = element('section', 'column');
    section.style.gap = '12px';
    const header = element('div', 'row-between mobile-stack');
    header.append(element('h2', '', 'Próximos eventos'));
    if (role === 'ENTRENADOR') {
        const add = element('a', 'btn', '+ Nuevo evento');
        add.href = `evento-nuevo.html?equipo=${encodeURIComponent(teamId)}`;
        header.append(add);
    }
    section.append(header);
    if (!upcoming.length) {
        const empty = element('section', 'empty');
        empty.append(emptyIllustration(), element('h2', '', 'No hay próximos eventos'), element('p', 'muted', 'Aquí aparecerán los entrenamientos y partidos de este equipo.'));
        if (role === 'ENTRENADOR') {
            const add = element('a', 'btn', 'Crear evento');
            add.href = `evento-nuevo.html?equipo=${encodeURIComponent(teamId)}`;
            empty.append(add);
        }
        section.append(empty);
    } else {
        upcoming.forEach((item) => section.append(eventCard(item, role)));
    }
    const pastDetails = element('details', 'past-events');
    const summary = element('summary', '', `Eventos pasados (${past.length})`);
    pastDetails.append(summary);
    if (past.length) past.forEach((item) => pastDetails.append(eventCard(item, role)));
    else pastDetails.append(element('p', 'muted small', 'Todavía no hay eventos pasados.'));
    section.append(pastDetails);
    return section;
}

function memberRow(member, role) {
    const row = element('tr');
    const values = [member.dorsal == null ? '—' : String(member.dorsal), member.nombre, member.email, member.rol];
    ['Dorsal', 'Nombre', 'Email', 'Rol'].forEach((label, index) => {
        const cell = element('td', index === 1 ? 'name-cell member-name-cell' : '');
        cell.dataset.label = label;
        if (index === 1) cell.append(userAvatar(member), element('span', '', values[index]));
        else cell.textContent = values[index];
        row.append(cell);
    });
    const actions = element('td');
    actions.dataset.label = 'Acciones';
    if (role === 'ENTRENADOR' && member.rol === 'JUGADOR') {
        const controls = element('div', 'table-actions');
        controls.append(button('Cambiar dorsal', 'btn sec small-btn', async () => {
            const entered = window.prompt(`Nuevo dorsal para ${member.nombre} (0–99):`, String(member.dorsal ?? ''));
            if (entered === null) return;
            const dorsal = Number(entered);
            if (!Number.isInteger(dorsal) || dorsal < 0 || dorsal > 99) {
                showToast('El dorsal debe ser un número entero entre 0 y 99.', 'error');
                return;
            }
            try {
                await api.updateMember(teamId, member.usuarioId, { dorsal });
                showToast('Dorsal actualizado correctamente.');
                await loadTeam();
            } catch (error) {
                showToast(errorMessage(error), 'error');
            }
        }));
        controls.append(button('Quitar', 'btn danger small-btn', async () => {
            if (!confirmDelete(`¿Quitar a ${member.nombre} de este equipo?`)) return;
            try {
                await api.deleteMember(teamId, member.usuarioId);
                showToast('Jugador eliminado del equipo.');
                await loadTeam();
            } catch (error) {
                showToast(errorMessage(error), 'error');
            }
        }));
        actions.append(controls);
    } else {
        actions.textContent = '—';
        actions.className = 'muted';
    }
    row.append(actions);
    return row;
}

function renderMembers(role, members) {
    const layout = element('div', 'row-start');
    const tableCard = element('section', 'card grow');
    tableCard.append(element('h2', '', `Plantilla (${members.length})`));
    if (!members.length) {
        tableCard.append(emptyIllustration(), element('p', 'muted', 'Aún no hay miembros en la plantilla.'));
    } else {
        const wrap = element('div', 'table-wrap');
        const table = element('table', 'table-mobile-cards');
        const head = element('thead');
        const heading = element('tr');
        ['Dorsal', 'Nombre', 'Email', 'Rol', 'Acciones'].forEach((label) => heading.append(element('th', '', label)));
        head.append(heading);
        const body = element('tbody');
        members.forEach((member) => body.append(memberRow(member, role)));
        table.append(head, body);
        wrap.append(table);
        tableCard.append(wrap);
    }
    layout.append(tableCard);

    if (role === 'ENTRENADOR') {
        const addCard = element('section', 'card');
        addCard.style.width = 'min(100%, 320px)';
        addCard.append(element('h2', '', 'Añadir jugador'));
        const form = element('form', 'column');
        form.style.gap = '12px';
        const emailField = element('div', 'field');
        const emailLabel = element('label', '', 'Email del usuario *');
        emailLabel.htmlFor = 'member-email';
        const email = element('input');
        email.id = 'member-email';
        email.name = 'email';
        email.type = 'email';
        email.required = true;
        email.placeholder = 'jugador@correo.es';
        emailField.append(emailLabel, email, element('span', 'field-error'));
        const numberField = element('div', 'field');
        const numberLabel = element('label', '', 'Dorsal *');
        numberLabel.htmlFor = 'member-number';
        const number = element('input');
        number.id = 'member-number';
        number.name = 'dorsal';
        number.type = 'number';
        number.min = '0';
        number.max = '99';
        number.required = true;
        number.placeholder = 'Ej. 9';
        numberField.append(numberLabel, number, element('span', 'field-error'));
        const alert = element('div', 'banner');
        alert.hidden = true;
        alert.setAttribute('role', 'alert');
        const save = button('Añadir al equipo', 'btn full', null, 'submit');
        form.append(emailField, numberField, element('p', 'muted small', 'El usuario debe existir. Se añadirá con rol JUGADOR.'), alert, save);
        form.addEventListener('input', () => {
            alert.hidden = true;
            form.querySelectorAll('[aria-invalid="true"]').forEach((input) => input.removeAttribute('aria-invalid'));
        });
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            alert.hidden = true;
            save.disabled = true;
            save.textContent = 'Guardando…';
            try {
                await api.addMember(teamId, { email: email.value.trim(), dorsal: Number(number.value) });
                showToast('Jugador añadido correctamente.');
                await loadTeam();
            } catch (error) {
                alert.hidden = false;
                alert.textContent = errorMessage(error);
                if (error.status === 400 || error.status === 409) {
                    const input = error.message.toLowerCase().includes('dorsal') ? number : email;
                    input.setAttribute('aria-invalid', 'true');
                    input.focus();
                    input.closest('.field').querySelector('.field-error').textContent = error.message;
                }
            } finally {
                save.disabled = false;
                save.textContent = 'Añadir al equipo';
            }
        });
        addCard.append(form);
        layout.append(addCard);
    }
    return layout;
}

async function loadTeam() {
    setLoading(page, 'Cargando equipo…');
    try {
        const detail = await api.team(teamId);
        const team = detail.equipo;
        const role = detail.rol;
        renderCrumb(team);
        const content = element('div', 'column');
        content.style.gap = '20px';
        content.append(createHeader(team, role), createTabs());
        if (tab === 'eventos') {
            const [upcoming, past] = await Promise.all([
                api.teamEvents(teamId, 'proximos'),
                api.teamEvents(teamId, 'pasados')
            ]);
            content.append(renderEvents(role, upcoming, past));
        } else {
            const members = await api.members(teamId);
            content.append(renderMembers(role, members));
        }
        page.replaceChildren(content);
        document.title = `Huddle · ${team.nombre}`;
    } catch (error) {
        showError(page, error, loadTeam);
    }
}

const user = await requireUser();
if (user && teamId) await loadTeam();