export class HuddleApiError extends Error {
    constructor(status, message, error = '') {
        super(message);
        this.name = 'HuddleApiError';
        this.status = status;
        this.error = error;
    }
}

export const SESSION_KEY = 'huddle.session';

export function readSession() {
    try {
        const session = JSON.parse(localStorage.getItem(SESSION_KEY));
        return session?.usuarioId && session.nombre && session.email ? session : null;
    } catch {
        return null;
    }
}

export function saveSession(user) {
    const session = { usuarioId: user.id, nombre: user.nombre, email: user.email };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    return session;
}

export function clearSession() {
    localStorage.removeItem(SESSION_KEY);
    localStorage.removeItem('huddle.usuarioId');
    localStorage.removeItem('huddle.nombre');
    localStorage.removeItem('huddle.email');
}

async function request(path, { method = 'GET', body } = {}) {
    const headers = { Accept: 'application/json' };
    const session = readSession();
    if (session) headers['X-Usuario-Id'] = String(session.usuarioId);
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let response;
    try {
        response = await fetch(path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body)
        });
    } catch (cause) {
        throw new HuddleApiError(0, 'No se pudo conectar con Huddle. Comprueba tu conexión e inténtalo de nuevo.', cause);
    }

    if (response.status === 204) return null;
    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = null;
        }
    }
    if (!response.ok) {
        const message = data?.message || `La solicitud no se pudo completar (HTTP ${response.status}).`;
        throw new HuddleApiError(response.status, message, data?.error || '');
    }
    return data;
}

export const api = {
    users: () => request('/api/usuarios'),
    userByEmail: (email) => request(`/api/usuarios?email=${encodeURIComponent(email)}`),
    createUser: (body) => request('/api/usuarios', { method: 'POST', body }),
    updateUser: (id, body) => request(`/api/usuarios/${encodeURIComponent(id)}`, { method: 'PUT', body }),
    teams: () => request('/api/equipos'),
    team: (id) => request(`/api/equipos/${encodeURIComponent(id)}`),
    createTeam: (body) => request('/api/equipos', { method: 'POST', body }),
    members: (teamId) => request(`/api/equipos/${encodeURIComponent(teamId)}/miembros`),
    addMember: (teamId, body) => request(`/api/equipos/${encodeURIComponent(teamId)}/miembros`, { method: 'POST', body }),
    updateMember: (teamId, userId, body) => request(`/api/equipos/${encodeURIComponent(teamId)}/miembros/${encodeURIComponent(userId)}`, { method: 'PATCH', body }),
    deleteMember: (teamId, userId) => request(`/api/equipos/${encodeURIComponent(teamId)}/miembros/${encodeURIComponent(userId)}`, { method: 'DELETE' }),
    teamEvents: (teamId, time) => request(`/api/equipos/${encodeURIComponent(teamId)}/eventos?tiempo=${encodeURIComponent(time)}`),
    event: (id) => request(`/api/eventos/${encodeURIComponent(id)}`),
    createEvent: (teamId, body) => request(`/api/equipos/${encodeURIComponent(teamId)}/eventos`, { method: 'POST', body }),
    updateEvent: (id, body) => request(`/api/eventos/${encodeURIComponent(id)}`, { method: 'PUT', body }),
    deleteEvent: (id) => request(`/api/eventos/${encodeURIComponent(id)}`, { method: 'DELETE' }),
    attendance: (id) => request(`/api/eventos/${encodeURIComponent(id)}/respuestas`),
    saveResponse: (id, body) => request(`/api/eventos/${encodeURIComponent(id)}/respuesta`, { method: 'PUT', body })
};