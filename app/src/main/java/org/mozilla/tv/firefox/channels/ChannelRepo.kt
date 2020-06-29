/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.channels.content.ChannelContent
import org.mozilla.tv.firefox.channels.content.getMusicChannels
import org.mozilla.tv.firefox.channels.content.getNewsChannels
import org.mozilla.tv.firefox.channels.content.getSportsChannels
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileImageUtilWrapper
import org.mozilla.tv.firefox.channels.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.FormattedDomainWrapper
import java.util.Collections

private const val PREF_CHANNEL_REPO = "ChannelRepo"

// BlackList Ids
private const val BUNDLED_PINNED_SITES_ID_BLACKLIST = "blacklist_pinned_tiles"
private const val BUNDLED_NEWS_ID_BLACKLIST = "blacklist_news"
private const val BUNDLED_SPORTS_ID_BLACKLIST = "blacklist_sports"
private const val BUNDLED_MUSIC_ID_BLACKLIST = "blacklist_music"

/**
 * ChannelRepo abstracts app logic that requires exposures to other repos (e.g. removing a pinned
 * tile channel would require a reference to [PinnedTileRepo].
 *
 * [TileSource] is used to determine which Repo is responsible to handle requested operations
 */
class ChannelRepo(
    application: Application,
    imageUtilityWrapper: PinnedTileImageUtilWrapper,
    formattedDomainWrapper: FormattedDomainWrapper,
    private val pinnedTileRepo: PinnedTileRepo
) {
    private val _sharedPreferences: SharedPreferences =
        application.getSharedPreferences(PREF_CHANNEL_REPO, Context.MODE_PRIVATE)

    fun getPinnedTiles(): Observable<List<ChannelTile>> =
        pinnedTiles.filterNotBlacklisted(blacklistedPinnedIds)

    fun getNewsTiles(): Observable<List<ChannelTile>> =
        bundledNewsTiles.filterNotBlacklisted(blacklistedNewsIds)

    fun getSportsTiles(): Observable<List<ChannelTile>> =
        bundledSportsTiles.filterNotBlacklisted(blacklistedSportsIds)

    fun getMusicTiles(): Observable<List<ChannelTile>> =
        bundledMusicTiles.filterNotBlacklisted(blacklistedMusicIds)

    fun removeChannelContent(tileData: ChannelTile) {
        when (tileData.tileSource) {
            TileSource.CUSTOM -> {
                TelemetryIntegration.INSTANCE.homeTileRemovedEvent(tileData)
                pinnedTileRepo.removePinnedTile(tileData.url)
            }
            TileSource.BUNDLED -> {
                TelemetryIntegration.INSTANCE.homeTileRemovedEvent(tileData) // TODO: verify if we need news, sports and music tiles tracked
                addBundleTileToBlackList(tileData.tileSource, tileData.id)
                pinnedTileRepo.removePinnedTile(tileData.url)
            }
            TileSource.NEWS, TileSource.SPORTS, TileSource.MUSIC -> {
                addBundleTileToBlackList(tileData.tileSource, tileData.id)
            }
        }
    }

    /**
     * Used to handle removing bundle tiles by adding to its [BundleType] blacklist in Sha¬redPreferences
     */
    private fun addBundleTileToBlackList(source: TileSource, id: String) {
        val blackList = loadBlackList(source).toMutableSet()
        blackList.add(id)

        when (source) {
            TileSource.BUNDLED -> blacklistedPinnedIds.onNext(blackList)
            TileSource.NEWS -> blacklistedNewsIds.onNext(blackList)
            TileSource.SPORTS -> blacklistedSportsIds.onNext(blackList)
            TileSource.MUSIC -> blacklistedMusicIds.onNext(blackList)
            else -> Unit
        }

        saveBlackList(source, blackList)
    }

    private fun loadBlackList(source: TileSource): Set<String> {
        val sharedPrefKey = when (source) {
            TileSource.BUNDLED -> BUNDLED_PINNED_SITES_ID_BLACKLIST
            TileSource.NEWS -> BUNDLED_NEWS_ID_BLACKLIST
            TileSource.SPORTS -> BUNDLED_SPORTS_ID_BLACKLIST
            TileSource.MUSIC -> BUNDLED_MUSIC_ID_BLACKLIST
            else -> throw NotImplementedError("other types shouldn't be able remove tiles")
        }

        return _sharedPreferences.getStringSet(sharedPrefKey, Collections.emptySet())!!
    }

    private fun saveBlackList(source: TileSource, blackList: Set<String>) {
        val sharedPrefKey = when (source) {
            TileSource.BUNDLED -> BUNDLED_PINNED_SITES_ID_BLACKLIST
            TileSource.NEWS -> BUNDLED_NEWS_ID_BLACKLIST
            TileSource.SPORTS -> BUNDLED_SPORTS_ID_BLACKLIST
            TileSource.MUSIC -> BUNDLED_MUSIC_ID_BLACKLIST
            else -> throw NotImplementedError("other types shouldn't be able remove tiles")
        }

        _sharedPreferences.edit().putStringSet(sharedPrefKey, blackList.toSet()).apply()
    }

    private val pinnedTiles = pinnedTileRepo.pinnedTiles
        // This takes place off of the main thread because PinnedTile.toChannelTile needs
        // to perform file access, and blocks to do so
        .observeOn(Schedulers.io())
        .map { it.values.map { it.toChannelTile(imageUtilityWrapper, formattedDomainWrapper) } }
        .observeOn(AndroidSchedulers.mainThread())
    private val blacklistedPinnedIds = BehaviorSubject.createDefault(loadBlackList(TileSource.BUNDLED))
    private val bundledNewsTiles = Observable.just(ChannelContent.getNewsChannels())
        .replay(1)
        .autoConnect(0)
    private val blacklistedNewsIds = BehaviorSubject.createDefault(loadBlackList(TileSource.NEWS))

    private val bundledSportsTiles = Observable.just(ChannelContent.getSportsChannels())
        .replay(1)
        .autoConnect(0)
    private val blacklistedSportsIds = BehaviorSubject.createDefault(loadBlackList(TileSource.SPORTS))

    private val bundledMusicTiles = Observable.just(ChannelContent.getMusicChannels())
        .replay(1)
        .autoConnect(0)
    private val blacklistedMusicIds = BehaviorSubject.createDefault(loadBlackList(TileSource.MUSIC))
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun Observable<List<ChannelTile>>.filterNotBlacklisted(
    blacklistIds: Observable<Set<String>>
): Observable<List<ChannelTile>> {
    return Observables.combineLatest(this, blacklistIds)
        .map { (tiles, blacklistIds) -> tiles.filter { !blacklistIds.contains(it.id) } }
}
