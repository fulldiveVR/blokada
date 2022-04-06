/*
 * This file is part of Blokada.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright © 2022 Blocka AB. All rights reserved.
 *
 * @author Karol Gusak (karol@blocka.net)
 */

package engine

import model.BlokadaException
import model.Dns
import model.ipv4
import model.isDnsOverHttps
import newengine.BlockaDnsService
import org.pcap4j.packet.namednumber.UdpPort
import repository.DnsDataSource
import service.ConnectivityService
import utils.Logger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

object DnsMapperService {

    private val log = Logger("DnsMapper")

    private var servers = emptyList<InetAddress>()
    private var serversAlter = emptyList<InetAddress>()
    private var proxyDnsAlterPort = UdpPort(443, "dns alter port")
    private var useProxyDns = false

    fun setDns(dns: Dns, dnsAlter: Dns, doh: Boolean, plusMode: Boolean = false) {
        log.v("Using DNS configuration [DoH/PlusMode: $doh/$plusMode]: $dns")

        servers = decideDns(dns, plusMode)
        serversAlter = dnsAlter.ips.ipv4().map { Inet4Address.getByName(it) }
        proxyDnsAlterPort = UdpPort(dnsAlter.port!!.toShort(), dnsAlter.name)

        if (servers.isEmpty()) throw BlokadaException("No DNS servers found")
        else log.v("Using DNS: $servers")

        useProxyDns = false
        if (dns.isDnsOverHttps() && doh) {
            log.v("Will use DNS over HTTPS")
            useProxyDns = true
        }
    }

    fun count() = servers.size

    fun externalForIndex(index: Int): InetAddress {
        return if (useProxyDns) proxyDnsIp else servers[index]
    }

    fun externalToInternal(address: InetAddress): InetAddress? {
        var result: InetAddress? = null
        val src = dnsProxyDst4.copyOf()
        if (useProxyDns) {
            src[3] = 1.toByte()
            result = Inet4Address.getByAddress(src)
        } else {
            val dst = servers.firstOrNull { it == address }
            if (dst != null) {
                val index = servers.indexOf(dst)
                src[3] = (index + 1).toByte()
                result = Inet4Address.getByAddress(src)
            } else {
                val dst2 = serversAlter.firstOrNull { it == address }
                if (dst2 != null) {
                    val index = serversAlter.indexOf(dst2)
                    src[3] = (index + 1).toByte()
                    result = Inet4Address.getByAddress(src)
                }
            }
        }
        return result
    }

    fun dstDnsPort(): UdpPort {
        return if (useProxyDns) proxyDnsPort else UdpPort.DOMAIN
    }

    fun dstDnsAlter(): InetAddress {
        return serversAlter[0]
    }

    fun dstDnsAlterPort(): UdpPort {
        return proxyDnsAlterPort
    }

    val proxyDnsIpBytes = byteArrayOf(127, 0, 0, 1)
    val proxyDnsIp = Inet4Address.getByAddress(proxyDnsIpBytes)
    val proxyDnsPort = UdpPort(BlockaDnsService.PROXY_PORT, "blocka-doh-proxy")

}

fun decideDns(dns: Dns, plusMode: Boolean): List<InetAddress> {
    return when {
        dns.id == DnsDataSource.network.id -> {
            ConnectivityService.getActiveNetworkDns()
        }
        plusMode && dns.plusIps != null -> dns.plusIps.ipv4().map { Inet4Address.getByName(it) }
        else -> dns.ips.ipv4().map { Inet4Address.getByName(it) }
    }
}

// A TEST-NET IP range from RFC5735
private const val dnsProxyDst4String = "203.0.113.0"
internal val dnsProxyDst4 = Inet4Address.getByName(dnsProxyDst4String).address!!

// A special test subnet from RFC3849
private const val dnsProxyDst6String = "2001:DB8::"
internal val dnsProxyDst6 = Inet6Address.getByName(dnsProxyDst6String).address!!

