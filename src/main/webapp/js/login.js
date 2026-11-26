import { api, saveSession } from './api.js';
import { clearFormError, element, setButtonSaving, showFormError } from './ui.js';

const form = document.getElementById('login-form');
const alertBox = document.getElementById('form-alert');
const button = document.getElementById('login-button');

form.addEventListener('input', () => clearFormError(alertBox, form));
form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFormError(alertBox, form);
    const email = document.getElementById('login-email');
    if (!email.value.trim()) {
        email.setAttribute('aria-invalid', 'true');
        email.closest('.field').querySelector('.field-error').textContent = 'El email es obligatorio.';
        email.focus();
        return;
    }
    setButtonSaving(button, true, 'Comprobando…');
    try {
        const user = await api.userByEmail(email.value.trim());
        saveSession(user);
        window.location.assign('equipos.html');
    } catch (error) {
        showFormError(alertBox, error, form);
        if (error.status === 404) {
            const link = element('a', 'text-link', 'Regístrate para crear una cuenta.');
            link.href = 'registro.html';
            alertBox.append(document.createTextNode(' '), link);
        }
    } finally {
        setButtonSaving(button, false, '', 'Entrar');
    }
});