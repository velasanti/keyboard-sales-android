# Status Checks Requeridos en main

Verificado contra la API de GitHub el 2026-08-19. Nombres EXACTOS de los
checks requeridos en la proteccion de rama de main:

- Tokens y contraste
- ktlint y Android Lint
- Licencia y avisos
- Build y peso del APK

enforce_admins esta activo: nadie, ni el owner, puede mergear a main sin
que estos cuatro pasen.

## Si renombras un job en los workflows

Si el name de un job en .github/workflows cambia, el check requerido
apunta a un nombre que ya no existe y el PR queda BLOQUEADO (no pasa en
silencio, se traba).

Antes de renombrar un job:
1. Actualiza la proteccion de rama con el nombre nuevo en el mismo PR.
2. Actualiza este archivo.

Comando para verificar el estado actual:
gh api repos/velasanti/keyboard-sales-android/branches/main/protection

## Reversa de emergencia

Si un PR queda bloqueado por un check huerfano:
gh api -X DELETE repos/velasanti/keyboard-sales-android/branches/main/protection/enforce_admins

Arreglas el nombre del check, y reactivas con:
gh api -X POST repos/velasanti/keyboard-sales-android/branches/main/protection/enforce_admins
