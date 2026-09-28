<div align="center">

<img src="docs/icon.png" width="112" alt="MyNFC icon" />

# MyNFC

**Read, write, save and export NFC tags — with a UI that doesn't look like 2012.**

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)
![Target SDK](https://img.shields.io/badge/targetSdk-36-3DDC84?logo=android&logoColor=white)
![No ads](https://img.shields.io/badge/ads-none-10B981)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

MyNFC is an Android NFC toolkit built with Jetpack Compose. Tap a tag to see everything about it — chip type, UID, memory, and decoded NDEF records — then save it to your library, export it as JSON, or write a copy onto another tag. Compose links, contacts, Wi‑Fi credentials and more, and write them to any NDEF tag in one tap.

No ads, no accounts, no network permission. Your data stays on your device.

## Screenshots

| Home | Scan | Tag details | Advanced info |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/01-home.png" width="200" /> | <img src="docs/screenshots/02-scan.png" width="200" /> | <img src="docs/screenshots/04-tag-details.png" width="200" /> | <img src="docs/screenshots/05-tag-advanced.png" width="200" /> |

| Saved tags | Contact tag | My records | Add new record |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/03-saved.png" width="200" /> | <img src="docs/screenshots/06-contact-tag.png" width="200" /> | <img src="docs/screenshots/07-my-records.png" width="200" /> | <img src="docs/screenshots/08-add-new.png" width="200" /> |

| Record editor | Writing to a tag | Tools | Light theme |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/09-editor-wifi.png" width="200" /> | <img src="docs/screenshots/10-write-sheet.png" width="200" /> | <img src="docs/screenshots/11-advanced-tools.png" width="200" /> | <img src="docs/screenshots/12-home-light.png" width="200" /> |

## Features

### 📡 Read
- Scan any tag from anywhere in the app — details open automatically
- Identifies the chip: **NTAG210/212/213/215/216**, **MIFARE Ultralight / EV1 / C**, **MIFARE Classic 1K/4K**, ISO 14443‑4, ISO 14443‑3B, **FeliCa**, ISO 15693
- UID, manufacturer (ISO/IEC 7816‑6), technologies, NDEF type, memory used / total
- Status: writable, lockable, NDEF‑formatable
- Chip info: ATQA, SAK, max transceive length, NXP `GET_VERSION` fields (vendor, product type, storage size…), MIFARE sectors/blocks, ISO‑DEP historical bytes, NfcB/NfcF/NfcV parameters
- Decodes NDEF records into readable content: links, text, phone, email, SMS, geo, **Wi‑Fi (WSC)**, **vCard**, Android app records, smart posters, MIME and external types
- **Simple / Advanced** views — Advanced shows TNF, type, and raw payload hex for every record
- One‑tap **Open** (links, calls, maps, apps) and **Copy** on every record

### ✍️ Write
17 record types, grouped by category:

| Category | Types |
|---|---|
| Web & Text | Web link, Text |
| Contact & Business | Business card (vCard), Phone, Email, SMS |
| Network & Automation | Wi‑Fi (WPA/WPA2/Open), Location, Launch app |
| Social | Instagram, LinkedIn, Telegram, WhatsApp, YouTube, X, Facebook, TikTok |

- Inline validation, live size estimate and "fits on NTAG213/215/216" hint
- Save records to **My Records** and write them again any time
- Long‑press to select several records and write them to **one tag** as a multi‑record message
- Formats blank `NdefFormatable` tags automatically on first write

### 🔖 Save & 📦 Export
- Save scanned tags with a custom name; rename or delete later
- Search your library by name, UID, chip type or content
- **Export JSON** — a single tag or your whole library (tags + records) via the system file picker
- **Import JSON** — merges an export back in, skipping duplicates
- Share / copy a human‑readable report of any tag

### 🛠 Tools
- **Copy a tag** — read a source tag, then write an identical copy to another (byte‑for‑byte NDEF clone)
- **Erase**, **Format**, and **Lock** (permanently read‑only, with confirmation)

### 🎨 Design
- Material 3, custom light & dark themes, edge‑to‑edge
- Animated radar scan, bottom‑sheet write flow with success/error states and haptics
- Reader mode keeps tags inside the app — no system chooser popping up while you work

## JSON export format

Exports are plain, pretty‑printed JSON — the same format the app stores internally, so nothing is lost on a round trip.

```jsonc
{
  "app": "MyNFC",
  "formatVersion": 1,
  "exportedAt": 1790596946515,
  "tags": [
    {
      "id": "…",
      "scannedAt": 1790000000000,
      "name": "Portfolio card",
      "uid": "04:A2:3B:1C:7F:61:80",
      "tagType": "NTAG215",
      "technologies": ["NfcA", "MifareUltralight", "Ndef"],
      "manufacturer": "NXP Semiconductors",
      "ndefType": "NFC Forum Type 2",
      "maxSize": 492,
      "usedSize": 58,
      "writable": true,
      "canMakeReadOnly": true,
      "formatable": false,
      "records": [
        {
          "kind": "URL",
          "tnf": 1,
          "tnfName": "NFC Well Known",
          "type": "U",
          "typeHex": "55",
          "idHex": "",
          "payloadHex": "046769746875622E636F6D2F…",
          "value": "https://github.com/…",
          "mimeType": null,
          "sizeBytes": 26
        }
      ],
      "chipInfo": [
        { "label": "ATQA", "value": "0x0044" },
        { "label": "SAK", "value": "0x00" }
      ]
    }
  ],
  "records": [
    {
      "id": "…",
      "type": "WIFI",
      "fields": { "ssid": "Cafe Guest", "auth": "WPA/WPA2", "password": "…" },
      "createdAt": 1790000000000,
      "updatedAt": 1790000000000
    }
  ]
}
```

Each record keeps its raw `tnf` / `typeHex` / `idHex` / `payloadHex`, so a saved or exported tag can be written back exactly as it was read.

> ⚠️ Wi‑Fi records store the password in plain text, both on the tag and in exports. Treat export files accordingly.

## Getting started

### Requirements
- A recent Android Studio that supports **AGP 8.13** (Narwhal or newer), with **JDK 17**
- An Android 8.0+ (API 26) device **with NFC** — emulators can't read tags
- A few NFC tags to play with (NTAG213/215/216 stickers are cheap and work great)

### Build & run

```bash
git clone https://github.com/YohannesTz/MyNFC.git
cd MyNFC
./gradlew installDebug
```

Or open the project in Android Studio and hit **Run**. No API keys or extra setup are needed.

### Release build

```bash
./gradlew assembleRelease
```

Configure a signing config in `app/build.gradle.kts` (or sign via **Build → Generate Signed Bundle / APK**) before publishing.

### Tests

The NDEF encode/decode logic is covered by instrumented tests (they use the real `android.nfc` classes, so they run on a device):

```bash
./gradlew connectedDebugAndroidTest
```

They check round trips for every record type, Wi‑Fi/vCard encoding, byte‑exact cloning, validation rules and JSON export/import.

## Architecture

Single‑activity Compose app, no DI framework — a small `AppContainer` wires everything together.

```
app/src/main/java/com/github/yohannestz/mynfc/
├── MainActivity.kt          # Enables NFC reader mode, forwards tags to NfcController
├── MyNfcApplication.kt      # AppContainer (repository + controller)
├── nfc/
│   ├── NfcController.kt     # Routes each tag: read, or run the pending write/erase/lock/format/clone
│   ├── TagReader.kt         # Tag → ScannedTag (chip introspection, GET_VERSION, NDEF)
│   ├── TagWriter.kt         # Write / erase / lock / format with friendly errors
│   ├── NdefParser.kt        # NdefRecord → readable NdefRecordInfo (and back, for cloning)
│   ├── RecordFactory.kt     # User records → NdefRecord, validation, summaries
│   ├── WifiCodec.kt         # Wi‑Fi Simple Configuration TLV encode/decode
│   └── Bytes.kt             # Hex helpers, IC manufacturer table
├── data/
│   ├── TagRepository.kt     # JSON‑file persistence exposed as StateFlows; import/export
│   └── model/               # ScannedTag, NdefRecordInfo, WriteRecord, RecordType, ExportBundle
└── ui/
    ├── MyNfcAppRoot.kt      # Navigation (type‑safe routes), bottom bar, global NFC session sheet
    ├── home/ scan/ detail/ write/ saved/ tools/ session/
    ├── components/          # Gradient cards, segmented toggle, radar pulse, info cards…
    ├── common/              # Icons/colors per record type, SAF export/import, intents
    └── theme/               # Colors, gradients, typography, shapes
```

**How a tap flows:** reader mode delivers the `Tag` on a binder thread → `NfcController` checks the current session. If it's idle, it reads the tag and emits it, and the UI navigates to its details. If a write operation is pending, it runs it and updates the session state, and the bottom sheet shows the progress, then success or error.

### Tech stack

| | |
|---|---|
| UI | Jetpack Compose, Material 3, Material Icons Extended |
| Navigation | Navigation Compose 2.8 (type‑safe routes) |
| State | Kotlin Coroutines, StateFlow, `collectAsStateWithLifecycle` |
| Serialization | kotlinx.serialization (JSON) |
| NFC | `android.nfc` reader mode — `Ndef`, `NdefFormatable`, `NfcA/B/F/V`, `IsoDep`, `MifareClassic`, `MifareUltralight` |
| Build | AGP 8.13, Kotlin 2.0.21, Gradle version catalog |

## Permissions & privacy

| Permission | Why |
|---|---|
| `android.permission.NFC` | Read and write tags |
| `android.permission.VIBRATE` | Haptic feedback when a write finishes |

MyNFC has **no internet permission**. Saved tags and records live in the app's private storage. Data only leaves the device when you export or share it yourself.

## Roadmap ideas

- [ ] Password‑protect NTAG21x tags (PWD_AUTH)
- [ ] Raw memory dump for Ultralight/NTAG pages
- [ ] Home‑screen widget / quick settings tile to start a scan
- [ ] CSV export
- [ ] Localizations

## Contributing

Issues and pull requests are welcome. Please describe the tag model you tested with when reporting read/write bugs.

## License

MyNFC is released under the [MIT License](LICENSE).
