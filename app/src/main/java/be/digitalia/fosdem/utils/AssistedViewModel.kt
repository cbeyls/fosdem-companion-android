package be.digitalia.fosdem.utils

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

@Suppress("UNCHECKED_CAST")
inline fun simpleViewModelProviderFactory(crossinline viewModelProducer: () -> ViewModel): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>) = viewModelProducer() as T
    }
}

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.assistedViewModels(
    crossinline viewModelProducer: () -> VM
) = viewModels<VM> {
    simpleViewModelProviderFactory(viewModelProducer)
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.assistedViewModels(
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    crossinline viewModelProducer: () -> VM
) = viewModels<VM>(ownerProducer) {
    simpleViewModelProviderFactory(viewModelProducer)
}