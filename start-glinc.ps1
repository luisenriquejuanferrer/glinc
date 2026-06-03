$root     = $PSScriptRoot
$bridge   = Join-Path $root "cgm-bridge-service"
$backend  = Join-Path $root "glinc-backend"
$frontend = Join-Path $root "glinc-frontend"

Write-Host ""
Write-Host "  Glinc iniciando..."
Write-Host "  Bridge   -> http://localhost:3001  (Swagger: /docs)"
Write-Host "  Backend  -> http://localhost:8080"
Write-Host "  Frontend -> http://localhost:8100"
Write-Host ""

if (Get-Command wt.exe -ErrorAction SilentlyContinue) {
    # Pasar como array evita que PowerShell parta el ';' como separador de comandos.
    $wtArgs = @(
        "new-tab", "--title", "Bridge :3001", "-d", $bridge,
            "powershell", "-NoExit", "-Command", "npm run dev",
        ";",
        "new-tab", "--title", "Backend :8080", "-d", $backend,
            "powershell", "-NoExit", "-Command", ".\mvnw.cmd spring-boot:run",
        ";",
        "new-tab", "--title", "Frontend :8100", "-d", $frontend,
            "powershell", "-NoExit", "-Command", "npm start -- --port 8100"
    )
    & wt.exe @wtArgs
} else {
    Start-Process powershell -ArgumentList @("-NoExit", "-Command", "Set-Location '$bridge'; npm run dev")
    Start-Process powershell -ArgumentList @("-NoExit", "-Command", "Set-Location '$backend'; .\mvnw.cmd spring-boot:run")
    Start-Process powershell -ArgumentList @("-NoExit", "-Command", "Set-Location '$frontend'; npm start -- --port 8100")
}
