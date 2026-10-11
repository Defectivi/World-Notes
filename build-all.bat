@echo off
setlocal
pushd "%~dp0"
for %%C in (legacy release snapshot) do (
    call "%~dp0gradlew.bat" build -Pchannel=%%C --console=plain
    if errorlevel 1 goto failed
)
call "%~dp0gradlew.bat" -p forge build --console=plain
if errorlevel 1 goto failed
popd
exit /b 0

:failed
set "worldnotesBuildExit=%errorlevel%"
popd
exit /b %worldnotesBuildExit%
