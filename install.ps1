# ============================================================================
#  GymManager - Instalador para Windows
#
#  Instala la última versión publicada en GitHub Releases:
#    - Verifica que haya Java 21+ (lo instala con winget si falta)
#    - Descarga GymManager.jar a %LOCALAPPDATA%\GymManager
#    - Crea accesos directos en el Escritorio y el Menú Inicio
#
#  Uso (PowerShell):
#    irm https://raw.githubusercontent.com/OscardMQ/GymManager/main/install.ps1 | iex
# ============================================================================

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Repo       = "OscardMQ/GymManager"
$InstallDir = Join-Path $env:LOCALAPPDATA "GymManager"
$JarUrl     = "https://github.com/$Repo/releases/latest/download/GymManager.jar"

Write-Host ""
Write-Host "  GymManager - Instalador  " -BackgroundColor DarkBlue -ForegroundColor White
Write-Host ""

# ── 1. Verificar Java 21 o superior ─────────────────────────────────────────
function Get-JavaMajorVersion {
    try {
        $salida = & java -version 2>&1 | Out-String
        $match  = [regex]::Match($salida, 'version "(\d+)')
        if ($match.Success) { return [int]$match.Groups[1].Value }
    } catch {}
    return 0
}

$javaMajor = Get-JavaMajorVersion
if ($javaMajor -ge 21) {
    Write-Host "[1/3] Java $javaMajor detectado." -ForegroundColor Green
} else {
    Write-Host "[1/3] Java 21+ no encontrado. Instalando Eclipse Temurin 21 con winget..." -ForegroundColor Yellow
    winget install --id EclipseAdoptium.Temurin.21.JRE -e --silent `
        --accept-package-agreements --accept-source-agreements
    # Refrescar PATH de esta sesión para encontrar el Java recién instalado
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                [Environment]::GetEnvironmentVariable("Path", "User")
    $javaMajor = Get-JavaMajorVersion
    if ($javaMajor -lt 21) {
        Write-Host "No se pudo instalar Java automaticamente." -ForegroundColor Red
        Write-Host "Instala Java 21+ desde https://adoptium.net y vuelve a ejecutar este script."
        exit 1
    }
    Write-Host "      Java $javaMajor instalado correctamente." -ForegroundColor Green
}

# ── 2. Descargar el JAR de la última release ────────────────────────────────
Write-Host "[2/3] Descargando GymManager..."
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
Invoke-WebRequest -Uri $JarUrl -OutFile (Join-Path $InstallDir "GymManager.jar") -UseBasicParsing
$mb = [math]::Round((Get-Item (Join-Path $InstallDir "GymManager.jar")).Length / 1MB, 1)
Write-Host "      Descargado en $InstallDir ($mb MB)" -ForegroundColor Green

# ── 3. Crear accesos directos (javaw = sin ventana de consola) ──────────────
Write-Host "[3/3] Creando accesos directos..."
$javaw = $null
try { $javaw = (Get-Command javaw -ErrorAction Stop).Source } catch {}
if (-not $javaw) { $javaw = (Get-Command java).Source }   # último recurso

$shell = New-Object -ComObject WScript.Shell
$rutas = @(
    (Join-Path ([Environment]::GetFolderPath("Desktop")) "GymManager.lnk"),
    (Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\GymManager.lnk")
)
foreach ($ruta in $rutas) {
    $lnk = $shell.CreateShortcut($ruta)
    $lnk.TargetPath       = $javaw
    $lnk.Arguments        = "-jar `"$(Join-Path $InstallDir 'GymManager.jar')`""
    $lnk.WorkingDirectory = $InstallDir
    $lnk.Description      = "GymManager - Sistema de Gestion de Gimnasio"
    $lnk.Save()
}
Write-Host "      Accesos directos creados (Escritorio y Menu Inicio)." -ForegroundColor Green

Write-Host ""
Write-Host "Instalacion completada." -ForegroundColor Green
Write-Host "Abre 'GymManager' desde el Menu Inicio o el acceso directo del Escritorio."
Write-Host ""
Write-Host "Para desinstalar: borra la carpeta $InstallDir, los accesos directos"
Write-Host "y la carpeta de datos $env:USERPROFILE\.gymmanager (contiene la base de datos)."
