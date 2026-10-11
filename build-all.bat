@echo off
setlocal
pushd "%~dp0"
for %%C in (lts legacy release snapshot) do (
    call "%~dp0gradlew.bat" build -Pchannel=%%C --console=plain
    if errorlevel 1 goto failed
)
for %%C in (lts release) do (
    call "%~dp0gradlew.bat" -p forge build -Pchannel=%%C --console=plain
    if errorlevel 1 goto failed
)
for %%C in (lts release) do (
    call "%~dp0gradlew.bat" -p neoforge build -Pchannel=%%C --console=plain
    if errorlevel 1 goto failed
)
popd
exit /b 0

:failed
set "worldnotesBuildExit=%errorlevel%"
popd
exit /b %worldnotesBuildExit%
