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

package ui.rewards

import com.fulldive.wallet.presentation.base.BaseMvpFragment
import com.joom.lightsaber.getInstance
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import org.adshield.databinding.FragmentRewardsBinding
import ui.StatsViewModel

class RewardsFragment : BaseMvpFragment<FragmentRewardsBinding>(), RewardsView {

    @InjectPresenter
    lateinit var presenter: RewardsPresenter

    override fun getViewBinding() = FragmentRewardsBinding.inflate(layoutInflater)

    @ProvidePresenter
    fun providePresenter(): RewardsPresenter = appInjector.getInstance()
}