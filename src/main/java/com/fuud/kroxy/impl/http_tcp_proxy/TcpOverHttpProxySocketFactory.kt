package com.fuud.kroxy.impl.http_tcp_proxy

import com.fuud.kroxy.impl.SocketFactory
import com.fuud.kroxy.impl.util.DelegateSocket
import com.google.common.net.HostAndPort
import io.ktor.network.sockets.ReadWriteSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.experimental.io.readUTF8Line
import kotlinx.coroutines.experimental.io.writeStringUtf8
import java.io.IOException


class TcpOverHttpProxySocketFactory(private val socketFactory: SocketFactory, private val httpProxy: HostAndPort) : SocketFactory {

    override suspend fun connect(hostAndPort: HostAndPort): ReadWriteSocket {
        val socket = socketFactory.connect(httpProxy)
        val readChannel = socket.openReadChannel()
        val writeChannel = socket.openWriteChannel()

        writeChannel.writeStringUtf8("CONNECT ${hostAndPort} HTTP/1.1\r\n")
        writeChannel.writeStringUtf8("Host: ${hostAndPort}\r\n")
        writeChannel.writeStringUtf8("User-Agent: curl/7.32.0\r\n")
        writeChannel.writeStringUtf8("Proxy-Connection: Keep-Alive\r\n")
        writeChannel.writeStringUtf8("\r\n")
        writeChannel.flush()

        val status = readChannel.readUTF8Line()
        if (status?.contains("200 Connection established") != true) {
            while (true) {
                val message = readChannel.readUTF8Line() ?: break
                println(message)
            }
            throw IOException("Cannot build tunnel via $httpProxy because of '$status'")
        }

        while (true) {
            val line = readChannel.readUTF8Line() ?: throw IOException("Connection closed")
            if (line.isBlank()) {
                break
            }
        }

        return DelegateSocket(socket, writeChannel, readChannel)

    }
}