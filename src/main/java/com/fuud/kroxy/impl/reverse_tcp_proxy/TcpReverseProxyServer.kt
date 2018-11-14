package com.fuud.kroxy.impl.reverse_tcp_proxy

import com.fuud.kroxy.impl.SocketFactory
import com.fuud.kroxy.impl.util.DelegateSocket
import com.google.common.net.HostAndPort
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ReadWriteSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.io.writeStringUtf8
import kotlinx.coroutines.experimental.launch
import java.net.InetSocketAddress

class TcpReverseProxyServer(port: Int): SocketFactory {
    private val channel = Channel<Socket>()
    init {
        launch {
            val server = aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().bind(InetSocketAddress("127.0.0.1", port))
            while (true) {
                val socket = server.accept()
                channel.send(socket)
            }
        }
    }

    override suspend fun connect(hostAndPort: HostAndPort): ReadWriteSocket {
        val socket = channel.receive()
        val writeChannel = socket.openWriteChannel();
        writeChannel.writeStringUtf8(hostAndPort.toString() + "\r\n")

        return DelegateSocket(socket, writeChannel)
    }

}