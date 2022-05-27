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

package ui

import androidx.lifecycle.*
import com.fulldive.wallet.interactors.ExperienceExchangeInterator
import kotlinx.coroutines.launch
import model.AdsCounter
import service.PersistenceService
import utils.Logger

class AdsCounterViewModel : ViewModel() {

    private val log = Logger("AdsCounter")
    private val persistence = PersistenceService

    private val _counter = MutableLiveData<AdsCounter>()
    val counter: LiveData<Long> = _counter.distinctUntilChanged().map { it.get() }

    init {
        viewModelScope.launch {
            var counter = persistence.load(AdsCounter::class)
            if (counter.runtimeValue != 0L) {
                log.w("Rolling ads counter loaded from persistence")
                counter = counter.roll()
            }
            _counter.value = counter
        }
    }

    fun setRuntimeCounter(counter: Long, experienceExchangeInterator: ExperienceExchangeInterator) {
        viewModelScope.launch {
            _counter.value?.let { oldValue ->
                val newValue = oldValue.copy(runtimeValue = counter)
                persistence.save(newValue)
                _counter.value = newValue
                experienceExchangeInterator.setExperience(newValue.runtimeValue - oldValue.runtimeValue)
            }
        }
    }

    fun roll() {
        viewModelScope.launch {
            _counter.value?.let {
                log.v("Rolling ads counter: ${it.get()}")
                val new = it.roll()
                persistence.save(new)
                _counter.value = new
            }
        }
    }
}