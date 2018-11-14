package com.fuud.kroxy.impl

import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel

suspend fun ByteReadChannel.copyTo(writeChannel: ByteWriteChannel, transformer: (Byte) -> Byte = {it -> it}) {
    val buffer = ByteArray(100)
    while (true) {
        val read = this.readAvailable(buffer, 0, 100)
        if (read < 0) {
            writeChannel.close()
            break
        } else {
            for (i in 0 until read) {
                buffer[i] = transformer(buffer[i])
            }
            writeChannel.writeFully(buffer, 0, read)
            writeChannel.flush()
        }
    }
}