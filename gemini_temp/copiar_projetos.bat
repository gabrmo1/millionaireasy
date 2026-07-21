@echo off
cd /d "%~dp0"
setlocal EnableDelayedExpansion

for /d %%D in ("..\*") do (
    if /i not "%%~fD"=="%CD%" (
        set "IS_PROJECT=0"
        if exist "%%~fD\package.json" set "IS_PROJECT=1"
        if exist "%%~fD\pom.xml" set "IS_PROJECT=1"
        if exist "%%~fD\build.gradle" set "IS_PROJECT=1"
        if exist "%%~fD\build.gradle.kts" set "IS_PROJECT=1"

        if "!IS_PROJECT!"=="1" (
            set "FOLDER_NAME=%%~nxD"
            set "DEST_PATH=%CD%\!FOLDER_NAME!"

            if exist "!DEST_PATH!\" (
                rmdir /s /q "!DEST_PATH!" >nul 2>&1
                if not exist "!DEST_PATH!\" (
                    powershell -command "Write-Host 'Pasta antiga [!FOLDER_NAME!] excluida com sucesso!' -ForegroundColor Red"
                )
            )

            mkdir "!DEST_PATH!" >nul 2>&1
            
            robocopy "%%~fD" "!DEST_PATH!" /s /e /xd node_modules target .gradle .mvn build dist coverage .git .idea .vscode .vs /xf package-lock.json yarn.lock npm-debug.log yarn-debug.log yarn-error.log .env .env.* .DS_Store *.svg *.png *.ico *.jpg *.jpeg *.gif robots.txt manifest.json LICENSE vite-env.d.ts setupTests.ts .gitignore .dockerignore >nul 2>&1

            if exist "!DEST_PATH!\" (
                powershell -command "Write-Host 'Nova pasta [!FOLDER_NAME!] gerada com sucesso!' -ForegroundColor Green"
            )
        )
    )
)

timeout /t 1 /nobreak >nul
exit