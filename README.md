# Custom Launcher para Android TV y Fire TV

Proyecto Android de launcher para televisores Android TV y Amazon Fire TV Stick.

## Contenido de origen
El paquete de origen es `CustomLauncher_AndroidTV_FireTV_build.zip`. La estructura prevista del proyecto es:

```
customlauncher/
  build.gradle.kts
  settings.gradle.kts
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/com/tuusuario/customlauncher/
    src/main/res/
    src/main/assets/recomendaciones_launcher.json
  docs/adb_comandos.md
.github/workflows/build-android-tv.yml
BUILD_ANDROID_TV_FIRETV.md
```

> Nota: este README documenta la estructura del paquete original; no implica que todos esos archivos ya se hayan cargado a GitHub.

## Compilar
El proyecto usa Gradle Kotlin DSL y Android Gradle Plugin. Abre la carpeta `customlauncher` en Android Studio y sincroniza Gradle. Para generar el APK, ejecuta la tarea `assembleDebug` del módulo app.

## GitHub Actions
El workflow de compilación está en `.github/workflows/build-android-tv.yml`. Cuando el proyecto Android esté completo, la acción compilará y publicará el APK de depuración como artefacto.

## Dispositivos
El objetivo es Android TV y Fire TV Stick. La compatibilidad final depende de los requisitos del sistema, permisos y paquetes instalados en cada dispositivo.
