import { clearSession, HuddleApiError, readSession } from './api.js';

export function element(tag, className = '', text = '') {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
}

export function button(text, className = 'btn', onClick, type = 'button') {
    const node = element('button', className, text);
    node.type = type;
    if (onClick) node.addEventListener('click', onClick);
    return node;
}

export function setLoading(container, label = 'Cargando…') {
    container.replaceChildren();
    const loader = element('div', 'skeleton-grid');
    loader.setAttribute('aria-label', label);
    loader.setAttribute('aria-busy', 'true');
    loader.setAttribute('role', 'status');
    for (let index = 0; index < 3; index += 1) {
        const card = element('div', 'skeleton-card');
        card.setAttribute('aria-hidden', 'true');
        const heading = element('div', 'skeleton-head');
        heading.append(element('span', 'skeleton-avatar'), element('span', 'skeleton-line w60'));
        card.append(heading, element('span', 'skeleton-line w80'), element('span', 'skeleton-line'), element('span', 'skeleton-line w35'));
        loader.append(card);
    }
    if (container.classList.contains('card-grid')) loader.style.gridColumn = '1 / -1';
    container.append(loader);
}

export function showEmpty(container, title, description, actionText, action) {
    container.replaceChildren();
    const empty = element('section', 'empty');
    empty.append(emptyIllustration(), element('h2', '', title), element('p', 'muted', description));
    if (actionText && action) empty.append(button(actionText, 'btn', action));
    if (container.classList.contains('card-grid')) empty.style.gridColumn = '1 / -1';
    container.append(empty);
}

export function errorMessage(error) {
    if (!(error instanceof HuddleApiError)) return 'Se ha producido un error inesperado. Inténtalo de nuevo.';
    if (error.status === 0) return error.message;
    if (error.status === 400) return error.message || 'Revisa los datos introducidos.';
    if (error.status === 403) return error.message || 'No tienes permiso para realizar esta acción.';
    if (error.status === 404) return error.message || 'No se ha encontrado el recurso solicitado.';
    if (error.status === 409) return error.message || 'El dato ya existe o entra en conflicto con otro.';
    if (error.status >= 500) return 'Error del servidor. Inténtalo de nuevo.';
    return error.message;
}

export function showError(container, error, retry) {
    container.replaceChildren();
    const box = element('div', 'banner');
    box.setAttribute('role', 'alert');
    box.append(element('p', '', errorMessage(error)));
    if (retry) box.append(button('Reintentar', 'btn sec small-btn', retry));
    container.append(box);
}

export function showToast(message, kind = 'success') {
    let region = document.querySelector('.toast-region');
    if (!region) {
        region = element('div', 'toast-region');
        region.setAttribute('aria-live', 'polite');
        document.body.append(region);
    }
    const toast = element('div', kind === 'error' ? 'toast error' : 'toast', message);
    region.append(toast);
    window.setTimeout(() => toast.remove(), 3800);
}

export function emptyIllustration() {
    const namespace = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(namespace, 'svg');
    svg.setAttribute('viewBox', '0 0 88 68');
    svg.setAttribute('class', 'empty-illustration');
    svg.setAttribute('aria-hidden', 'true');
    const calendar = document.createElementNS(namespace, 'rect');
    calendar.setAttribute('x', '16');
    calendar.setAttribute('y', '12');
    calendar.setAttribute('width', '48');
    calendar.setAttribute('height', '42');
    calendar.setAttribute('rx', '9');
    calendar.setAttribute('fill', 'currentColor');
    calendar.setAttribute('opacity', '.12');
    const outline = document.createElementNS(namespace, 'rect');
    outline.setAttribute('x', '16');
    outline.setAttribute('y', '12');
    outline.setAttribute('width', '48');
    outline.setAttribute('height', '42');
    outline.setAttribute('rx', '9');
    outline.setAttribute('fill', 'none');
    outline.setAttribute('stroke', 'currentColor');
    outline.setAttribute('stroke-width', '2');
    const detail = document.createElementNS(namespace, 'path');
    detail.setAttribute('d', 'M16 25h48M28 8v9M52 8v9M29 36h8m7 0h8M29 44h8');
    detail.setAttribute('fill', 'none');
    detail.setAttribute('stroke', 'currentColor');
    detail.setAttribute('stroke-width', '2');
    detail.setAttribute('stroke-linecap', 'round');
    svg.append(calendar, outline, detail);
    return svg;
}

