package com.fuud.kroxy.impl.websocket

import com.fuud.kroxy.impl.SocketFactory
import com.google.common.net.HostAndPort
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

class WebSocketProxyServer(port: Int, socketFactory: SocketFactory){
    init {
        embeddedServer(Netty, port) {
            install(WebSockets)
            install(CallLogging)

            routing {
                webSocket("/ws") {
                    val packet = incoming.receive()
                    val connectTo = packet as Frame.Text
                    val targetSocket = socketFactory.connect(HostAndPort.fromString(connectTo.readText()))
                    val writeChannel = targetSocket.openWriteChannel()
                    val readChannel = targetSocket.openReadChannel()


                    launch {
                        incoming.consumeEach {
                            if (it is Frame.Binary) {
                                writeChannel.writeFully(it.buffer)
                            }
                        }
                    }

                    while (true) {
                        readChannel.read(min = 1) {
                            runBlocking {
                                send(Frame.Binary(false, it))
                            }
                        }
                    }
                }
            }
        }.start(wait = false)
    }
}