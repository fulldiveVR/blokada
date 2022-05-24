/*
 * Copyright (c) 2022 FullDive
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fulldive.wallet.models

data class ApiHost(val url: String, val port: Int) {
    companion object {
        const val DEFAULT_PORT = 9090
        val EMPTY = ApiHost("", 0)

        fun from(text: String): ApiHost {
            return text.split(":".toRegex()).toTypedArray()
                .let { chunks ->
                    ApiHost(
                        chunks[0],
                        if (chunks.size > 1) chunks[1].toInt() else DEFAULT_PORT
                    )
                }
        }
    }
}