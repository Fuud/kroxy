package com.fuud.kroxy

import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel

interface ConnectionProcessor{
    suspend fun process(byteReadChannel: ByteReadChannel, byteWriteChannel: ByteWriteChannel)
}