package com.fuud.kroxy.impl

import com.google.common.net.HostAndPort
import io.ktor.cio.write
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.experimental.io.readASCIILine
import kotlinx.coroutines.experimental.launch
import java.lang.IllegalStateException
import java.net.InetSocketAddress
import java.net.URL

class HttpProxy(val port:Int, socketFactory: SocketFactory) {
    init {
        launch {
            val server = aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().bind(InetSocketAddress("127.0.0.1", port))
            println("Started proxy server at ${server.localAddress}")

            while (true) {
                val socket = server.accept()

                launch {
                    println("Socket accepted: ${socket.remoteAddress}")

                    val input = socket.openReadChannel()
                    val output = socket.openWriteChannel()

                    try {
                        val line = input.readASCIILine()
                                ?: throw IllegalStateException("Http method was expected but got null")

                        var targetHost: String? = null;
                        if (line.startsWith("CONNECT ")) {
                            targetHost = line.substringAfter("CONNECT ").substringBeforeLast(" ")
                            while (true) {
                                val headerLine = input.readASCIILine()
                                        ?: throw IllegalStateException("Header was expected but got null")
                                if (headerLine.startsWith("Host: ")) {
                                    targetHost = headerLine.substringAfter("Host: ").trim()
                                }
                                if (headerLine.isBlank()) {
                                    break;
                                }
                            }

                            if (!targetHost.isNullOrBlank()) {
                                val hostAndPort = HostAndPort.fromString(targetHost)
                                val targetSocket = socketFactory.connect(hostAndPort)

                                val targetReadChannel = targetSocket.openReadChannel()
                                val targetWriteChannel = targetSocket.openWriteChannel()

                                output.write("HTTP/1.0 200 Connection established\r\n\r\n");
                                output.flush()

                                val proxyRead = launch {
                                    targetReadChannel.copyTo(output)
                                }

                                val proxyWrite = launch {
                                    input.copyTo(targetWriteChannel)
                                }

                                proxyRead.join()
                                proxyWrite.join()
                            }
                        }

                        if (line.startsWith("GET") || line.startsWith("POST") || line.startsWith("PUT")) {
                            val requestParts = line.split(" ")
                            val method = requestParts[0]
                            val url = URL(requestParts[1])
                            val httpVersion = requestParts[2]
                            val port = if (url.port < 0) {
                                url.defaultPort
                            } else {
                                url.port
                            }

                            val targetSocket = socketFactory.connect(HostAndPort.fromParts(url.host, port))
                            val targetReadChannel = targetSocket.openReadChannel()
                            val targetWriteChannel = targetSocket.openWriteChannel()

                            targetWriteChannel.write("$method ${url.path} $httpVersion\r\n")
                            val proxyRead = launch {
                                targetReadChannel.copyTo(output)
                            }

                            val proxyWrite = launch {
                                input.copyTo(targetWriteChannel)
                            }

                            proxyRead.join()
                            proxyWrite.join()

                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    } finally {
                        socket.close()
                    }
                }
            }
        }
    }
}