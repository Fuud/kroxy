package com.fuud.kroxy.impl.encrypted

import com.fuud.kroxy.impl.SocketFactory
import com.fuud.kroxy.impl.copyTo
import com.google.common.net.HostAndPort
import io.ktor.network.sockets.ReadWriteSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.io.ByteChannel
import kotlinx.coroutines.experimental.io.ReaderJob
import kotlinx.coroutines.experimental.io.WriterJob
import kotlinx.coroutines.experimental.io.reader
import kotlinx.coroutines.experimental.io.writer

class EncryptedSocketFactory(private val cipher: Cipher, private val socketFactory: SocketFactory) : SocketFactory {

    override suspend fun connect(hostAndPort: HostAndPort): ReadWriteSocket {
        val socket = socketFactory.connect(hostAndPort)

        return object : ReadWriteSocket {
            override val socketContext: Deferred<Unit>
                get() = socket.socketContext

            override fun attachForReading(channel: ByteChannel): WriterJob {
                return writer(ioCoroutineDispatcher, channel) {
                    cipher.decrypt(socket.openReadChannel()).copyTo(channel)
                }
            }

            override fun attachForWriting(channel: ByteChannel): ReaderJob {
                return reader(ioCoroutineDispatcher, channel) {
                    val encrypted = cipher.encrypt(socket.openWriteChannel())
                    channel.copyTo(encrypted)
                }
            }

            override fun close() {
                socket.close()
            }
        }
    }
}