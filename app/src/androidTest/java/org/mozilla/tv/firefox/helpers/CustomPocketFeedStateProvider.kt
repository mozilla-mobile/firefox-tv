/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers

import android.app.Application
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import org.mozilla.tv.firefox.pocket.PocketVideoParser
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketVideoStore

/**
 * Provides a fake [PocketVideoRepo] implementation for testing purposes.
 *
 * Any values pushed to [fakedPocketRepoState] will be immediately emitted.
 */
class CustomPocketFeedStateProvider(private val appContext: Application) {

    val fakedPocketRepoState = PublishSubject.create<PocketVideoRepo.FeedState>()
    val fakedPocketRepo = object : PocketVideoRepo(
        PocketVideoStore(appContext, appContext.assets, PocketVideoParser::convertVideosJSON),
        isPocketEnabledByLocale = { true },
        isPocketKeyValid = true
    ) {
        override val feedState: Observable<FeedState>
            get() = fakedPocketRepoState
                .observeOn(AndroidSchedulers.mainThread())
    }
}
