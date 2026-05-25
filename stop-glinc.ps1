Write-Host ""
@(
    @{ Port = 3001; Name = "Bridge" },
    @{ Port = 8080; Name = "Backend" },
    @{ Port = 8100; Name = "Frontend" }
) | ForEach-Object {
    $svc  = $_
    $conn = Get-NetTCPConnection -LocalPort $svc.Port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force
        Write-Host "  $($svc.Name) :$($svc.Port) detenido (PID $($conn.OwningProcess))"
    } else {
        Write-Host "  $($svc.Name) :$($svc.Port) ya estaba libre"
    }
}
Write-Host ""
