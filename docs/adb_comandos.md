# Comandos ADB — Custom Launcher TV

Paquete: `com.tuusuario.customlauncher` · Activity: `.MainActivity` · Kino TV: `com.arkiv.player.light`

> Sustituye `192.168.1.50` por la IP de tu TV / Fire Stick.
> En Fire Stick: **Ajustes → Mi Fire TV → Opciones para desarrolladores → Depuración ADB** (y "Apps de orígenes desconocidos").
> En Android TV: activa **Opciones de desarrollador → Depuración por USB/red**.

---

## 1. Conectar

```bash
adb kill-server
adb start-server
adb connect 192.168.1.50:5555
adb devices
```

## 2. Compilar e instalar

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 3. Averiguar los paquetes reales de las apps por defecto (Fila 0)

```bash
adb shell pm list packages | grep -i -E "arkiv|smarttube|teamsmart|liskovsoft|settings|vlc|mxtech"
```

Si alguno difiere de `LauncherPrefs.CANDIDATAS_POR_DEFECTO`, o lo agregas a esa lista o lo eliges en el equipo con el botón central sostenido.

## 4. Fijar el launcher como pantalla de inicio

### 4A. Android TV / Google TV (Android 10 o superior)

```bash
adb shell cmd package set-home-activity com.tuusuario.customlauncher/.MainActivity
```

Verificar quién atiende HOME:

```bash
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

### 4B. Fire TV Stick (Fire OS) — EXPERIMENTAL

Fire OS no tiene selector de launcher predeterminado y `set-home-activity` normalmente no funciona. El método habitual es desactivar el launcher de Amazon:

```bash
# Desactivar el launcher de Amazon (se puede revertir; ver sección 8)
adb shell pm disable-user --user 0 com.amazon.tv.launcher

# Abrir el nuevo launcher
adb shell am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.MAIN -c android.intent.category.HOME
```

Advertencias:
- En versiones recientes de Fire OS el sistema puede reactivarlo solo o dejar la pantalla en negro: ten a mano el comando de reversión (sección 8) y una conexión ADB funcional **antes** de desactivar nada.
- Alternativa sin riesgo: no reemplazar el launcher y reasignar el botón HOME con una app de mapeo de botones (p. ej. "Button Mapper") para abrir este launcher.

## 5. Iniciar manualmente y ver el registro

```bash
adb shell am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.MAIN -c android.intent.category.HOME
adb logcat -c
adb logcat -s CustomLauncherVoice KinoSync LauncherContent
```

## 6. Emular comandos de voz (testing del SpeechRecognitionRouter)

Formato (las comillas internas evitan que ADB parta la frase):

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'FRASE AQUI'"
```

> Si tu terminal (sobre todo Windows) no envía bien los acentos, prueba la frase sin ellos; el router los elimina de todos modos.

### 6.1 Plugin `xuper` — televisión abierta

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'pon las estrellas'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'canal 5'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'pon el 5'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'n mas'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'multimedios'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'canal siete'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'azteca uno'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'el gourmet'"
```

### 6.2 Plugin `pluto` — Enlace y Azteca -1 hora

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'pon enlace'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'azteca uno menos una hora'"
```

### 6.3 Plugin `cultural`

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'canal once'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'canal veintidos'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'tv unam'"
```

### 6.4 Plugin `archive` — cine de oro

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'pon pedro infante'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'ver cantinflas'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'jorge negrete'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'cine de oro'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'peliculas viejas'"
```

### 6.5 Plugins `novelas`, `docs`, `retro-tv`, `comedia`, `regionales`, `audios`

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'quiero ver una telenovela'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'un documental de naturaleza'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'series de antes'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'algo de comedia'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'canal del estado'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'pon unos boleros'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'musica ranchera'"
```

### 6.6 Fallback (búsqueda global de Kino)

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'quiero ver el partido de futbol'"
```

### 6.7 Variante de asistentes de voz (MEDIA_PLAY_FROM_SEARCH → se normaliza a ACTION_SEARCH)

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.media.action.MEDIA_PLAY_FROM_SEARCH --es query 'canal 22'"
```

### 6.8 Simular el botón de búsqueda/micrófono del control (abre el dictado integrado si el equipo lo tiene)

```bash
adb shell input keyevent KEYCODE_SEARCH
```

## 7. Verificar que Kino TV entiende los intents (CRÍTICO)

El router asume que Kino TV acepta `ACTION_VIEW` con los extras `plugin_source`, `target_content` y `playback_mode`. Compruébalo:

```bash
# Filtros de intent que declara Kino TV
adb shell dumpsys package com.arkiv.player.light | grep -i -B2 -A12 "Activity Resolver Table"

# Prueba directa del contrato, sin pasar por el launcher
adb shell "am start -a android.intent.action.VIEW -p com.arkiv.player.light --es plugin_source xuper --es target_content 'Canal 5' --es playback_mode live"
```

- Si el segundo comando responde `Error: Activity not started, unable to resolve Intent`, Kino TV **no** declara un filtro `VIEW` sin datos. El launcher entonces usa el plan B (abre Kino TV por su icono llevando los extras), pero Kino debe leerlos en su actividad principal.
- Si abre Kino pero no cambia de canal, el contrato real de extras es distinto: ajusta `plugin_source`/`target_content`/`playback_mode` en el JSON y en `MainActivity`.

Historial para la Fila 3 (provider asumido):

```bash
adb shell content query --uri content://com.arkiv.player.light.provider/history
```

## 8. Pruebas de la interfaz con el control remoto

```bash
adb shell input keyevent KEYCODE_HOME               # HOME → vuelve a la Fila 0
adb shell input keyevent KEYCODE_DPAD_RIGHT         # mover foco
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_CENTER        # clic corto: abrir la tarjeta
adb shell input keyevent --longpress KEYCODE_DPAD_CENTER   # clic largo: abrir selector de apps (Fila 0)
```

Revisar lo guardado en SharedPreferences (app de debug):

```bash
adb shell run-as com.tuusuario.customlauncher cat shared_prefs/launcher_prefs.xml
```

## 9. Revertir

```bash
# Fire OS: reactivar el launcher de Amazon
adb shell pm enable com.amazon.tv.launcher

# Android TV: dejar que el sistema vuelva a elegir su launcher
adb shell pm clear com.google.android.tvlauncher
adb uninstall com.tuusuario.customlauncher
```


### 6.9 Historial de YouTube / Shorts

```bash
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'abre el historial de YouTube'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'videos vistos'"
adb shell "am start -n com.tuusuario.customlauncher/.MainActivity -a android.intent.action.SEARCH --es query 'shorts'"
```
