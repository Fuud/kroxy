package com.fuud.kroxy.impl.tcp_proxy

import com.fuud.kroxy.ConnectionProcessor
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import java.net.InetSocketAddress

class TcpProxyServer(port: Int, connectionProcessor: ConnectionProcessor) {
    init {
        launch {
            val server = aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().bind(InetSocketAddress("0.0.0.0", port))
            while (true) {
                val socket = server.accept()
                println("TcpProxyServer. Socket accepted: ${socket.remoteAddress}")
                launch {
                    socket.use { socket ->
                        val readChannel = socket.openReadChannel()
                        val writeChannel = socket.openWriteChannel()
                        connectionProcessor.process(readChannel, writeChannel)
                    }
                }
            }
        }
    }
}