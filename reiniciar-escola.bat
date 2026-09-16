@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set PROJETO=C:\ProjetosEclipse\escola
set PORTA=8080
set JAR_NOME=escola-0.0.1-SNAPSHOT.jar
set LOG=%PROJETO%\iniciar.log

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERRO] Execute como Administrador.
    pause
    exit /b 1
)

echo ============================================================
echo  Reiniciando ESCOLA na porta %PORTA%
echo ============================================================
echo.

if not exist "%PROJETO%\pom.xml" (
    echo [ERRO] pom.xml nao encontrado em %PROJETO%
    pause
    exit /b 1
)

set ENCONTRADO=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%PORTA% ^| findstr LISTENING') do (
    echo Encerrando PID %%a...
    taskkill /PID %%a /F >nul 2>&1
    if !errorlevel! equ 0 (
        echo   [OK] PID %%a encerrado.
        set ENCONTRADO=1
    ) else (
        echo   [AVISO] Nao foi possivel encerrar PID %%a.
    )
)

if !ENCONTRADO! equ 1 (
    echo Porta %PORTA% liberada.
    timeout /t 2 /nobreak >nul
) else (
    echo Nenhum processo na porta %PORTA%.
)

echo.
echo Compilando com Maven...
cd /d "%PROJETO%"
call "%PROJETO%\mvnw.cmd" -q clean package -DskipTests

if %errorlevel% neq 0 (
    echo [ERRO] Falha ao compilar.
    pause
    exit /b 1
)

set JAR=%PROJETO%\target\%JAR_NOME%
if not exist "%JAR%" (
    echo [ERRO] JAR nao encontrado: %JAR%
    pause
    exit /b 1
)

echo.
echo Iniciando: %JAR%
start "EscolaApp" cmd /c "java -jar "%JAR%" > "%LOG%" 2>&1"

echo.
echo ============================================================
echo  Aplicacao iniciada!
echo  Local:   http://localhost:%PORTA%
echo  Celular: http://SEU_IP:%PORTA%
echo  Log:     %LOG%
echo ============================================================
timeout /t 5 /nobreak >nul
exit /b 0