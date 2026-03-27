@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ========================================================================
echo  Base de datos H2 ^(embebida^)
echo ========================================================================
echo.
echo El backend usa **H2 en archivo** ^(no hace falta instalar PostgreSQL^).
echo Los datos se guardan en: backend-management-service\data\synaxis.mv.db
echo cuando corres start-backend.bat desde la raiz del proyecto.
echo.
echo Consola web H2 ^(con el API en marcha^):
echo   http://localhost:8080/h2-console
echo.
echo JDBC URL en la consola ^(copiar tal cual^):
echo   jdbc:h2:file:./data/synaxis
echo   Usuario: sa    Contrasena: ^(dejar vacia^)
echo.
echo ------------------------------------------------------------------------
echo  La API REST usa el puerto 8080.
echo ------------------------------------------------------------------------
echo.
echo Para arrancar el backend:  start-backend.bat
echo.
pause
endlocal
