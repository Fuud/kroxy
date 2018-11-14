package com.fuud.kroxy.impl.encrypted

import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel

interface Cipher {
    suspend fun encrypt(output: ByteWriteChannel): ByteWriteChannel

    suspend fun decrypt(input: ByteReadChannel): ByteReadChannel
}