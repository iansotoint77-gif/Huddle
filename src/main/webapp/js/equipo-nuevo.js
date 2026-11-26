import { api } from './api.js';
import { requireUser, clearFormError, setButtonSaving, showFormError } from './ui.js';

const form = document.getElementById('team-form');
const alertBox = document.getElementById('form-alert');
const saveButton = document.getElementById('save-team');

form.addEventListener('input', () => clearFormError(alertBox, form));
form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFormError(alertBox, form);
    setButtonSaving(saveButton, true);
    try {
        const team = await api.createTeam({
            nombre: document.getElementById('team-name').value.trim(),
            categoria: document.getElementById('team-category').value.trim()
        });
        window.location.assign(`equipo.html?id=${encodeURIComponent(team.id)}`);
    } catch (error) {
        showFormError(alertBox, error, form);
    } finally {
        setButtonSaving(saveButton, false, '', 'Crear equipo');
    }
});

await requireUser();