export function eventTypeIcon(type) {
    const namespace = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(namespace, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('aria-hidden', 'true');
    if (type === 'PARTIDO') {
        const ball = document.createElementNS(namespace, 'circle');
        ball.setAttribute('cx', '12');
        ball.setAttribute('cy', '12');
        ball.setAttribute('r', '8.5');
        const seams = document.createElementNS(namespace, 'path');
        seams.setAttribute('d', 'M4.6 8.1c4.2.5 7.3 3.6 8.1 8.2M9.3 3.8c.5 4.2 3.6 7.2 8.2 8M5.2 17.8c3.1-1 5.2-3.1 6.1-6.2m2.8 8.1c1-3.2 3.2-5.3 6.3-6.2');
        svg.append(ball, seams);
    } else {
        const weight = document.createElementNS(namespace, 'path');
        weight.setAttribute('d', 'M6 8v8M4 10v4M18 8v8m2-6v4M6 12h12');
        svg.append(weight);
    }
    const wrapper = element('span', `event-type-icon ${type === 'PARTIDO' ? 'match' : 'training'}`);
    wrapper.setAttribute('aria-hidden', 'true');
    wrapper.append(svg);
    return wrapper;
}

export function userAvatar(user) {
    const name = String(user.nombre ?? '').trim();
    const parts = name.split(/\s+/).filter(Boolean);
    const initials = parts.length > 1
        ? `${parts[0][0]}${parts[parts.length - 1][0]}`
        : (parts[0] || '?').slice(0, 2);
    const identity = user.id ?? user.usuarioId;
    let tone;
    if (identity !== undefined && identity !== null && Number.isFinite(Number(identity))) {
        tone = Math.abs(Number(identity)) % 8;
    } else {
        tone = [...name].reduce((hash, character) => hash + character.charCodeAt(0), 0) % 8;
    }
    const avatar = element('span', `member-avatar avatar-tone-${tone}`, initials.toLocaleUpperCase('es'));
    avatar.setAttribute('aria-hidden', 'true');
    avatar.title = name;
    return avatar;
}

export function showFormError(banner, error, form) {
    banner.hidden = false;
    banner.classList.remove('ok');
    banner.replaceChildren(element('span', '', errorMessage(error)));
    if (form && error instanceof HuddleApiError && (error.status === 0 || error.status >= 500)) {
        banner.append(button('Reintentar', 'btn sec small-btn', () => form.requestSubmit()));
    }
    form?.querySelectorAll('[aria-invalid="true"]').forEach((input) => input.removeAttribute('aria-invalid'));
    if (error instanceof HuddleApiError && error.status === 400 && form) {
        const message = error.message.toLowerCase();
        const match = [...form.querySelectorAll('input, select, textarea')].find((input) => {
            const label = form.querySelector(`label[for="${CSS.escape(input.id)}"]`)?.textContent?.toLowerCase() || '';
            return message.includes(label.replace('*', '').trim()) || (message.includes('rival') && input.name === 'rival') ||
                (message.includes('fecha') && input.name === 'fechaHora');
        });
        if (match) {
            match.setAttribute('aria-invalid', 'true');
            match.focus();
            const errorText = match.closest('.field')?.querySelector('.field-error');
            if (errorText) errorText.textContent = error.message;
        }
    }
}

export function clearFormError(banner, form) {
    banner.hidden = true;
    banner.replaceChildren();
    form.querySelectorAll('[aria-invalid="true"]').forEach((input) => input.removeAttribute('aria-invalid'));
    form.querySelectorAll('.field-error').forEach((node) => { node.textContent = ''; });
}

export async function requireUser() {
    const user = readSession();
    if (!user || !Number.isInteger(Number(user.usuarioId)) || Number(user.usuarioId) < 1) {
        clearSession();
        window.location.replace('login.html');
        return null;
    }
    const current = { id: Number(user.usuarioId), nombre: user.nombre, email: user.email };
    mountAccountMenu(current);
    return current;
}

export function mountAccountMenu(user) {
    const toggle = document.getElementById('account-toggle');
    const dropdown = document.getElementById('account-dropdown');
    if (!toggle || !dropdown) return;

    const initials = user.nombre.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toLocaleUpperCase('es');
    document.getElementById('account-avatar').textContent = initials;
    document.getElementById('account-name').textContent = user.nombre;
    document.getElementById('account-profile-name').textContent = user.nombre;
    document.getElementById('account-profile-email').textContent = user.email;

    const close = () => {
        dropdown.hidden = true;
        toggle.setAttribute('aria-expanded', 'false');
    };
    toggle.addEventListener('click', () => {
        const opening = dropdown.hidden;
        dropdown.hidden = !opening;
        toggle.setAttribute('aria-expanded', String(opening));
    });
    document.addEventListener('click', (event) => {
        if (!event.target.closest('.account-menu')) close();
    });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') close();
    });
    dropdown.querySelectorAll('a').forEach((link) => link.addEventListener('click', close));
    document.getElementById('logout-button').addEventListener('click', () => {
        clearSession();
        window.location.assign('login.html');
    });
}

