package com.fuud.kroxy.impl.reverse_tcp_proxy

import com.fuud.kroxy.impl.SocketFactory
import com.fuud.kroxy.impl.copyTo
import com.google.common.net.HostAndPort
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.io.readUTF8Line
import kotlinx.coroutines.experimental.launch
import java.io.IOException
import java.lang.IllegalArgumentException

class TcpReverseProxyClient(private val server: HostAndPort, private val toServerSocketFactory: SocketFactory, private val outgoingSocketFactory: SocketFactory) {
    init {
        launch {
            while (true) {
                try {
                    val socket = toServerSocketFactory.connect(server)
                    val readChannel = socket.openReadChannel()
                    val writeChannel = socket.openWriteChannel()
                    val connectTo = readChannel.readUTF8Line()
                            ?: throw IllegalArgumentException("no target was sent")
                    println("Requested proxy connection to $connectTo")

                    launch {
                        outgoingSocketFactory.connect(HostAndPort.fromString(connectTo))
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
                } catch (e: IOException) {
                    e.printStackTrace()
                    delay(1000)
                }
            }
        }
    }
}