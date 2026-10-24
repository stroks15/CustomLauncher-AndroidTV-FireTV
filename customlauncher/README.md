# Custom Launcher TV — "Mi Televisión"

Launcher Leanback para Android TV y Amazon Fire Stick, pensado para adultos mayores: tarjetas gigantes, contraste alto, foco con escala y borde visible, y enrutador de voz hacia Kino TV (`com.arkiv.player.light`).

## Abrir y compilar
Abre esta carpeta en Android Studio y sincroniza Gradle. Ejecuta `gradle assembleDebug` o la tarea `assembleDebug` del módulo app. El APK se genera normalmente en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura de fuentes prevista
- `app/src/main/AndroidManifest.xml`: declaración del launcher HOME y búsqueda.
- `app/src/main/java/com/tuusuario/customlauncher/`: fuentes Kotlin.
- `app/src/main/res/`: layouts, colores, temas e iconos.
- `app/src/main/assets/recomendaciones_launcher.json`: recomendaciones y configuración de filas.
- `docs/adb_comandos.md`: comandos de instalación y configuración ADB.

## Pendiente
Cargar los archivos Kotlin y recursos originales restantes del ZIP de construcción. Sin esos archivos no se puede compilar una APK funcional.
