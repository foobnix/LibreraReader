# Sherpa-ONNX TTS third-party notices

This project includes optional offline Chinese TTS support based on the following
third-party components and model assets.

## sherpa-onnx

- Source: https://github.com/k2-fsa/sherpa-onnx
- Used files: `libsherpa-onnx-jni.so`, `libsherpa-onnx-c-api.so`, `libsherpa-onnx-cxx-api.so`
- License: Apache License 2.0

## ONNX Runtime

- Source: https://github.com/microsoft/onnxruntime
- Used file: `libonnxruntime.so`
- License: MIT License

## matcha-icefall-zh-baker and vocos model assets

- Model page: https://k2-fsa.github.io/sherpa/onnx/tts/all/Chinese/matcha-icefall-zh-baker.html
- Used files:
  - `app/src/main/assets/matcha-icefall-zh-baker/model-steps-3.onnx`
  - `app/src/main/assets/matcha-icefall-zh-baker/lexicon.txt`
  - `app/src/main/assets/matcha-icefall-zh-baker/tokens.txt`
  - `app/src/main/assets/matcha-icefall-zh-baker/phone.fst`
  - `app/src/main/assets/matcha-icefall-zh-baker/date.fst`
  - `app/src/main/assets/matcha-icefall-zh-baker/number.fst`
  - `app/src/main/assets/vocos-22khz-univ.onnx`
- Important restriction: the included model README states that the Baker dataset
  used to train the model is for non-commercial use only.

