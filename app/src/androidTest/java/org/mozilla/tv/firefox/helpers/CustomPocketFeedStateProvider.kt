/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.app.Application
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.pocket.PocketVideoRepo

/**
 * Provides a fake [PocketVideoRepo] implementation for testing purposes.
 *
 * Any values pushed to [fakedPocketRepoState] will be immediately emitted.
 */
class CustomPocketFeedStateProvider(private val appContext: Application) {

    val fakedPocketRepoState = BehaviorSubject.create<PocketVideoRepo.FeedState>()
    val fakedPocketRepo = PocketVideoRepo(
        appContext.serviceLocator.pocketVideoStore,
        isPocketEnabledByLocale = { true },
        _feedState = fakedPocketRepoState
    )
}
