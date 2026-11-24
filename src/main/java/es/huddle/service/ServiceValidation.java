package es.huddle.service;

import es.huddle.exception.ValidationException;

final class ServiceValidation {
    private ServiceValidation() {
    }

    static String required(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("El campo " + field + " es obligatorio.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ValidationException("El campo " + field + " supera la longitud máxima de " + maxLength + ".");
        }
        return normalized;
    }

    static String email(String value) {
        String email = required(value, "email", 254);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ValidationException("El email no tiene un formato válido.");
        }
        return email.toLowerCase(java.util.Locale.ROOT);
    }

    static int dorsal(Integer dorsal) {
        if (dorsal == null || dorsal < 0 || dorsal > 99) {
            throw new ValidationException("El dorsal debe estar entre 0 y 99.");
        }
        return dorsal;
    }
}