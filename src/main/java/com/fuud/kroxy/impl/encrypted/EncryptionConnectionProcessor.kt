package com.fuud.kroxy.impl.encrypted

import com.fuud.kroxy.ConnectionProcessor
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel

class EncryptionConnectionProcessor(private val cipher: Cipher,
                                    private val connectionProcessor: ConnectionProcessor) : ConnectionProcessor {

    override suspend fun process(byteReadChannel: ByteReadChannel, byteWriteChannel: ByteWriteChannel) {
        connectionProcessor.process(
                cipher.decrypt(byteReadChannel),
                cipher.encrypt(byteWriteChannel)
        )
    }
}