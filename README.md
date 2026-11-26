# Huddle

## Descripción

## Capturas de la aplicación

## Modelo de datos

## Arquitectura de contenedores

## Arquitectura por capas

## Configuración y ejecución

## Usuarios y credenciales de prueba

## API REST

## Ejecución de tests

## Persistencia de datos

## Limitaciones conocidas y decisiones de diseño

El acceso es una **sesión simulada sin contraseña ni autenticación real**. El login identifica la cuenta por email; el navegador guarda `usuarioId`, `nombre` y `email` en `localStorage` y `api.js` envía `usuarioId` en la cabecera `X-Usuario-Id`. Cerrar sesión elimina esos datos del navegador. Esta demostración no protege frente a suplantación de identidad y no debe utilizarse como autenticación en producción.
