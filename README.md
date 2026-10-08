# Scholr — Academic Discovery

A complete Jetpack Compose Android app built around a **Premium Claymorphism** design
language: soft 3D tactile surfaces, dual-offset shadows, recessed wells, and procedurally
drawn clay illustrations.

Everything here compiles and runs with no backend and no binary assets — the illustrations,
loaders and app icon are all drawn from code.

---

## 1. Opening the project

1. **Android Studio → Open** → select this folder (`ADL Project`).
2. Let Gradle sync. Android Studio will fetch the Gradle distribution named in
   `gradle/wrapper/gradle-wrapper.properties`.
   *There is no `gradle-wrapper.jar` / `gradlew` in the folder* — it's a binary, so it
   isn't checked in here. Android Studio doesn't need it. To get CLI builds, run once:

```bash
gradle wrapper --gradle-version 9.7.1
```

3. Run the `app` configuration on an emulator or device.

**Toolchain** — pinned to match a current (2026) Android Studio install, so it syncs with
no "upgrade AGP" prompts and no JDK juggling:

| | |
|---|---|
| Gradle | 9.7.1 |
| Android Gradle Plugin | 9.3.1 |
| Kotlin | 2.4.10 |
| Compose BOM | 2026.08.00 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| JDK | 17+ (Android Studio's bundled JBR works as-is) |

`local.properties` points at the SDK and is machine-specific — regenerate or edit it if you
move the project.

---

## 2. The typeface

The app ships compiling on the platform sans so it runs immediately. To switch to
**Plus Jakarta Sans**:

1. Download it from Google Fonts.
2. Drop four static files into `app/src/main/res/font/` (lowercase, underscores only):
   `plus_jakarta_sans_medium.ttf`, `plus_jakarta_sans_semibold.ttf`,
   `plus_jakarta_sans_bold.ttf`, `plus_jakarta_sans_extrabold.ttf`
3. In [`ui/theme/Type.kt`](app/src/main/java/com/scholr/app/ui/theme/Type.kt), uncomment
   the `PlusJakartaSans` block and point `ScholrFontFamily` at it.

Nothing else changes — every text style in the app resolves through `ScholrFontFamily`.

---

## 3. How the clay effect actually works

This is the part worth understanding, because it is the whole design language.

Compose's built-in `Modifier.shadow()` casts **one** shadow from a fixed light source. Clay
needs **two** with independent offsets — a dark one pushed down-right and a white one
pulled up-left. So
[`ui/clay/ClayModifiers.kt`](app/src/main/java/com/scholr/app/ui/clay/ClayModifiers.kt)
drops to the framework `Canvas` and layers them by hand:

| Modifier | What it does |
|---|---|
| `clayExtruded(...)` | Dual outer shadow — `#A39BBE` @45% at +8dp, `#FFFFFF` @90% at −6dp |
| `clayInset(...)` | Soft inner shadow, drawn by clipping to the shape and casting its own outline inward |
| `claySurface(...)` | `clayExtruded` + clip + gradient face. Cards, bars, tiles |
| `claySurfaceSolid(...)` | Same, with a flat colour |
| `clayWell(...)` | Clip + recess gradient + `clayInset`. Search bars, bookmark buttons, chat composer |

Two things to know:

**Shadows draw outside the layout bounds.** `drawBehind` is not clipped to the composable's
box, which is exactly what lets the spill render — and exactly why every `LazyColumn` in the
app carries `contentPadding = 16.dp`. Without it the viewport shaves the shadows off and the
cards go flat.

**There is a fallback renderer.** `clayExtruded` rasterises the blur with
`Paint.setShadowLayer` by default (one draw call, perfect falloff). If you ever hit hardware
that refuses shadow layers on shapes, flip one global switch and every surface in the app
changes at once:

```kotlin
ClayConfig.renderer = ClayShadowRenderer.Stacked
```

`ClayConfig.depthScale` is also there if you want to demo the language at exaggerated depth.

---

## 4. Project map

```
app/src/main/java/com/scholr/app/
├── MainActivity.kt                 edge-to-edge host
├── ScholrApp.kt                    Scaffold + NavHost + bottom-bar shell
├── ScholrViewModel.kt              single source of truth (snapshot state, no Flow plumbing)
├── data/
│   ├── model/Models.kt             Paper, Conference, Interest, ChatMessage, FieldPalette
│   └── SampleData.kt               8 papers, 6 conferences, 12 interests — swap for a repo
├── navigation/Destinations.kt      routes + the 4 top-level destinations
└── ui/
    ├── theme/                      Color · Type · Theme · Dimens (radii + shadow geometry)
    ├── clay/                       ClayModifiers · ClayShapes · ClayComponents · ClayTextField
    ├── illustration/               ClayPrimitives · ClayIllustrations · KineticBackground · TestTubeLoader
    ├── components/                 ScholrHeader · ScholrBottomBar · PaperCard
    ├── screens/                    the 9 screens
    └── ScholrPreviews.kt           @Preview harness for the design system
```

---

## 5. The journey, screen by screen

| Screen | What was built |
|---|---|
| **Splash** | Scripted 2.4s beat. The mark springs in, a **horizontal test-tube loader** fills left→right behind a rippling meniscus with rising bubbles, a travelling glass shimmer and graduation ticks. Status line steps through three messages. |
| **Onboarding** | 3-slide carousel. Illustrations live in a `HorizontalPager` with a scale/rotate/alpha parallax falloff; the copy underneath swaps via `AnimatedContent` so the two layers move at different rates. Clay page indicator, green "Get started" on the last slide. |
| **Auth** | `KineticBackground` — 10 clay sprites (stars, pens, spheres, squircles, rings) drifting along **Lissajous** paths at independent speeds and phases, so nothing visibly loops. Sign In / Sign Up share one composable so the background never re-initialises when you flip between them. |
| **Interests** | 3-column grid. Selecting a tile literally **extrudes it further** — shadow offsets go 5→12dp, blur 10→22dp, and the face flips to purple clay, all spring-animated. Gated at 3 picks. |
| **Home** | `ScholrHeader`, greeting, horizontal quick-filter chip row, reading-streak panel with clay bars, then the "Recommended for you" feed of paper cards, then trending topics. |
| **Search** | Recessed search field, recent searches, trending suggestions, live results, and a designed empty state. |
| **Conferences** | Nearest-deadline alert, field tabs, and conference cards with an acronym badge, venue/dates, and a recessed deadline strip with an urgency dot. |
| **Library** | Clay segmented control (recessed track, extruded sliding thumb) over saved papers and collections. |
| **Paper detail** | Floating clay top bar, hero card with field pill and inset bookmark, stat tiles, abstract, key findings, tags, pinned action dock. |
| **AI Analysis** | `ModalBottomSheet` at **80% height** with a clay drag handle. Generated summary blocks and the conversation share one scroll; suggested-prompt chips and the composer are pinned below. Three-dot clay typing indicator while the model "thinks". |

---

## 6. Illustrations and animation

No PNGs. Every 3D form is composed from three ingredients in
[`ClayPrimitives.kt`](app/src/main/java/com/scholr/app/ui/illustration/ClayPrimitives.kt) —
an offset contact shadow, a radial gradient lit from the upper-left, and a tight specular
dot. `claySphere`, `claySquircle`, `clayStroke`, `clayStar` and `clayPen` are enough to build
the brain, folder and microscope, plus every floating sprite in the auth background.

Motion inventory:

- `InfiniteTransition` — kinetic background drift, illustration bob/breathe, logo dot, test-tube ripple and bubbles, typing dots
- `AnimatedContent` — onboarding copy transitions
- `animateDpAsState` / `animateFloatAsState` with springs — press depth on every clay control, tile extrusion, nav-pill lift, segmented thumb
- `AnimatedVisibility` — nav labels, tile checkmarks, bottom bar show/hide
- `graphicsLayer` — parallax, rotation and translation on floating shapes

---

## 7. Wiring a real backend

Two seams, both deliberate:

1. **Content** — `SampleData` is read only through `ScholrViewModel`. Replace those reads
   with a repository and no screen changes.
2. **AI** — `ScholrViewModel.answerFor()` is a deterministic stand-in. Replace its body with
   your API call; the sheet doesn't care where the string comes from. `aiThinking` already
   drives the typing indicator.
