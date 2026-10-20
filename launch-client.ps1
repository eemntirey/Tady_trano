$javafxPath = 'C:\Users\eemntirey\.m2\repository\org\openjfx'
$modules = @(
    "$javafxPath\javafx-base\27\javafx-base-27-win.jar",
    "$javafxPath\javafx-controls\27\javafx-controls-27-win.jar",
    "$javafxPath\javafx-fxml\27\javafx-fxml-27-win.jar",
    "$javafxPath\javafx-graphics\27\javafx-graphics-27-win.jar",
    "$javafxPath\javafx-web\27\javafx-web-27-win.jar",
    "$javafxPath\javafx-media\27\javafx-media-27-win.jar",
    "$javafxPath\jdk-jsobject\27\jdk-jsobject-27-win.jar"
)
$modulePath = ($modules -join ';')
$clientClasses = 'c:\Users\eemntirey\eclipse-workspace\Tady_trano\tady-trano-client\target\classes'
$cpFile = 'c:\Users\eemntirey\eclipse-workspace\Tady_trano\tady-trano-client\tady-trano-client\cp.txt'
$classPath = $clientClasses + ';' + (Get-Content $cpFile -Raw)

java --enable-native-access=ALL-UNNAMED --module-path $modulePath --add-modules jdk.jsobject,javafx.controls,javafx.fxml,javafx.web,javafx.media -cp $classPath mg.emit.tadytrano.client.TadyTranoApp