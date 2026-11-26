import { api, saveSession } from './api.js';
import { clearFormError, element, setButtonSaving, showFormError } from './ui.js';

const form = document.getElementById('register-form');
const alertBox = document.getElementById('form-alert');
const button = document.getElementById('register-button');

form.addEventListener('input', () => clearFormError(alertBox, form));
form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFormError(alertBox, form);
    setButtonSaving(button, true);
    try {
        const user = await api.createUser({
            nombre: document.getElementById('register-name').value.trim(),
            email: document.getElementById('register-email').value.trim()
        });
        saveSession(user);
        window.location.assign('equipos.html');
    } catch (error) {
        showFormError(alertBox, error, form);
        if (error.status === 409) {
            const link = element('a', 'text-link', 'Inicia sesión con ese email.');
            link.href = 'login.html';
            alertBox.append(document.createTextNode(' '), link);
        }
    } finally {
        setButtonSaving(button, false, '', 'Crear cuenta');
    }
});