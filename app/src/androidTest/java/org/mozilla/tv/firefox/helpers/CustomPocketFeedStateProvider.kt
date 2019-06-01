/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION") // PocketVideoParser

package org.mozilla.tv.firefox.helpers

import android.app.Application
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.pocket.PocketVideoJSONValidator
import org.mozilla.tv.firefox.pocket.PocketVideoParser
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketVideoStore

/**
 * Provides a fake [PocketVideoRepo] implementation for testing purposes.
 *
 * Any values pushed to [fakedPocketRepoState] will be immediately emitted.
 */
class CustomPocketFeedStateProvider(appContext: Application) {

    val fakedPocketRepoState = BehaviorSubject.create<PocketVideoRepo.FeedState>()
    val fakedPocketRepo = PocketVideoRepo(
        PocketVideoStore(
            appContext,
            appContext.assets,
            PocketVideoJSONValidator(PocketVideoParser)
        ),
        isPocketEnabledByLocale = { true },
        _feedState = fakedPocketRepoState
    )
}
