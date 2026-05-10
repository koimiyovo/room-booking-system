param([Parameter(Mandatory)][string]$Command)

switch ($Command) {
    "run"     { mvn spring-boot:run -pl bootstrap }
    "run-dev" { mvn spring-boot:run -pl bootstrap "-Dspring.profiles.active=dev" }
    "stop"    {
        $conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        if ($conn) {
            Stop-Process -Id $conn.OwningProcess -Force
            Write-Host "Application stopped."
        } else {
            Write-Host "No process found on port 8080."
        }
    }
    "build"   { mvn clean package "-DskipTests" }
    "test"    { mvn clean test }
    default   { Write-Host "Usage: .\scripts.ps1 <run|run-dev|stop|build|test>" }
}
