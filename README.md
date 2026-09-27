# Gespräch Live

Android-App für möglichst unmittelbare Live-Untertitel in Gesprächen. Ziel ist eine Bedienung, die auch in Stresssituationen möglichst wenig Interaktion verlangt.

## Bedienprinzip: Öffnen → Lesen

Nach der einmaligen Android-Mikrofonfreigabe soll der normale Ablauf nur noch sein:

1. App antippen.
2. Die App beginnt automatisch zuzuhören.
3. Gesprochener Text erscheint groß auf dem Bildschirm.

Es ist **kein Flugmodus** nötig. Eine vorhandene Internetverbindung darf bestehen, aber die Zielarchitektur benötigt sie für die Transkription nicht. WLAN- oder Mobilfunkausfall soll den Gesprächsmodus nicht beeinflussen.

Auf dem Hauptbildschirm bleiben bewusst nur die für das Gespräch wichtigen Elemente: großer Live-Text, ein klarer Hörstatus, Pause/Weiter und Text löschen. Technische Optionen wie Empfindlichkeit oder Noise Suppression gehören später in die Einstellungen und werden gespeichert.

## Kernanforderungen

- Offline-Transkription ohne Internetabhängigkeit
- Audioverarbeitung lokal auf dem Gerät
- möglichst geringe Verzögerung
- große, kontrastreiche Schrift
- sehr wenige Bedienhandlungen
- automatische Aufnahme beim App-Start
- robuste Erkennung bei Umgebungslärm
- visuelle Unterscheidung verschiedener Sprecher durch Farbe **und** Beschriftung
- verschiedene Sprecher visuell unterscheiden, ohne Geschlecht oder Identität zu erraten

## Aktueller Prototyp

Der Gesprächspfad verwendet jetzt die eigene PCM-Audioaufnahme und die lokale `SherpaGermanStreamingEngine`. Androids `SpeechRecognizer` ist aus `MainActivity` entfernt. Die App startet nach vorhandener Mikrofonfreigabe automatisch, verarbeitet Audio lokal und zeigt Streaming-Zwischenergebnisse sowie abgeschlossene Segmente an.

Solange die deutschen Modellgewichte noch nicht als App-Assets vorliegen, startet die Transkription bewusst nicht und meldet das fehlende Offline-Modell. Es gibt keinen stillen Cloud-Fallback.

## Zielarchitektur

```text
App öffnen
   ↓
Mikrofon automatisch aktiv
   ↓
direkte PCM-Audioaufnahme
   ↓
Rauschunterdrückung / Pegelanpassung
   ↓
Voice Activity Detection (VAD)
   ↓
lokale Sprecher-Diarisierung (Person 1, Person 2, …)
   ↓
lokale Speech-to-Text-Engine
   ↓
Live-Untertitel
```

## Sprecher unterscheiden

Die App soll lokal erkennen, ob aufeinanderfolgende Sprachabschnitte von derselben oder einer anderen Person stammen. Jeder erkannte Sprecher erhält eine stabile Kennzeichnung wie **Person 1**, **Person 2** usw. und zusätzlich eine unterscheidbare Textfarbe.

Die Farbe ist nur eine zusätzliche Orientierung: Die Beschriftung bleibt immer sichtbar, damit die Funktion nicht von der Farbwahrnehmung abhängt. Die App leitet aus einer Stimme weder Geschlecht noch Identität ab. Ist keine zuverlässige Zuordnung möglich, wird **Sprecher unbekannt** angezeigt.

Die Oberfläche unterstützt dieses Datenmodell bereits. Die automatische lokale Speaker-Diarization wird im nächsten Schritt an die PCM-Audiopipeline angeschlossen.

## Mikrofon und Hintergrundgeräusche

Ein einfacher Lautstärkeregler trennt Sprache nicht von Störgeräuschen. Geplant sind Pegelanzeige, einstellbare Empfindlichkeit, Noise Suppression, VAD und ein ungefilterter Originalmodus. Diese technischen Einstellungen sollen den Hauptbildschirm nicht überladen.

