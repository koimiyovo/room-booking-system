param([Parameter(Mandatory)][string]$Command)

function Show-Help {
    $version = (mvn help:evaluate '-Dexpression=project.version' -q '-DforceStdout').Trim()
    Write-Host ""
    Write-Host "Usage: .\scripts.ps1 <command>"
    Write-Host ""
    Write-Host "Maven (backend):"
    Write-Host "  run            Start the application"
    Write-Host "  run-dev        Start with dev profile (seeds sample data)"
    Write-Host "  stop           Kill the process on port 8080"
    Write-Host "  build          Package without tests"
    Write-Host "  test           Run all tests"
    Write-Host ""
    Write-Host "Frontend:"
    Write-Host "  fe-dev         Start the Vite dev server (http://localhost:5173)"
    Write-Host "  fe-test        Run frontend tests"
    Write-Host "  fe-build       Build the frontend for production"
    Write-Host ""
    Write-Host "Full stack:"
    Write-Host "  dev            Start backend (dev profile) in a new window + frontend here"
    Write-Host ""
    Write-Host "Docker:"
    Write-Host "  docker-build   Build the Docker image"
    Write-Host "  docker-up      Build and start all containers"
    Write-Host "  docker-up-dev  Build and start with dev profile (seeds sample data)"
    Write-Host "  docker-down    Stop and remove containers"
    Write-Host "  docker-down-v  Stop and remove containers + Postgres volume"
    Write-Host "  docker-logs    Follow application logs"
    Write-Host ""
    Write-Host "Release (current: $version):"
    Write-Host "  release-patch  bump patch  e.g. $version -> x.y.Z+1  (bug fix)"
    Write-Host "  release-minor  bump minor  e.g. $version -> x.Y+1.0  (new feature)"
    Write-Host "  release-major  bump major  e.g. $version -> X+1.0.0  (breaking change)"
    Write-Host ""
}

function Invoke-Release {
    param([string]$NewVersion)
    Write-Host "Releasing v$NewVersion..."
    mvn versions:set "-DnewVersion=$NewVersion" '-DgenerateBackupPoms=false'
    git add pom.xml domain/pom.xml infrastructure-api/pom.xml infrastructure-persistence/pom.xml infrastructure-provider/pom.xml bootstrap/pom.xml
    git commit -m "chore: release v$NewVersion"
    git tag "v$NewVersion"
    $env:APP_VERSION = $NewVersion
    try {
        docker compose build
        docker tag "room-booking-system:$NewVersion" 'room-booking-system:latest'
    } finally {
        Remove-Item Env:\APP_VERSION -ErrorAction SilentlyContinue
    }
    Write-Host ""
    Write-Host "Released v$NewVersion -- push when ready: git push; git push --tags"
}

function Import-EnvFile {
    param([string]$Path)
    if (Test-Path $Path) {
        Get-Content $Path | ForEach-Object {
            if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)\s*$') {
                [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
            }
        }
    }
}

switch ($Command) {

    # -- Maven -------------------------------------------------------------------

    "run"     { mvn install -DskipTests; if ($?) { mvn spring-boot:run -pl bootstrap } }
    "run-dev" {
        Import-EnvFile ".env.dev"
        mvn install -DskipTests; if ($?) { mvn spring-boot:run -pl bootstrap "-Dspring-boot.run.profiles=dev" }
    }
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

    # -- Frontend ----------------------------------------------------------------

    "fe-dev"   { Push-Location frontend; try { npm run dev   } finally { Pop-Location } }
    "fe-test"  { Push-Location frontend; try { npm test      } finally { Pop-Location } }
    "fe-build" { Push-Location frontend; try { npm run build } finally { Pop-Location } }

    # -- Full stack --------------------------------------------------------------

    "dev" {
        Import-EnvFile ".env.dev"
        $root = $PSScriptRoot
        Start-Process powershell -ArgumentList '-NoExit', '-Command', `
            "Set-Location '$root'; mvn install -DskipTests; if (`$?) { mvn spring-boot:run -pl bootstrap '-Dspring-boot.run.profiles=dev' }"
        Push-Location frontend
        try { npm run dev } finally { Pop-Location }
    }

    # -- Docker ------------------------------------------------------------------

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

    # -- Release -----------------------------------------------------------------

    "release-patch" {
        $version = (mvn help:evaluate '-Dexpression=project.version' -q '-DforceStdout').Trim()
        $parts = $version.Split('.')
        Invoke-Release "$($parts[0]).$($parts[1]).$([int]$parts[2]+1)"
    }
    "release-minor" {
        $version = (mvn help:evaluate '-Dexpression=project.version' -q '-DforceStdout').Trim()
        $parts = $version.Split('.')
        Invoke-Release "$($parts[0]).$([int]$parts[1]+1).0"
    }
    "release-major" {
        $version = (mvn help:evaluate '-Dexpression=project.version' -q '-DforceStdout').Trim()
        $parts = $version.Split('.')
        Invoke-Release "$([int]$parts[0]+1).0.0"
    }

    "help"  { Show-Help }
    default { Show-Help }
}
