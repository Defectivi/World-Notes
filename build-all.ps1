$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    foreach ($channel in @('lts', 'legacy', 'release', 'snapshot')) {
        & .\gradlew.bat build "-Pchannel=$channel" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Build failed for $channel" }
    }
    foreach ($channel in @('lts', 'release')) {
        & .\gradlew.bat -p forge build "-Pchannel=$channel" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Build failed for Forge $channel" }
    }
    foreach ($channel in @('lts', 'release')) {
        & .\gradlew.bat -p neoforge build "-Pchannel=$channel" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Build failed for NeoForge $channel" }
    }
} finally {
    Pop-Location
}
