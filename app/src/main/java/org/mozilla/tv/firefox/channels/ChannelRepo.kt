/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.channels.content.ChannelContent
import org.mozilla.tv.firefox.channels.content.getMusicChannels
import org.mozilla.tv.firefox.channels.content.getNewsChannels
import org.mozilla.tv.firefox.channels.content.getSportsChannels
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import java.util.Collections

private const val PREF_CHANNEL_REPO = "ChannelRepo"

// BlackList Ids
private const val BUNDLED_PINNED_SITES_ID_BLACKLIST = "blacklist_pinned_tiles"
private const val BUNDLED_NEWS_ID_BLACKLIST = "blacklist_news"
private const val BUNDLED_SPORTS_ID_BLACKLIST = "blacklist_sports"
private const val BUNDLED_MUSIC_ID_BLACKLIST = "blacklist_music"

enum class BundleType {
    PINNED_TILES, NEWS_TILES, SPORTS_TILES, MUSIC_TILES
}

/**
 * ChannelRepo abstracts app logic that requires exposures to other repos (e.g. removing a pinned
 * tile channel would require a reference to [PinnedTileRepo].
 *
 * [TileSource] is used to determine which Repo is responsible to handle requested operations
 */
class ChannelRepo(
    context: Context,
    private val pinnedTileRepo: PinnedTileRepo
) {
    private val _sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREF_CHANNEL_REPO, Context.MODE_PRIVATE)

    fun getNewsTiles(): Observable<List<ChannelTile>> =
        bundledNewsTiles.filterNotBlacklisted(blacklistedNewsIds)

    fun getSportsTiles(): Observable<List<ChannelTile>> =
        bundledSportsTiles.filterNotBlacklisted(blacklistedSportsIds)

    fun getMusicTiles(): Observable<List<ChannelTile>> =
        bundledMusicTiles.filterNotBlacklisted(blacklistedMusicIds)

    fun removeChannelContent(tileData: ChannelTile) {
        when (tileData.tileSource) {
            TileSource.BUNDLED, TileSource.CUSTOM -> {
                TelemetryIntegration.INSTANCE.homeTileRemovedEvent(tileData)
                addBundleTileToBlackList(BundleType.PINNED_TILES, tileData.id)
                pinnedTileRepo.removePinnedTile(tileData.url)
            }
            TileSource.POCKET -> throw NotImplementedError("pocket shouldn't be able to remove tiles")
            TileSource.TV_GUIDE -> {

            }
        }
    }

    /**
     * Used to handle removing bundle tiles by adding to its [BundleType] blacklist in SharedPreferences
     */
    private fun addBundleTileToBlackList(type: BundleType, id: String) {
        val blackList = loadBlackList(type).toMutableList()
        blackList.add(id)
        saveBlackList(type, blackList)
    }

    private fun loadBlackList(type: BundleType): List<String> {
        val sharedPrefKey = when (type) {
            BundleType.PINNED_TILES -> BUNDLED_PINNED_SITES_ID_BLACKLIST
            BundleType.NEWS_TILES -> BUNDLED_NEWS_ID_BLACKLIST
            BundleType.SPORTS_TILES -> BUNDLED_SPORTS_ID_BLACKLIST
            BundleType.MUSIC_TILES -> BUNDLED_MUSIC_ID_BLACKLIST
        }

        return _sharedPreferences.getStringSet(sharedPrefKey, Collections.emptySet())!!.toList()
    }

    private fun saveBlackList(type: BundleType, blackList: List<String>) {
        val sharedPrefKey = when (type) {
            BundleType.PINNED_TILES -> BUNDLED_PINNED_SITES_ID_BLACKLIST
            BundleType.NEWS_TILES -> BUNDLED_NEWS_ID_BLACKLIST
            BundleType.SPORTS_TILES -> BUNDLED_SPORTS_ID_BLACKLIST
            BundleType.MUSIC_TILES -> BUNDLED_MUSIC_ID_BLACKLIST
        }

        _sharedPreferences.edit().putStringSet(sharedPrefKey, blackList.toSet()).apply()
    }

    private val bundledNewsTiles = Observable.just(ChannelContent.getNewsChannels())
        .replay(1)
        .autoConnect(0)
    // TODO in #2326 (replace emptyList with blacklist. Push any updates to this subject)
    private val blacklistedNewsIds = BehaviorSubject.createDefault(loadBlackList(BundleType.NEWS_TILES))

    private val bundledSportsTiles = Observable.just(ChannelContent.getSportsChannels())
        .replay(1)
        .autoConnect(0)
    // TODO in #2326 (replace emptyList with blacklist. Push any updates to this subject)
    private val blacklistedSportsIds = BehaviorSubject.createDefault(loadBlackList(BundleType.SPORTS_TILES))

    private val bundledMusicTiles = Observable.just(ChannelContent.getMusicChannels())
        .replay(1)
        .autoConnect(0)
    // TODO in #2326 (replace emptyList with blacklist. Push any updates to this subject)
    private val blacklistedMusicIds = BehaviorSubject.createDefault(loadBlackList(BundleType.MUSIC_TILES))
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun Observable<List<ChannelTile>>.filterNotBlacklisted(
    blacklistIds: Observable<List<String>>
): Observable<List<ChannelTile>> {
    return Observables.combineLatest(this, blacklistIds)
        .map { (tiles, blacklistIds) -> tiles.filter { !blacklistIds.contains(it.id) } }
}
