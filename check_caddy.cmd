@echo off
tasklist /FI "IMAGENAME eq caddy.exe" 2>NUL | find /I /N "caddy.exe">NUL
if "%ERRORLEVEL%"=="0" echo Caddy server is running