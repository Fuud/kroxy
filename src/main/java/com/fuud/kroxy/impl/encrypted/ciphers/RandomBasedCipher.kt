package com.fuud.kroxy.impl.encrypted.ciphers

import com.fuud.kroxy.impl.copyTo
import com.fuud.kroxy.impl.encrypted.Cipher
import kotlinx.coroutines.experimental.io.ByteChannel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.launch
import java.util.Random
import kotlin.experimental.xor

class RandomBasedCipher(private val passphrase: String) : Cipher {
    override suspend fun decrypt(input: ByteReadChannel): ByteReadChannel {
        val seed = input.readInt().xor(passphrase.hashCode())
        println("decrypt-seed: $seed")
        val random = Random(seed.toLong())

        val target = ByteChannel()

        launch {
            input.copyTo(target) {
                it.xor(random.nextInt().toByte())
            }
        }

        return target
    }

    override suspend fun encrypt(output: ByteWriteChannel): ByteWriteChannel {
        val seed = Random().nextInt()
        println("encrypt-seed: $seed")
        val random = Random(seed.toLong())

        val target = ByteChannel()
        output.writeInt(seed.xor(passphrase.hashCode()))
        output.flush()

        launch {
            target.copyTo(output) {
                it.xor(random.nextInt().toByte())
            }
        }

        return target
    }
}