# Offline-Modelle – Release Candidate 0.2.1

## Deutsche Spracherkennung

Verwendet wird:
- sherpa-onnx-nemo-stt_de_fastconformer_hybrid_large_pc-int8
- model.int8.onnx
- tokens.txt
- Upstream: NVIDIA stt_de_fastconformer_hybrid_large_pc
- Lizenz des Upstream-Modells: CC BY 4.0
- Runtime: sherpa-onnx OfflineRecognizer

Die CI lädt die Modellartefakte für den Build. Es gibt keinen Cloud-Fallback.

## Voice Activity Detection

- silero_vad.onnx
- Silero VAD
- Lizenz: MIT

Im realen Android-11-Test war eine Abschlussstille von 0,35 s zuverlässiger als 0,25 s. Diese Einstellung bleibt für 0.2.1 unverändert.

## Sprecheranalyse

- wespeaker_en_voxceleb_resnet34.onnx
- WeSpeaker / VoxCeleb ResNet34
- Verwendung ausschließlich für neutrale sitzungsbezogene Sprechergruppierung (Person 1, Person 2, …)
- keine Namens-, Identitäts- oder Geschlechtsbestimmung

Die Sprechergruppierung ist bewusst eine Zusatzfunktion. Falsche zusätzliche Person-IDs dürfen die Texttranskription nicht blockieren.

## Nicht verwenden

Die ältere Kroko-Streaming-ONNX-Variante wird für den Release Candidate nicht gebündelt. Sie bleibt wegen der früher festgestellten Lizenzunsicherheit ausdrücklich außerhalb der Distribution.

## Veröffentlichung

Die wesentlichen Hinweise stehen in THIRD_PARTY_NOTICES.txt bzw. THIRD_PARTY_NOTICES.md. Vor einer öffentlichen Veröffentlichung müssen die vollständigen Lizenz- und Attributionsanforderungen der tatsächlich ausgelieferten Artefakte nochmals geprüft werden.
