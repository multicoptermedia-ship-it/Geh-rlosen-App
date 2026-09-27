# Deutsches Offline-Sprachmodell

Die Streaming-Engine erwartet dieses Verzeichnis:

```
app/src/main/assets/sherpa-onnx-streaming-zipformer-de-kroko-2025-08-06/
├── encoder.onnx
├── decoder.onnx
├── joiner.onnx
└── tokens.txt
```

Die Modelldateien werden absichtlich nicht als Platzhalter eingecheckt. Ohne alle vier
Dateien meldet die App, dass das deutsche Offline-Sprachmodell fehlt, statt auf eine
Netzwerk-/Cloud-Erkennung auszuweichen.

Modell-ID:
`sherpa-onnx-streaming-zipformer-de-kroko-2025-08-06`

Die Dateinamen und die Zipformer2-Konfiguration entsprechen der sherpa-onnx
Kotlin-Konfiguration für dieses deutsche Streaming-Modell.

Vor einer Veröffentlichung der APK müssen die Modellgewichte samt Lizenzhinweisen
bewusst in die Distribution aufgenommen und auf realen Android-Geräten hinsichtlich
Latenz, Speicherverbrauch und Erkennungsqualität geprüft werden.
