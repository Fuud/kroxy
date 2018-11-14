package com.fuud.kroxy.impl.tcp_proxy

import com.fuud.kroxy.ConnectionProcessor
import com.fuud.kroxy.impl.SocketFactory
import com.fuud.kroxy.impl.copyTo
import com.google.common.net.HostAndPort
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.readUTF8Line
import kotlinx.coroutines.experimental.launch
import java.lang.IllegalArgumentException

class TcpProxyHandler(val socketFactory: SocketFactory) : ConnectionProcessor{
    override suspend fun process(readChannel: ByteReadChannel, writeChannel: ByteWriteChannel) {
        val connectTo = readChannel.readUTF8Line()
                ?: throw IllegalArgumentException("no target was sent")

        println("Requested proxy connection to $connectTo")

        socketFactory.connect(HostAndPort.fromString(connectTo))
                .use { target ->
                    val readJob = launch {
                        target.openReadChannel().copyTo(writeChannel)
                    }
                    val writeJob = launch {
                        readChannel.copyTo(target.openWriteChannel())
                    }
                    readJob.join()
                    writeJob.join()
                }
    }
}