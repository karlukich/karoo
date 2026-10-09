# Karoo Pantallas · versión de prueba 1B / 1C

Extensión para Hammerhead Karoo con una pantalla de datos a página completa, en dos versiones:

| Campo en el Karoo | Qué es |
|---|---|
| **1B · Ruta (oscuro)** | Fondo negro |
| **1C · Ruta (claro)** | Fondo claro, para pleno sol |

Datos: velocidad (con media), potencia 3 s con su zona y la normalizada, pulso con su zona, pendiente coloreada según inclinación, distancia y desnivel acumulado. Las zonas, el peso y las unidades salen del perfil del Karoo.

La pantalla se dibuja como una imagen (con la tipografía Barlow Condensed incluida en `app/src/main/assets/fonts`, licencia OFL) para que se vea igual que la maqueta, y se refresca una vez por segundo.

## Compilar
GitHub Actions compila solo al subir los cambios (pestaña **Actions**); el APK queda en **Releases**.
También se puede abrir la carpeta con Android Studio y usar *Build → Build APK(s)*.

## Instalar en el Karoo
Con la app Hammerhead Companion: en el móvil, mantener pulsado el enlace `karoo-pantallas.apk` de la página de Releases → Compartir → Hammerhead Companion → **Install** en el Karoo.

Desde esta versión el APK se firma siempre con la misma clave (`app/karoo.keystore`), así que las siguientes actualizaciones se instalan encima sin desinstalar.

## Ponerla en pantalla
Perfiles → página nueva de **un solo campo** → toca el campo → **Pantallas** → 1B o 1C.

## Estructura
```
app/src/main/kotlin/com/carlos/karoopantallas/
├── PantallasExtension.kt   Registra 1B y 1C en el Karoo
├── RutaScreen.kt           Recoge los datos y envía la imagen al Karoo cada segundo
├── render/RutaRenderer.kt  Dibuja la pantalla (diseño 480 × 620)
├── render/Palette.kt       Colores de la versión oscura y la clara
└── data/                   Suscripción a datos, unidades y formatos
karoo-ext/                  SDK oficial de Hammerhead (v1.1.9, Apache 2.0)
```
