# Gesprächs-Transkription für gehörlose und schwerhörige Menschen

Android-App für möglichst direkte Sprache-zu-Text-Transkription während eines Gesprächs.

## MVP

Die erste Version bietet:

- Start/Stop der Live-Spracherkennung
- Anzeige von Zwischen- und Endergebnissen
- große, gut lesbare Darstellung
- Verlauf der erkannten Sätze
- Mikrofon-Berechtigung zur Laufzeit
- deutsche Spracherkennung als Standard

## Technik

- Kotlin
- Jetpack Compose
- Android SpeechRecognizer
- Material 3

> Hinweis: Die Verfügbarkeit, Latenz und Offline-Fähigkeit der Spracherkennung hängen vom Android-Gerät und dem installierten Speech-Service ab.
