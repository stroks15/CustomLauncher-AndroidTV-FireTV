# Kat & Liam h4ck Launcher

Launcher Leanback para Android TV y Amazon Fire Stick, pensado para adultos mayores: tarjetas gigantes, contraste alto,
foco con escala 1.15x y borde amarillo, y enrutador de voz hacia Kino TV (`com.arkiv.player.light`).

## Novedades de la versión 1.1
- **Nombre nuevo**: "Kat & Liam h4ck Launcher" (se cambia en `res/values/strings.xml`: `app_name` y `titulo_inicio`).
- **Barra superior** con el nombre del launcher a la izquierda y, a la misma altura, accesos directos a **Ajustes**, **WiFi** y la **hora** (respeta el formato 12/24 h del sistema).
- **Fila "▶️ CONTINUAR VIENDO"** justo debajo de los canales en vivo. Se llena sola cada vez que abres una película, serie, novela o documental y desaparece cuando no hay nada. Para quitar una tarjeta: mantén presionado el botón central → "Ya la terminé, quitar".
  - Limitación: Kino TV no avisa al launcher cuánto se vio ni si terminó, por eso la tarjeta se queda hasta que la quitas. Al abrirla, se vuelve a lanzar la misma entrada en Kino TV con el extra `continue_watching = true`.
  - No se registran canales en vivo ni música.

## Abrir y compilar
1. Abre esta carpeta en **Android Studio** (Koala o superior). Generará el Gradle Wrapper al sincronizar.
2. `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
3. Instala y fija el launcher: ver `docs/adb_comandos.md`.

## Estructura
| Archivo | Para qué sirve |
|---|---|
| `AndroidManifest.xml` | HOME + DEFAULT + LEANBACK_LAUNCHER, SearchManager (`SEARCH`, `MEDIA_PLAY_FROM_SEARCH`) |
| `MainActivity.kt` | Router de voz (script solicitado), barra superior (Ajustes / WiFi / hora), lanzamiento de apps y de contenido Kino |
| `HomeFragment.kt` | Filas Leanback 0–4 (incluye la fila "Continuar viendo") |
| `ContinuarViendo.kt` | Registro local de películas/series abiertas para "Continuar viendo" |
| `LauncherPrefs.kt` | SharedPreferences de las 4 apps de la Fila 0 y valores por defecto |
| `AppPickerDialog.kt` | AlertDialog con `getInstalledApplications()` |
| `KinoSync.kt` | Sincronización de historial en segundo plano (contrato ASUMIDO, ver abajo) |
| `assets/recomendaciones_launcher.json` | Filas, tarjetas y mapeo de los plugins |

## Cosas que debes completar / verificar
- **Logotipos**: no se incluyen (son marcas registradas). Pon tus archivos en `app/src/main/assets/logos/` con los nombres del JSON (`canal_5.png`, etc.). Sin archivo, la tarjeta muestra el nombre en letra gigante.
- **Contrato con Kino TV**: los extras `plugin_source` / `target_content` / `playback_mode` y el provider de historial son los de la especificación; verifica con la sección 7 de `docs/adb_comandos.md`.
- **Paquetes por defecto** de SmartTube, Settings y Media Player: ver `LauncherPrefs.CANDIDATAS_POR_DEFECTO`.
- **Acceso a WiFi en Fire OS**: usa `Settings.ACTION_WIFI_SETTINGS` y, si no existe, la pantalla de red de Fire OS (mejor esfuerzo) y por último Ajustes generales.
- **Tamaños**: si las 4 tarjetas de la Fila 0 no caben en tu pantalla, reduce `card_app_ancho` en `res/values/dimens.xml`.
