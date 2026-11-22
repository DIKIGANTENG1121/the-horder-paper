@echo off
setlocal
echo === Membangun HordesPaper untuk Paper 26.3 (JDK 25) ===
where gradle >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Gradle belum terpasang di PATH.
    echo Alternatif termudah tanpa install: upload folder ini ke GitHub dan jalankan tab Actions!
    pause
    exit /b 1
)
gradle build :neon-testkit:build
if %errorlevel% neq 0 (
    echo [ERROR] Build gagal. Kirim pesan error di atas ke chat untuk perbaikan.
    pause
    exit /b 1
)
echo === SUKSES! File plugin JAR ada di: build\libs\HordesPaper-1.6.3f-paper26.3.jar ===
pause
