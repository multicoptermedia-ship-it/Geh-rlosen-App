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


## License safety

Do not bundle or redistribute the legacy Kroko `.onnx` weights referenced by
`sherpa-onnx-streaming-zipformer-de-kroko-2025-08-06` without obtaining the
appropriate model license.

Banafo's September 2025 model-card history distinguishes the newer Community
`.data` weights (CC-BY-SA) from the older `.onnx` exports (non-commercial
only at that time). The current Kroko model card describes Community models as
CC-BY-SA, but that does not retroactively prove that the legacy ONNX export has
the same redistribution terms.

For release builds, use only model files whose exact artifact and license have
been verified. Keep attribution/license notices with any redistributable model.


## Preferred redistributable German model

For a freely redistributable build, prefer:

- `sherpa-onnx-nemo-stt_de_fastconformer_hybrid_large_pc-int8`
- Files: `model.int8.onnx`, `tokens.txt`
- Upstream model: NVIDIA `stt_de_fastconformer_hybrid_large_pc`
- Upstream model license: CC-BY-4.0
- Runtime mode: sherpa-onnx OfflineRecognizer + VAD / simulated streaming
- Approximate quantized ONNX model size: 132 MB

The application must include the required CC-BY-4.0 attribution/license notice
when this model is redistributed. Do not replace this with the legacy Kroko
ONNX weights.

The existing streaming engine remains in source until the VAD/offline path has
passed Android build and device tests.
