# Datenschutz – technischer Stand

Gespräch Live ist nach dem Prinzip „lokal und datensparsam“ aufgebaut.

- Die Spracherkennung läuft mit dem gebündelten Modell auf dem Android-Gerät.
- Für die Transkription ist keine Internetverbindung erforderlich.
- Mikrofon-Audio wird für die Erkennung im Arbeitsspeicher verarbeitet und von der App nicht als Audiodatei gespeichert.
- Der angezeigte Gesprächsverlauf wird derzeit nur für die laufende App-Sitzung im Arbeitsspeicher gehalten und nicht dauerhaft gespeichert.
- Beim Pausieren wird die Mikrofonaufnahme beendet und die Audioressource freigegeben.
- Verlässt die App den Vordergrund, wird die Aufnahme gestoppt. Eine Rückkehr startet das Mikrofon nicht automatisch.
- Android-App-Backups sind deaktiviert, damit interne App-Daten nicht automatisch über die normale Backup-Funktion übertragen werden.
- Die App fordert nur die Mikrofonberechtigung an, die für ihre Kernfunktion benötigt wird.

Vor einer öffentlichen Veröffentlichung werden dieser technische Stand, die endgültige Datenschutzerklärung und die Angaben zur Google-Play-Datensicherheit noch einmal gemeinsam geprüft.
