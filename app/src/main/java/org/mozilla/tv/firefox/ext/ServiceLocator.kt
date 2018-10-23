/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.app.Activity
import android.app.Application
import android.content.Context
import android.support.v4.app.Fragment
import org.mozilla.tv.firefox.FirefoxApplication
import org.mozilla.tv.firefox.utils.ServiceLocator

val Application.serviceLocator: ServiceLocator
    get() = (this as FirefoxApplication).serviceLocator

val Activity.serviceLocator: ServiceLocator
    get() = this.application.serviceLocator

val Context.serviceLocator: ServiceLocator
    get() = this.application.serviceLocator

val Fragment.serviceLocator: ServiceLocator?
    get() = this.context?.serviceLocator