export async function withLoading(container, loader, render, label) {
    setLoading(container, label);
    try {
        const data = await loader();
        render(data);
    } catch (error) {
        showError(container, error, () => withLoading(container, loader, render, label));
    }
}

export function formatDateTime(value, options = {}) {
    const date = new Date(value);
    return new Intl.DateTimeFormat('es-ES', {
        weekday: options.short ? 'short' : 'long',
        day: 'numeric', month: 'short', year: options.year === false ? undefined : 'numeric',
        hour: '2-digit', minute: '2-digit'
    }).format(date).replace(',', ' ·');
}

export function dateParts(value) {
    const date = new Date(value);
    return {
        weekday: new Intl.DateTimeFormat('es-ES', { weekday: 'short' }).format(date).replace('.', ''),
        day: new Intl.DateTimeFormat('es-ES', { day: '2-digit' }).format(date),
        month: new Intl.DateTimeFormat('es-ES', { month: 'short' }).format(date).replace('.', '')
    };
}

export function roleLabel(role) {
    return role === 'ENTRENADOR' ? 'Entrenador' : 'Jugador';
}

export function statusChip(status) {
    const labels = { VOY: ['Voy', 'yes'], NO_VOY: ['No voy', 'no'], QUIZAS: ['Quizás', 'maybe'], SIN_RESPONDER: ['Sin responder', 'unanswered'] };
    const [label, style] = labels[status] || ['Sin responder', 'unanswered'];
    return element('span', `chip ${style}`, label);
}

export function countsRow(event) {
    const row = element('div', 'event-counts');
    row.append(element('span', 'chip yes', `Voy ${event.voy}`));
    row.append(element('span', 'chip no', `No voy ${event.noVoy}`));
    row.append(element('span', 'chip maybe', `Quizás ${event.quizas}`));
    row.append(element('span', 'chip unanswered', `Sin responder ${event.sinResponder}`));
    const values = [
        ['progress-yes', Number(event.voy) || 0],
        ['progress-no', Number(event.noVoy) || 0],
        ['progress-maybe', Number(event.quizas) || 0],
        ['progress-unanswered', Number(event.sinResponder) || 0]
    ];
    const total = values.reduce((sum, [, count]) => sum + count, 0);
    const progress = element('div', 'response-progress');
    progress.setAttribute('role', 'img');
    progress.setAttribute('aria-label', total ? `Distribución de ${total} respuestas` : 'Sin respuestas registradas');
    values.forEach(([className, count]) => {
        const segment = element('span', className);
        segment.style.width = total ? `${count / total * 100}%` : '0';
        progress.append(segment);
    });
    row.append(progress);
    return row;
}

export function setButtonSaving(buttonNode, saving, busyText = 'Guardando…', normalText = '') {
    if (saving) {
        buttonNode.dataset.originalText = buttonNode.textContent;
        buttonNode.textContent = busyText;
        buttonNode.disabled = true;
    } else {
        buttonNode.textContent = normalText || buttonNode.dataset.originalText || buttonNode.textContent;
        buttonNode.disabled = false;
    }
}

export function requireId(parameter) {
    const id = new URLSearchParams(location.search).get(parameter);
    if (!id || !/^\d+$/.test(id) || Number(id) < 1) {
        window.location.replace('equipos.html');
        return null;
    }
    return id;
}

export function confirmDelete(message) {
    return window.confirm(message);
}