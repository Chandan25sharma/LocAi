package com.locai.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Tiny factory so ViewModels can take constructor params (our [com.locai.app.LocAiContainer]) without a DI framework. */
class LambdaViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
