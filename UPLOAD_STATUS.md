# Estado del repositorio

Repositorio: https://github.com/stroks15/CustomLauncher-AndroidTV-FireTV

## Estructura y compilación
- El proyecto Android principal está en la raíz: `app/`, `build.gradle.kts` y `settings.gradle.kts`.
- Los archivos Kotlin y recursos XML presentes en `app/src/main` forman la base del launcher.
- GitHub Actions compila el módulo raíz `:app` al cambiar los archivos del proyecto y publica el APK debug como artefacto si la compilación termina correctamente.
- La carpeta `customlauncher/` es una estructura auxiliar antigua y no es el módulo que compila el workflow actual.

## Pendiente de verificación
- Confirmar el resultado de la compilación en GitHub Actions.
- Revisar cualquier error de Kotlin, recursos o dependencias que reporte el trabajo.
- Probar el APK en Android TV y Fire TV Stick reales.

No se debe considerar el APK listo hasta que GitHub Actions termine en verde y se haya probado en un dispositivo.
