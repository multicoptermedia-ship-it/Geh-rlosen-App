# Gespräch Live

Android-App für Live-Untertitel in Gesprächen – mit dem Ziel, gehörlosen und schwerhörigen Menschen die Teilnahme an gesprochenen Gesprächen zu erleichtern.

## Kernanforderungen

- **Offline zuerst:** Transkription soll ohne Internet funktionieren.
- **Datenschutz:** Audio soll für die Offline-Transkription auf dem Gerät bleiben.
- **Geringe Verzögerung:** Text erscheint möglichst schon während des Sprechens.
- **Gute Lesbarkeit:** große Schrift und reduzierte Bedienung.
- **Robust bei Umgebungslärm:** Pegelanzeige, Empfindlichkeit, Noise Suppression und VAD.

## Aktueller Prototyp

Der aktuelle Stand verwendet Androids `SpeechRecognizer`: Start/Stop, Deutsch, Zwischenergebnisse, Gesprächsverlauf und große Textdarstellung.

> Wichtig: Androids `SpeechRecognizer` garantiert nicht auf jedem Gerät vollständigen Offline-Betrieb. Er bleibt deshalb nur die Prototyp-Engine.

## Zielarchitektur

```text
Mikrofon
   ↓
direkte PCM-Audioaufnahme
   ↓
Rauschunterdrückung / Pegelanpassung
   ↓
Voice Activity Detection (VAD)
   ↓
lokale Speech-to-Text-Engine
   ↓
Live-Untertitel
```

## Mikrofon und Hintergrundgeräusche

Ein Lautstärkeregler allein trennt Sprache nicht von Störgeräuschen. Vorgesehen sind deshalb:

1. **Pegelanzeige** – zeigt den aktuellen Mikrofoneingang.
2. **Empfindlichkeit** – steuert die spätere Sprach-/Aktivitätsschwelle.
3. **Noise Suppression** – reduziert geeignete Hintergrundgeräusche.
4. **VAD** – erkennt Sprachabschnitte und Stille.
5. **Originalsignal** – Filter können deaktiviert werden, falls sie leise Sprecher beeinträchtigen.

## Technik

- Kotlin
- Jetpack Compose
- Material 3
- minSdk 26
- Android `SpeechRecognizer` nur als temporärer Prototyp

## Nächste Schritte

- direkte PCM-Mikrofonaufnahme
- lokale Offline-STT-Engine integrieren und mit Deutsch testen
- echte Noise Suppression / Audio-Vorverarbeitung
- Voice Activity Detection
- kontinuierliche Transkription
- Mikrofonpegel und Empfindlichkeitssteuerung
- automatische Scrollposition
- einstellbare Schriftgröße und Kontrast
- Tablet-Optimierung
- Offline-Test ohne WLAN und Mobilfunk

## Status

Früher Prototyp / Version 0.2 in Entwicklung. Noch nicht für produktiven oder barrierefreiheitskritischen Einsatz freigegeben.
