$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    foreach ($channel in @('legacy', 'release', 'snapshot')) {
        & .\gradlew.bat build "-Pchannel=$channel" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Build failed for $channel" }
    }
} finally {
    Pop-Location
}
