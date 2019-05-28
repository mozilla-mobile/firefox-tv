/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.channels.content.ChannelContent
import org.mozilla.tv.firefox.channels.content.getMusicChannels
import org.mozilla.tv.firefox.channels.content.getNewsChannels
import org.mozilla.tv.firefox.channels.content.getSportsChannels
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

/**
 * ChannelRepo abstracts app logic that requires exposures to other repos (e.g. removing a pinned
 * tile channel would require a reference to [PinnedTileRepo].
 *
 * [TileSource] is used to determine which Repo is responsible to handle requested operations
 */
class ChannelRepo(
    private val pinnedTileRepo: PinnedTileRepo
) {

    fun getNewsTiles(): Observable<List<ChannelTile>> =
        bundledNewsTiles.filterNotBlacklisted(blacklistedNewsIds)
            .map { it.padToTen() }

    fun getSportsTiles(): Observable<List<ChannelTile>> =
        bundledSportsTiles.filterNotBlacklisted(blacklistedSportsIds)
            .map { it.padToTen() }

    fun getMusicTiles(): Observable<List<ChannelTile>> =
        bundledMusicTiles.filterNotBlacklisted(blacklistedMusicIds)
            .map { it.padToTen() }

    fun removeChannelContent(tileData: ChannelTile) {
        when (tileData.tileSource) {
            TileSource.BUNDLED, TileSource.CUSTOM -> {
                TelemetryIntegration.INSTANCE.homeTileRemovedEvent(tileData)
                pinnedTileRepo.removePinnedTile(tileData.url)
            }
            TileSource.POCKET -> throw NotImplementedError("pocket shouldn't be able to remove tiles")
            TileSource.TV_GUIDE -> Unit // TODO in #2326
        }
    }

    private val bundledNewsTiles = Observable.just(ChannelContent.getNewsChannels())
        .replay(1)
        .autoConnect(0)
    // TODO in #2326 (replace emptyList with blacklist. Push any updates to this subject)
    private val blacklistedNewsIds = BehaviorSubject.createDefault(emptyList<String>())

    private val bundledSportsTiles = Observable.just(ChannelContent.getSportsChannels())
        .replay(1)
        .autoConnect(0)
    // TODO in #2326 (replace emptyList with blacklist. Push any updates to this subject)
    private val blacklistedSportsIds = BehaviorSubject.createDefault(emptyList<String>())

    private val bundledMusicTiles = Observable.just(ChannelContent.getMusicChannels())
        .replay(1)
        .autoConnect(0)
    // TODO in #2326 (replace emptyList with blacklist. Push any updates to this subject)
    private val blacklistedMusicIds = BehaviorSubject.createDefault(emptyList<String>())
}

private fun Observable<List<ChannelTile>>.filterNotBlacklisted(
    blacklistIds: Observable<List<String>>
): Observable<List<ChannelTile>> {
    return Observables.combineLatest(this, blacklistIds)
        .map { (tiles, blacklistIds) -> tiles.filter { !blacklistIds.contains(it.id) } }
}

/**
 * Temporary method that will be removed before shipping. This allows us to test the UX
 * for channels that do not yet have 10 approved sites
 *
 * See #2195 ("For categories that don't have 10 tiles, add duplicates of approved
 * channels so we have an idea of what they look like.")
 */
private fun List<ChannelTile>.padToTen(): List<ChannelTile> {
    return (0 until 10)
        .flatMap { this }
        .take(10)
}
