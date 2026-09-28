# Smartphone-Abnahmetest – Gespräch Live 0.2.1

## Ergebnis

Release Candidate 0.2.1 wurde auf realer Android-Hardware (Android 11) installiert und praktisch getestet.

Bestätigte Funktionen:
- deutsche Sprache wird gut erkannt, besonders bei langsamem Standarddeutsch
- relativ leises Audiosignal kann weiterhin transkribiert werden
- Sprache aus TV-/Video-Inhalten wird erkannt
- Live-Text wird kontinuierlich gescrollt
- Sprecherwechsel werden grundsätzlich erkannt und farblich dargestellt
- Person 2 und weitere Sprecher-IDs wurden im Praxistest sichtbar
- Pause/Weiter und die lokale Verarbeitung funktionieren

## Installationstest

Bei einer manuellen Installation des Test-APKs trat einmalig ein Installationsproblem auf. Nach einem Neustart des Android-11-Tablets ließ sich die App installieren und funktionierte anschließend normal. Der Fehler ist bisher nicht reproduziert und wird deshalb als Beobachtung dokumentiert, nicht als bestätigter App-Fehler.

Der anschließende Abnahmetest nach vollständiger Deinstallation war erfolgreich: Die 0.2.1-Test-APK ließ sich frisch installieren und die App funktionierte. Die Spracherkennung arbeitete wie erwartet; die Sprecherzuordnung wurde im erneuten Praxistest sogar als verbessert wahrgenommen und ordnete Sprecher erfolgreich zu.

## Beobachtungen / bekannte Einschränkungen

- Die lokale Analyse erzeugt eine wahrnehmbare Verzögerung zwischen Sprache und Text.
- Dialekt wird teilweise schlechter erkannt als Standarddeutsch.
- Gleichzeitige Hintergrundmusik kann die Erkennung verschlechtern.
- Die Sprechergruppierung kann bei tatsächlich zwei Personen zusätzliche IDs wie Person 3 oder Person 4 erzeugen.
- Die Sprecher-ID ist eine Orientierungshilfe; Priorität hat die korrekte Texttranskription.
- Die frühere Pegelmeldung „Mikrofon: leise“ wurde entfernt, weil auch leises Audio erfolgreich erkannt werden kann. Während aktiver Aufnahme zeigt die Oberfläche „● Zuhören“.

## Eingefrorene ASR-Einstellungen für 0.2.1

- VAD threshold: 0,5
- minSilenceDuration: 0,35 s
- minSpeechDuration: 0,20 s
- sample rate: 16 kHz
- Sprecheranalyse ab ca. 0,75 s Segmentlänge

Die 0,35-s-Abschlussstille wurde im A/B-Gerätetest gegenüber 0,25 s als zuverlässiger bestätigt und wird für diesen Release Candidate nicht verändert.

## Abnahmestatus

**Neuinstallation auf dem Android-11-Testgerät: bestanden.**

Die Kernfunktion „Öffnen → Lesen“ funktioniert im Praxistest. Weitere Arbeiten an 0.2.1 konzentrieren sich auf Veröffentlichung, Datenschutz, Lizenz-/Attributionsprüfung und Release-Paketierung; keine experimentellen Änderungen an der funktionierenden Spracherkennung.
