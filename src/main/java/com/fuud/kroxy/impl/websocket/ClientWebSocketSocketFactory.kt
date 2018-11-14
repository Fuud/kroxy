package com.fuud.kroxy.impl.websocket

import com.fuud.kroxy.impl.SocketFactory
import com.google.common.net.HostAndPort
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.network.sockets.ReadWriteSocket
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.io.ByteChannel
import kotlinx.coroutines.experimental.io.ReaderJob
import kotlinx.coroutines.experimental.io.WriterJob
import kotlinx.coroutines.experimental.io.reader
import kotlinx.coroutines.experimental.io.writer
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

class ClientWebSocketSocketFactory(val wsServer: HostAndPort) : SocketFactory {
    val client = HttpClient(CIO).config { install(WebSockets) }

    override suspend fun connect(hostAndPort: HostAndPort): ReadWriteSocket {
        TODO("Does not work now")

        val socketContext = CompletableDeferred<Unit>()
        val attachForRead = Channel<ByteChannel>()
        val attachForWrite = Channel<ByteChannel>()

        launch {
            client.ws(
                    method = HttpMethod.Get,
                    host = wsServer.host,
                    port = wsServer.port,
                    path = "/ws"
            ) {
                send(Frame.Text(hostAndPort.toString()))
                socketContext.complete(Unit)

                launch {
                    val attachedReadChannel = attachForRead.receive()
                    incoming.consumeEach {
                        if (it is Frame.Binary) {
                            attachedReadChannel.writeFully(it.buffer)
                        }
                    }
                }

                val attachedForWrite = attachForWrite.receive()
                while (true) {
                    attachedForWrite.read(min = 1) {
                        runBlocking {
                            send(Frame.Binary(false, it))
                        }
                    }
                }
            }
        }

        return object : ReadWriteSocket {
            override val socketContext: Deferred<Unit>
                get() = socketContext

            override fun attachForReading(channel: ByteChannel): WriterJob {
                return writer(ioCoroutineDispatcher, channel) {
                    attachForRead.send(channel)
                }
            }

            override fun attachForWriting(channel: ByteChannel): ReaderJob {
                return reader(ioCoroutineDispatcher, channel) {
                    attachForWrite.send(channel)
                }
            }

            override fun close() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }
}