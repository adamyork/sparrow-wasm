package com.github.adamyork.sparrow.wasm.service

class AssetLoadException(fileName: String) : RuntimeException("cant load $fileName")
