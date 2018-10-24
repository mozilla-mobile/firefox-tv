/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import org.mozilla.tv.firefox.utils.ServiceLocator

/**
 * Used by [ViewModelProviders] to instantiate [ViewModel]s with constructor arguments.
 *
 * Example usage:
 * ```kotlin
 * val factory = ViewModelFactory(serviceLocator)
 * val myViewModel = ViewModelProviders.of(this, factory).get(ExampleViewModel::class.java)
 * ```
 */
class ViewModelFactory(private val serviceLocator: ServiceLocator) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
//            PocketViewModel::class -> PocketViewModel(serviceLocator.pocketRepo) as T // TODO this class is not present in master
            Object::class -> "" as T // TODO remove me. I let the class pass lint
            // This class needs to either return a ViewModel or throw, so we have no good way of silently handling
            // failures in production. However a failure could only occur if code requests a VM that we have not added
            // to this factory, so any problems should be caught in dev.
            else -> throw IllegalArgumentException("A class was passed to ViewModelFactory#create that it does not " +
                "know how to handle")
        }
    }
}
