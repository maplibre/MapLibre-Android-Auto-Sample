# GameMaps

Android map prototype with an original GTA-inspired minimap style, based on
[MapLibre-Android-Auto-Sample](https://github.com/maplibre/MapLibre-Android-Auto-Sample).

The first implementation replaces the demo map on the phone and car with a shared,
high-contrast street map, a GPS position arrow, course tracking and a Recenter action.
It is a development prototype: routing, destination search, voice guidance, offline
map downloads and Apple CarPlay are not implemented.

## Run it

Open the project root in Android Studio. Use **JDK 17**, install **Android SDK 35**,
sync Gradle, select the `app` configuration and run it on an Android 10+ phone.
Allow location access (precise location is recommended for driving).
The bundled style still needs internet access for map tiles and fonts.

```sh
./gradlew :app:assembleDebug :automotive:assembleDebug :car_common:testDebugUnitTest
./gradlew :app:lintDebug :automotive:lintDebug :car_common:lintDebug
```

On Windows use `gradlew.bat` in Command Prompt or `./gradlew.bat` in PowerShell.
See [the first-step guide](docs/STEP_1.md) for Android Auto / DHU setup and checks.

## Structure

| Module / file | Responsibility |
| --- | --- |
| `app` | Phone preview, permissions and Recenter button; Android Auto entry package |
| `automotive` | Separate Android Automotive OS package, reusing the shared renderer |
| `car_common` | Car templates, MapLibre rendering, style and location helpers |
| `car_common/src/main/assets/styles/gta-inspired.json` | Shared Style Specification v8 JSON; colors, roads, buildings and labels |
| `GameMapStyle.kt` | One asset URI and initial camera for all map views |
| `GameMapLocation.kt` | Permission-aware MapLibre location activation and camera follow |
| `MyCarAppService.kt` | Host validation and foreground location notification |
| `CarMapRenderer.kt` / `CarMapContainer.kt` | VirtualDisplay + Presentation, surface and MapView lifecycle |

The car map uses the sample's **VirtualDisplay + Presentation** pipeline. It does
not use the old bitmap-copy loop described in earlier upstream documentation.
Surface replacement, session shutdown and MapView disposal release their owned
resources; the MapView is not removed from an unrelated WindowManager.

## Maps and location

The original style uses [OpenFreeMap](https://openfreemap.org/quick_start/)
OpenMapTiles-compatible vector tiles. No API key is required. Tiles and fonts are
online resources; the project does not bundle or promise offline coverage.
Attribution remains visible on the car display and available in MapLibre on the phone.

Location is activated only after permission and style loading. Approximate permission
is accepted, but its accuracy may be insufficient for a useful driving view. The
MapLibre engine supplies the position; the camera follows GPS course rather than
the phone's compass. Panning lets you inspect the map; **Recenter** restores follow.
No position is sent to an application backend or saved as a trip by this prototype.
The tile provider receives map requests necessary to display the viewed area.

On the car, the existing CarAppService runs as a location foreground service while
the map session is visible. If a host or Android version rejects that transition,
Recenter asks you to open the phone app and retry. This still needs validation on
real hosts, including with the phone screen off. The phone preview alone stops
location updates when its MapView stops.

## Validation status

Static JSON/XML/TOML and diff checks were performed for this implementation.
The Android build was attempted but stopped before compilation because this editing
environment cannot download Gradle and has no Android SDK. No APK, emulator, DHU,
real-device GPS, rendering or smoothness result is claimed. Run the commands and
manual scenarios in [STEP_1.md](docs/STEP_1.md) before treating the milestone as verified.
The existing example unit tests are not a substitute for those scenarios.

## Credits and license

Code: [MIT](LICENSE), retaining the upstream MapLibre contributors' license.
Upstream sample contributors include Flitsmeister, MapLibre and VietMap.
Map data: OpenStreetMap contributors; tiles: OpenFreeMap / OpenMapTiles.
The visual style and arrow in this fork are original assets; no GTA game files or
logos are included. This project is not affiliated with Rockstar Games.