## Technik

- Kotlin
- Jetpack Compose
- Material 3
- minSdk 26
- sherpa-onnx als lokale Streaming-STT-Laufzeit
- direkte `AudioRecord`-PCM-Aufnahme mit 16 kHz Mono

## Nächste Schritte

- direkte PCM-Mikrofonaufnahme **implementiert**
- PCM-Audiostream an austauschbare Offline-STT-Schnittstelle **implementiert**
- laufende RMS-Mikrofonpegelmessung **implementiert**
- sherpa-onnx deutsche Streaming-Engine **implementiert**
- deutsche Modellgewichte inklusive Lizenzhinweisen in die App-Assets aufnehmen und auf Geräten testen
- echte Noise Suppression / Audio-Vorverarbeitung
- Voice Activity Detection
- lokale Speaker-Diarization und stabile Sprecher-IDs
- kontinuierliche Transkription
- lokale Speaker-Diarization: Sprecherwechsel erkennen und farblich/mit Person-Nummer markieren
- Live-Modus scrollt automatisch zum neuesten Text
- beim manuellen Zurückscrollen bleibt die Leseposition stehen
- „Zum aktuellen Gespräch“ kehrt mit einem Tipp zu Live zurück
- Einstellungsseite für Mikrofon, Schrift und Kontrast
- Tablet-Optimierung
- Offline-Funktionstest ohne Netzwerkverbindung

## Gesprächsverlauf

Während des normalen Mitlesens folgt die Ansicht automatisch dem neuesten Text. Scrollt der Nutzer nach oben, wechselt die Anzeige in den Nachlesemodus und lässt die gewählte Position in Ruhe, während die Transkription im Hintergrund weiterläuft. Mit **↓ Zum aktuellen Gespräch** springt die Ansicht wieder zum neuesten Text.

Der Verlauf bleibt zunächst nur für die laufende Sitzung im Arbeitsspeicher. Eine dauerhafte Speicherung von Gesprächen soll später nur als bewusst aktivierte Option angeboten werden.

## Aktueller Audio-Unterbau

`ConversationAudioController` verbindet die direkte 16-kHz-Mono-PCM-Aufnahme mit der austauschbaren `OfflineSpeechEngine`. Die PCM-Blöcke werden fortlaufend an die Engine übergeben; parallel wird ohne Netzwerkzugriff ein RMS-Mikrofonpegel berechnet. Die Oberfläche muss dadurch später weder das konkrete Sprachmodell noch die Audioaufnahme kennen.

Die sherpa-onnx-Streaming-Engine für das deutsche Kroko-Zipformer-Modell ist als Adapter implementiert. Sie liefert fortlaufend Zwischenergebnisse und schließt Textsegmente über Endpoint-Erkennung ab. Die großen Modellgewichte selbst sind noch nicht im Repository enthalten; fehlen sie, meldet die App dies ausdrücklich und fällt nicht stillschweigend auf eine Cloud-Erkennung zurück.

## Status

Version 0.2 in Entwicklung. Noch nicht für produktiven oder barrierefreiheitskritischen Einsatz freigegeben.


## Sprecher unterscheiden

Die App soll Sprecherwechsel sichtbar machen. Erkannte Stimmen erhalten stabile neutrale Kennzeichnungen wie **Person 1**, **Person 2** usw. und jeweils eine gut unterscheidbare Textfarbe. Farbe wird immer zusätzlich durch die Person-Nummer ergänzt, damit die Information nicht allein von Farbwahrnehmung abhängt.

Die Funktion soll **keine** automatische Zuordnung zu „männlich“ oder „weiblich“ vornehmen. Ziel ist ausschließlich die für das Gespräch relevante Information: Spricht dieselbe Person weiter oder hat der Sprecher gewechselt?

Der aktuelle Prototyp kennt noch keine verlässliche Sprecher-ID und zeigt deshalb zunächst Person 1. Die echte Zuordnung folgt mit der lokalen Diarisierungs-Engine.
