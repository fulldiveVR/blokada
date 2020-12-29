/*
 * This file is part of Blokada.
 *
 * Blokada is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Blokada is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Blokada.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright © 2020 Blocka AB. All rights reserved.
 *
 * @author Karol Gusak (karol@blocka.net)
 */

package ui

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import model.*
import repository.DnsDataSource
import service.AlertDialogService
import service.ConnectivityService
import service.PersistenceService
import utils.Logger
import org.blokada.R

class NetworksViewModel : ViewModel() {

    private val log = Logger("Networks")
    private val persistence = PersistenceService
    private val connectivity = ConnectivityService

    private val _configs = MutableLiveData<List<NetworkSpecificConfig>>()
    val configs: LiveData<List<NetworkSpecificConfig>> = _configs.distinctUntilChanged()

    private val _activeConfig = MutableLiveData<NetworkSpecificConfig>()
    val activeConfig: LiveData<NetworkSpecificConfig> = _activeConfig.distinctUntilChanged()

    init {
        val configs = persistence.load(NetworkSpecificConfigs::class).configs
        _configs.value = configs
        _activeConfig.decideConfig()
        updateLiveData()

        connectivity.onNetworkAvailable = { network ->
            viewModelScope.launch {
                _configs.ensureConfigFor(network)
            }
        }

        connectivity.onActiveNetworkChanged = { network ->
            viewModelScope.launch {
                _activeConfig.decideConfig(network)
                updateLiveData()
            }
        }
    }

    fun getConfig(network: NetworkDescriptor): NetworkSpecificConfig {
        return _configs.value!!.first { it.network == network }
    }

    fun getConfigForId(networkId: NetworkId): NetworkSpecificConfig {
        return _configs.value!!.first { it.network.id() == networkId }
    }

    fun getActiveNetworkConfig(): NetworkSpecificConfig {
        return _activeConfig.value!!
    }

    fun hasCustomConfigs(): Boolean {
        return _configs.value!!.any { it.enabled && !it.network.isFallback() }
    }

    fun actionEnable(network: NetworkDescriptor, enabled: Boolean) {
        log.v("actionEnable ($enabled) for ${network.id()}")
        viewModelScope.launch {
            val cfg = getConfig(network).copy(enabled = enabled)
            _configs.replace(cfg)
            updateLiveData()
            if (enabled) _activeConfig.decideConfig()
        }
    }

    fun actionEncryptDns(network: NetworkDescriptor, encryptDns: Boolean) {
        log.v("actionEncryptDns ($encryptDns) for ${network.id()}")
        viewModelScope.launch {
            val cfg = getConfig(network).copy(encryptDns = encryptDns)
            _configs.replace(cfg)
            updateLiveData()
        }
    }

    fun actionUseNetworkDns(network: NetworkDescriptor, useNetworkDns: Boolean) {
        log.v("actionUseNetworkDns ($useNetworkDns) for ${network.id()}")
        viewModelScope.launch {
            val cfg = getConfig(network).copy(useNetworkDns = useNetworkDns)
            _configs.replace(cfg)
            updateLiveData()
        }
    }

    fun actionUseDns(network: NetworkDescriptor, dns: DnsId, useBlockaDnsInPlusMode: Boolean) {
        log.v("actionUseDns ($dns, blockaInPlus: $useBlockaDnsInPlusMode) for ${network.id()}")
        viewModelScope.launch {
            val cfg = getConfig(network).copy(dnsChoice = dns, useBlockaDnsInPlusMode = useBlockaDnsInPlusMode)
            _configs.replace(cfg)
            updateLiveData()
        }
    }

    private fun MutableLiveData<List<NetworkDescriptor>>.add(network: NetworkDescriptor) {
        if (network.type != NetworkType.FALLBACK) {
            var current = value ?: emptyList()
            if (!current.contains(network)) {
                log.v("Detected new network: ${network.id()}")
                current += network
                current = current.sortedBy { it.type.ordinal }
                value = current
            }
        }
    }

    private fun MutableLiveData<List<NetworkSpecificConfig>>.ensureConfigFor(network: NetworkDescriptor) {
        var current = value!!
        val toReplace = current.firstOrNull { it.network == network }
        if (toReplace == null) {
            current += Defaults.networkConfig(network)
            persistence.save(NetworkSpecificConfigs(current))
            value = current
        }
    }

    private fun MutableLiveData<List<NetworkSpecificConfig>>.replace(config: NetworkSpecificConfig) {
        value = value!!.map {
            if (it.network == config.network) config
            else it
        }
        persistence.save(NetworkSpecificConfigs(value!!))

        // Notify about changes to the active network config
        if (config.network == getActiveNetworkConfig().network) {
            _activeConfig.decideConfig()
        }
    }

    private fun MutableLiveData<NetworkSpecificConfig>.decideConfig(new: NetworkDescriptor? = null) {
        val configs = _configs.value!!
        val network = new ?: connectivity.activeNetwork
        value = configs.firstOrNull { it.network == network && it.enabled } ?: run {
            // Find a config for all networks of this type (if enabled)
            configs.firstOrNull {
                it.network.type == network.type && it.enabled && it.network.name == null
            } ?: getFallbackNetworkConfig()
        }
    }

    private fun updateLiveData() {
        viewModelScope.launch {
            // This will cause to emit new event and to refresh the public LiveData
            _configs.value = _configs.value
        }
    }

    private fun getFallbackNetworkConfig() = _configs.value!!.first { it.network.isFallback() }

}