import { api, saveSession } from './api.js';
import { clearFormError, requireUser, setButtonSaving, showFormError } from './ui.js';

const form = document.getElementById('settings-form');
const alertBox = document.getElementById('form-alert');
const button = document.getElementById('save-settings');
const nameInput = document.getElementById('profile-name');

form.addEventListener('input', () => clearFormError(alertBox, form));
form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFormError(alertBox, form);
    setButtonSaving(button, true);
    try {
        const updated = await api.updateUser(form.dataset.userId, { nombre: nameInput.value.trim() });
        const session = saveSession(updated);
        const initials = session.nombre.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toLocaleUpperCase('es');
        document.getElementById('account-avatar').textContent = initials;
        document.getElementById('account-name').textContent = session.nombre;
        document.getElementById('account-profile-name').textContent = session.nombre;
        nameInput.value = session.nombre;
        alertBox.hidden = false;
        alertBox.classList.add('ok');
        alertBox.textContent = 'Tu perfil se ha actualizado correctamente.';
    } catch (error) {
        showFormError(alertBox, error, form);
    } finally {
        setButtonSaving(button, false, '', 'Guardar cambios');
    }
});

const user = await requireUser();
if (user) {
    form.dataset.userId = String(user.id);
    nameInput.value = user.nombre;
    document.getElementById('profile-email').textContent = user.email;
}