package com.fuud.kroxy.impl

import com.google.common.net.HostAndPort
import io.ktor.network.sockets.ReadWriteSocket

interface SocketFactory{
    suspend fun connect(hostAndPort: HostAndPort): ReadWriteSocket
}