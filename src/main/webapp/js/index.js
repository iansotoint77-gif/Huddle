import { readSession } from './api.js';

window.location.replace(readSession() ? 'equipos.html' : 'login.html');