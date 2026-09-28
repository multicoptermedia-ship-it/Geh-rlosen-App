# Gespräch Live

Android-App für gehörlose und schwerhörige Menschen: gesprochene deutsche Sprache wird lokal auf dem Gerät in großen, gut lesbaren Text umgesetzt.

## Bedienprinzip: Öffnen → Lesen

Nach der einmaligen Mikrofonfreigabe startet die App beim Öffnen den Gesprächsmodus. Eine Internetverbindung ist für die Transkription nicht erforderlich.

## Release Candidate 0.2.1

Der aktuelle Stand wurde auf einem Android-11-Gerät praktisch getestet.

Bestätigt:
- deutsche Sprache wird lokal erkannt und als Text angezeigt
- auch relativ leise Sprache kann erkannt werden
- TV-/Video-Sprache wird transkribiert
- kontinuierliches Live-Scrollen
- manuelles Zurückscrollen unterbricht das automatische Folgen
- Pause/Weiter und Stopp beim Verlassen der App
- lokale Sprecheranalyse mit neutralen Bezeichnungen Person 1, Person 2, …
- Sprecher werden zusätzlich farblich unterschieden
- keine Geschlechts- oder Namenszuordnung
- keine Internetberechtigung im App-Manifest
- Gesprächsverlauf nur im Arbeitsspeicher der laufenden Sitzung

Bekannte Einschränkungen:
- Die Ausgabe folgt der Sprache mit einer wahrnehmbaren Analyseverzögerung.
- Dialekt und gleichzeitige Hintergrundmusik können die Erkennungsqualität verschlechtern.
- Die Sprechergruppierung ist eine Zusatzfunktion und kann eine bekannte Stimme gelegentlich als weitere Person (z. B. Person 3/4) einordnen.
- Sehr kurze Äußerungen können als „Sprecher unbekannt“ erscheinen.

Die Priorität liegt auf zuverlässiger, gut lesbarer Transkription. Die funktionierende ASR-Konfiguration wird für 0.2.1 nicht weiter experimentell verändert.

## Technik

- Kotlin / Jetpack Compose / Material 3
- minSdk 26, targetSdk 35
- direkte AudioRecord-Aufnahme, 16 kHz Mono
- sherpa-onnx lokal auf dem Gerät
- NVIDIA FastConformer German CTC, quantisierte ONNX-Variante
- Silero VAD
- WeSpeaker VoxCeleb ResNet34 für sitzungsbezogene Sprecher-Embeddings
- VAD-Abschlussstille 0,35 s (im Gerätetest zuverlässiger als 0,25 s)
- Sprecheranalyse ab ca. 0,75 s Sprachsegment

## Datenschutz

Mikrofon-Audio wird lokal verarbeitet und von der App nicht als Audiodatei gespeichert. Der Gesprächsverlauf wird derzeit nicht dauerhaft gespeichert. Beim Pausieren und beim Verlassen des Vordergrunds wird die Mikrofonaufnahme beendet. Weitere Details stehen in `PRIVACY.md`.

## Veröffentlichung

0.2.1 ist ein getesteter Release Candidate. Vor einer öffentlichen Store-Veröffentlichung folgen noch die abschließende Lizenz-/Attributionsprüfung, eine veröffentlichungsfähige Datenschutzerklärung, signierter Release-Build/AAB und die Google-Play-Angaben.
