package com.fuud.kroxy.impl.tcp_proxy

import com.fuud.kroxy.impl.SocketFactory
import com.fuud.kroxy.impl.util.DelegateSocket
import com.google.common.net.HostAndPort
import io.ktor.network.sockets.ReadWriteSocket
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.experimental.io.writeStringUtf8

class TcpProxyClient(val proxyHostAndPort: HostAndPort, val socketFactory: SocketFactory) : SocketFactory {

    override suspend fun connect(hostAndPort: HostAndPort): ReadWriteSocket {
        val proxySocket = socketFactory.connect(proxyHostAndPort)
        val writeChannel = proxySocket.openWriteChannel();
        writeChannel.writeStringUtf8(hostAndPort.toString() + "\r\n")

        return DelegateSocket(proxySocket, writeChannel)
    }
}