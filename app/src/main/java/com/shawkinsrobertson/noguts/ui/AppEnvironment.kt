package com.shawkinsrobertson.noguts.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shawkinsrobertson.noguts.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided - wrap the composition in CompositionLocalProvider(LocalAppContainer provides ...)")
}

/**
 * A single generic ViewModelProvider.Factory so each screen can build its ViewModel from
 * the [AppContainer] without a DI framework: `viewModel(factory = viewModelFactory { MyViewModel(container) })`.
 */
class SimpleViewModelFactory(private val create: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
