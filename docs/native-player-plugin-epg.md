# Reproductor Media3, plugins y EPG

## Arquitectura

- `player/CustomTvPlayer.kt`: Media3 ExoPlayer, HLS y DASH.
- `player/PlayerActivity.kt` + `res/layout/activity_player.xml`: pantalla completa, mando a distancia, liberación del reproductor.
- `utils/VoiceCommandValidator.kt`: exige que la orden de voz termine con la palabra `dexter`.
- `PluginRegistry.kt` + `assets/plugin_sources.json`: registro declarativo de fuentes independientes.
- `epg/EpgRepository.kt`: descarga asíncrona y parsea XMLTV o JSON de programación actual.
- `assets/recomendaciones_launcher.json`: filas y tarjetas; las tres filas destacadas empiezan por La Rosa de Guadalupe, Miénteme / Lie to Me y Candy Candy.

## Añadir un stream autorizado

En el elemento deseado de `recomendaciones_launcher.json`, añade una URL HLS/DASH explícita en `extras`:

```json
"extras": {
  "stream_url": "https://TU-DOMINIO-AUTORIZADO/ruta/master.m3u8",
  "epg_channel_id": "ID-EXACTO-DEL-CANAL-EN-XMLTV"
}
```

El ejemplo es una plantilla, no una URL real. No se incluyen URLs de canales, servicios de suscripción ni endpoints inventados. Para miniaturas dinámicas, añade también `"logo": "https://TU-DOMINIO-AUTORIZADO/miniatura.jpg"` al objeto del elemento (o una ruta como `logos/canal.png` dentro de assets); Glide aplica caché de memoria y disco. Para reproducir contenido protegido, el proveedor debe autorizar el acceso y exponer un stream compatible; Media3 no elimina DRM ni restricciones de acceso.

## Configurar la guía EPG

En `assets/plugin_sources.json`, asigna `epg_feed_url` a una URL HTTP(S) de XMLTV o JSON que tengas permiso de consultar. El JSON admite un array raíz o un objeto con `programmes`; cada programa usa `channel_id` (o `channel`), `title`, `start` y `end`. XMLTV usa elementos `programme` con atributos `channel`, `start`, `stop` y un elemento `title`.

El valor `epg_channel_id` de cada tarjeta debe coincidir exactamente con el ID real del feed. Si no hay feed o coincidencia, se muestra el subtítulo estático del catálogo.

## Activación de voz

Ejemplos de frases:

- `Pon Canal 5 dexter`
- `Abre La Rosa de Guadalupe dexter`
- `Reproduce Candy Candy dexter`

La palabra `dexter` es sólo una frase de activación. **No es una firma criptográfica ni autentica al hablante**; alguien que conozca el sufijo puede usarlo. La actividad de voz del sistema debe entregar el texto a `ACTION_SEARCH` o `MEDIA_PLAY_FROM_SEARCH`.

## Latencia

Los buffers se ajustan a 1 s para inicio y 1.5 s para rebuffer, con 15–30 s de buffer objetivo. Eso no garantiza “cero lag” ni menos de un segundo de zapping: la latencia real depende de la configuración HLS/DASH del proveedor, longitud de segmentos, CDN, red y DRM.

## Compatibilidad y compilación

Media3 1.5.1 requiere Android compatible con la API mínima del proyecto. Ejecuta `gradle --no-daemon :app:assembleDebug` con JDK 17. Prueba en dispositivo real tanto el control remoto como streams HLS/DASH de prueba autorizados.
