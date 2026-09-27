# Smartphone-Test – Gespräch Live 0.2.0

## Ziel
Diese Testfassung prüft die Kernidee „Öffnen → Lesen“ auf einem echten Android-Smartphone.

## Installation
1. Das GitHub-Actions-Artefakt `gespraech-live-0.2.0-offline-test` herunterladen und entpacken.
2. `Gespraech-Live-0.2.0-offline-test.apk` auf das Android-Smartphone übertragen.
3. Die APK installieren. Android kann dafür einmalig die Erlaubnis „Unbekannte Apps installieren“ verlangen.
4. Gespräch Live starten und den Mikrofonzugriff erlauben.

## Testdatei
- APK: `Gespraech-Live-0.2.0-offline-test.apk`
- Build-Commit: `3bd25d860de5d80814ced4bac5b1c8cf7e30ea37`
- Die Datei `Gespraech-Live-0.2.0-offline-test.apk.sha256` enthält die vom Build berechnete SHA-256-Prüfsumme der APK selbst.

## Kernprüfung
- Nach dem Öffnen soll die lokale Offline-Erkennung starten.
- Einen normalen deutschen Satz in ungefähr 0,5–2 m Abstand sprechen.
- Prüfen, ob Text erscheint und gut lesbar bleibt.
- „Pause“ drücken: Das Mikrofon muss stoppen.
- „Weiter“ drücken: Die Erkennung muss wieder starten.
- In eine andere App wechseln und zurückkehren: Gespräch Live darf nicht heimlich weiter zuhören. Danach bewusst „Weiter“ drücken.
- Im Gespräch nach oben scrollen: Neuer Text darf weiter entstehen, aber die Ansicht darf nicht automatisch nach unten springen. Mit „↓ Zum aktuellen Gespräch“ zurückkehren.

## Offline-Prüfung
1. Gespräch Live vollständig schließen.
2. WLAN und mobile Daten ausschalten.
3. Gespräch Live erneut öffnen.
4. Einen deutschen Satz sprechen.
5. Die Transkription muss weiterhin funktionieren.

## Geräuschprüfung
Je einen kurzen Test durchführen:
- ruhiger Raum
- Fernseher oder Radio im Hintergrund
- zwei Personen im Raum
- etwas größere Entfernung zum Smartphone

## Bitte bei Auffälligkeiten notieren
- Smartphone-Modell und Android-Version
- gesprochener Satz
- angezeigter Text
- ungefährer Abstand zum Smartphone
- ruhige oder laute Umgebung
- ob Pause/Weiter und App-Wechsel funktioniert haben

Testfassung: 0.2.0
