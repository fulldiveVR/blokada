package com.fulldive.wallet.presentation.base

import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import io.reactivex.disposables.CompositeDisposable
import moxy.MvpPresenter
import org.adshield.R
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException

abstract class BaseMoxyPresenter<View : BaseMoxyView>
    : MvpPresenter<View>(), ICompositable {

    override val compositeDisposable by lazy { CompositeDisposable() }

    override val defaultOnErrorConsumer: (Throwable) -> Unit by lazy { OnErrorConsumer() }

    @CallSuper
    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    open inner class OnErrorConsumer : BaseOnErrorConsumer() {

        protected fun logError(error: Throwable) {
            val errorMessage = error.message.orEmpty()
            Timber.w("$errorMessage >> ${Log.getStackTraceString(error)}")
        }

        @MainThread
        override fun onError(error: Throwable) {
            logError(error)
            when (error) {
                is UnknownHostException,
                is IOException -> onNoConnectionError()
                else -> error.message?.let(viewState::showMessage)
            }
        }

        @MainThread
        override fun onNoConnectionError() {
            viewState.showMessage(R.string.error_unknown)
        }
    }
}