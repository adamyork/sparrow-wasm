package com.github.adamyork.sparrow.wasm.service

interface WavService {

    fun chunk(file: String, chunkMs: Int): HashMap<Int, ByteArray>

}