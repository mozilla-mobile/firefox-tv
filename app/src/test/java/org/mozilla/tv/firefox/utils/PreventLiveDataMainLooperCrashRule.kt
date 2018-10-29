/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData

/**
 * This rule prevents a crash when unit testing [LiveData].
 *
 * Internally, [LiveData] adds some runnables to the main Android event loop,
 * which does not exist on the JVM.  With this rule active, these are run
 * immediately instead.
 *
 * See: https://medium.com/pxhouse/unit-testing-with-mutablelivedata-22b3283a7819
 */
typealias PreventLiveDataMainLooperCrashRule = InstantTaskExecutorRule
