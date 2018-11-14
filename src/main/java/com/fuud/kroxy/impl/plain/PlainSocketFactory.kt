package com.fuud.kroxy.impl.plain

import com.fuud.kroxy.impl.SocketFactory
import com.google.common.net.HostAndPort
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ReadWriteSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.util.ioCoroutineDispatcher

object PlainSocketFactory : SocketFactory {
    override suspend fun connect(hostAndPort: HostAndPort): ReadWriteSocket  =
            aSocket(ActorSelectorManager(ioCoroutineDispatcher)).tcp().connect(hostAndPort.host, hostAndPort.port)
}