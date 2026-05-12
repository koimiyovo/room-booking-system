param([Parameter(Mandatory)][string]$Command)

switch ($Command) {

    # ── Maven ────────────────────────────────────────────────────────────────

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

    # ── Docker ───────────────────────────────────────────────────────────────

    "docker-build"   { docker compose build }
    "docker-up"      { docker compose up --build }
    "docker-up-dev"  {
        $env:SPRING_PROFILES_ACTIVE = "dev"
        try { docker compose up --build }
        finally { Remove-Item Env:\SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue }
    }
    "docker-down"    { docker compose down }
    "docker-down-v"  { docker compose down -v }
    "docker-logs"    { docker compose logs -f app }

    default {
        Write-Host "Usage: .\scripts.ps1 <command>"
        Write-Host ""
        Write-Host "Maven:"
        Write-Host "  run            Start the application"
        Write-Host "  run-dev        Start with dev profile (seeds sample data)"
        Write-Host "  stop           Kill the process on port 8080"
        Write-Host "  build          Package without tests"
        Write-Host "  test           Run all tests"
        Write-Host ""
        Write-Host "Docker:"
        Write-Host "  docker-build   Build the Docker image"
        Write-Host "  docker-up      Build and start all containers"
        Write-Host "  docker-up-dev  Build and start with dev profile (seeds sample data)"
        Write-Host "  docker-down    Stop and remove containers"
        Write-Host "  docker-down-v  Stop and remove containers + Postgres volume"
        Write-Host "  docker-logs    Follow application logs"
    }
}
