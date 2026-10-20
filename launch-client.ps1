$ErrorActionPreference = 'Stop'

# Lance le client JavaFX depuis la racine du dépôt, sans dépendre d'un
# chemin Windows ou d'un répertoire Eclipse propres à une machine.
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$pomPath = Join-Path $repoRoot 'pom.xml'

if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
    throw "Impossible de trouver le pom.xml racine dans '$repoRoot'."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven (mvn) est introuvable. Installez Maven et ajoutez-le au PATH."
}

Push-Location $repoRoot
try {
    mvn -pl tady-trano-client javafx:run
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $exitCode
