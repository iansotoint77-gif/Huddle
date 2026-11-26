import { api } from './api.js';
import { button, element, setButtonSaving, setLoading, showError, showFormError, clearFormError, showToast } from './ui.js';

const usersState = document.getElementById('users-state');
const userList = document.getElementById('user-list');
const enterButton = document.getElementById('enter-app');
const createForm = document.getElementById('create-user-form');
const createToggle = document.getElementById('toggle-create-user');
const formAlert = document.getElementById('form-alert');
let selectedId = localStorage.getItem('huddle.usuarioId');

function renderUsers(users) {
    usersState.replaceChildren();
    userList.replaceChildren();
    if (!users.length) {
        const empty = element('p', 'muted', 'Aún no hay usuarios. Crea el primero para empezar.');
        userList.append(empty);
    }
    if (!users.some((user) => String(user.id) === String(selectedId))) selectedId = null;
    users.forEach((user) => {
        const choice = button('', `user-choice${String(user.id) === String(selectedId) ? ' selected' : ''}`);
        choice.setAttribute('aria-pressed', String(String(user.id) === String(selectedId)));
        choice.append(element('span', 'user-dot'), element('strong', '', user.nombre), element('span', 'user-email', user.email));
        choice.addEventListener('click', () => {
            selectedId = String(user.id);
            renderUsers(users);
        });
        userList.append(choice);
    });
    enterButton.disabled = !selectedId;
}

async function loadUsers() {
    setLoading(usersState, 'Cargando usuarios…');
    userList.replaceChildren();
    try {
        renderUsers(await api.users());
    } catch (error) {
        showError(usersState, error, loadUsers);
    }
}

enterButton.addEventListener('click', () => {
    if (!selectedId) return;
    localStorage.setItem('huddle.usuarioId', selectedId);
    window.location.assign('equipos.html');
});

createToggle.addEventListener('click', () => {
    createForm.hidden = !createForm.hidden;
    if (!createForm.hidden) document.getElementById('user-name').focus();
});

document.getElementById('cancel-create-user').addEventListener('click', () => {
    createForm.reset();
    clearFormError(formAlert, createForm);
    createForm.hidden = true;
});

createForm.addEventListener('input', () => clearFormError(formAlert, createForm));
createForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFormError(formAlert, createForm);
    const saveButton = document.getElementById('save-user');
    setButtonSaving(saveButton, true, 'Guardando…');
    try {
        const user = await api.createUser({
            nombre: document.getElementById('user-name').value.trim(),
            email: document.getElementById('user-email').value.trim()
        });
        selectedId = String(user.id);
        createForm.reset();
        createForm.hidden = true;
        await loadUsers();
        showToast('Usuario creado correctamente. Selecciónalo y pulsa Entrar.');
    } catch (error) {
        showFormError(formAlert, error, createForm);
    } finally {
        setButtonSaving(saveButton, false, '', 'Crear usuario');
    }
});

loadUsers();