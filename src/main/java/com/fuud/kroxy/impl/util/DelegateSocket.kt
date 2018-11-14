package com.fuud.kroxy.impl.util

import com.fuud.kroxy.impl.copyTo
import io.ktor.network.sockets.ReadWriteSocket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.io.ByteChannel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.ReaderJob
import kotlinx.coroutines.experimental.io.WriterJob
import kotlinx.coroutines.experimental.io.reader
import kotlinx.coroutines.experimental.io.writer

class DelegateSocket(val target: ReadWriteSocket, val writeChannel: ByteWriteChannel? = null, val readChannel: ByteReadChannel? = null) : ReadWriteSocket{
    override val socketContext: Deferred<Unit>
        get() = target.socketContext

    override fun attachForReading(channel: ByteChannel): WriterJob {
        return if (readChannel!= null){
            writer(socketContext, channel, null){
                readChannel.copyTo(channel)
            }
        }else{
            target.attachForReading(channel)
        }
    }

    override fun attachForWriting(channel: ByteChannel): ReaderJob {
        return if (writeChannel != null){
            reader(socketContext, channel, null) {
                channel.copyTo(writeChannel)
            }
        }else {
            target.attachForWriting(channel)
        }
    }

    override fun close() {
        target.close()
    }
}