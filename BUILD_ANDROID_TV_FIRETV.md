# Guía de compilación: Android TV / Fire TV Stick

## Preparación
1. Instala Android Studio y un JDK compatible con la versión de Gradle del proyecto.
2. Abre la carpeta `customlauncher` (no la raíz del repositorio si el proyecto Android está dentro de ella).
3. Espera la sincronización de Gradle y acepta la instalación de los SDK requeridos.
4. Ejecuta la tarea `app > Tasks > build > assembleDebug`.

## Resultado
El APK de depuración normalmente se genera en:

```
customlauncher/app/build/outputs/apk/debug/app-debug.apk
```

## Instalación por ADB
Con depuración ADB habilitada en el dispositivo:

```bash
adb connect DIRECCION_IP:5555
adb install -r app-debug.apk
```

En Fire TV, activa las opciones de desarrollador y la depuración ADB desde Configuración. Mantén el acceso ADB protegido y desactívalo cuando termines.

## Estado del repositorio
Esta guía y el workflow pueden prepararse desde GitHub, pero para obtener un APK compilable es indispensable subir también los archivos fuente y recursos reales del ZIP original. No se debe considerar una compilación exitosa hasta que GitHub Actions termine en verde.
