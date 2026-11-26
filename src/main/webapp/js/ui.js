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
    const card = element('div', 'skeleton-card');
    card.setAttribute('aria-label', label);
    card.setAttribute('role', 'status');
    card.append(element('div', 'skeleton'), element('div', 'skeleton w80'), element('div', 'skeleton w60'), element('p', 'muted small', label));
    container.append(card);
}

export function showEmpty(container, title, description, actionText, action) {
    container.replaceChildren();
    const empty = element('section', 'empty');
    empty.append(element('h2', '', title), element('p', 'muted', description));
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
        day: new Intl.DateTimeFormat('es-ES', { day: '2-digit' }).format(date)
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