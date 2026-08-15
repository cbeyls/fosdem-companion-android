package be.digitalia.fosdem.inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory

/**
 * Helper interface and methods to allow passing a creation callback trough CreationExtras.
 */

private val CREATION_CALLBACK_KEY =
    CreationExtras.Key<CallbackViewModelAssistedFactory.(CreationExtras) -> ViewModel>()

interface CallbackViewModelAssistedFactory : ViewModelAssistedFactory {
    override fun create(extras: CreationExtras): ViewModel {
        val callback = requireNotNull(extras[CREATION_CALLBACK_KEY]) { "Missing creation callback in CreationExtras" }
        return callback(extras)
    }
}

fun <VMF : CallbackViewModelAssistedFactory> CreationExtras.withCreationCallback(
    callback: VMF.(CreationExtras) -> ViewModel
): CreationExtras = MutableCreationExtras(this).addCreationCallback(callback)

@Suppress("UNCHECKED_CAST")
fun <VMF : CallbackViewModelAssistedFactory> MutableCreationExtras.addCreationCallback(
    callback: VMF.(CreationExtras) -> ViewModel
): CreationExtras = this.apply {
    this[CREATION_CALLBACK_KEY] = callback as CallbackViewModelAssistedFactory.(CreationExtras) -> ViewModel
}
