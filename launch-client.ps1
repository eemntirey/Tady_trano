$ErrorActionPreference = 'Stop'

# Lance le client JavaFX avec java directement (evite les problemes
# du plugin javafx-maven-plugin 0.0.8 avec JavaFX 27 / JDK 27).
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$m2 = 'C:\Users\eemntirey\.m2\repository'
$javafxPath = Join-Path $m2 'org\openjfx'

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java est introuvable. Installez un JDK 27 et ajoutez-le au PATH.'
}

$modules = @(
    "$javafxPath\javafx-base\27\javafx-base-27-win.jar",
    "$javafxPath\javafx-controls\27\javafx-controls-27-win.jar",
    "$javafxPath\javafx-fxml\27\javafx-fxml-27-win.jar",
    "$javafxPath\javafx-graphics\27\javafx-graphics-27-win.jar",
    "$javafxPath\javafx-web\27\javafx-web-27-win.jar",
    "$javafxPath\javafx-media\27\javafx-media-27-win.jar",
    "$javafxPath\jdk-jsobject\27\jdk-jsobject-27-win.jar"
)

$missing = $modules | Where-Object { -not (Test-Path $_) }
if ($missing) {
    throw 'Fichiers JavaFX introuvables : ' + $missing
}

$modulePath = ($modules -join ';')
$clientClasses = Join-Path $repoRoot 'tady-trano-client\target\classes'
$cpFile = Join-Path $repoRoot 'tady-trano-client\tady-trano-client\cp.txt'

if (-not (Test-Path -LiteralPath $cpFile -PathType Leaf)) {
    throw 'Classpath introuvable. Executer d''abord: mvn -pl tady-trano-client dependency:build-classpath -Dmdep.outputFile=tady-trano-client/tady-trano-client/cp.txt -Dmdep.includeScope=runtime'
}

$classPath = $clientClasses + ';' + (Get-Content $cpFile -Raw)

java --enable-native-access=ALL-UNNAMED `
    --module-path $modulePath `
    --add-modules jdk.jsobject,javafx.controls,javafx.fxml,javafx.web,javafx.media `
    -cp $classPath `
    mg.emit.tadytrano.client.TadyTranoApp