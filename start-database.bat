@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ========================================================================
echo  PostgreSQL local (sin Docker)
echo ========================================================================
echo.
echo El backend espera una instancia de PostgreSQL en tu maquina, gestionada
echo con pgAdmin, DBeaver o el servicio de Windows de PostgreSQL.
echo.
echo 1) Asegurate de que el servicio PostgreSQL este en ejecucion
echo    ^(Servicios de Windows / pgAdmin: conexion al servidor local^).
echo.
echo 2) Crea la base de datos si no existe ^(nombre debe coincidir con
echo    application.yaml en backend-management-service^):
echo.
echo    Base de datos:  tourpresence
echo    Usuario:        synexis
echo    Contrasena:     synexis123
echo    Puerto:         5432
echo    Host:           localhost
echo.
echo 3) En pgAdmin: clic derecho en Databases - Create - Database...
echo    o ejecuta: CREATE DATABASE tourpresence;
echo    y crea el rol/usuario synexis con la contrasena indicada si aun no existe.
echo.
echo ------------------------------------------------------------------------
echo  La API Spring Boot usa el puerto 8080 ^(Postman, apps moviles^).
echo  PostgreSQL usa el puerto 5432 ^(no son el mismo puerto^).
echo ------------------------------------------------------------------------
echo.
echo Para arrancar solo el backend:  start-backend.bat
echo.
pause
endlocal
