/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.experiments

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.service.fretboard.Fretboard
import mozilla.components.service.fretboard.ValuesProvider
import mozilla.components.service.fretboard.source.kinto.KintoExperimentSource
import mozilla.components.service.fretboard.storage.flatfile.FlatFileExperimentStorage
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import java.io.File
import kotlin.coroutines.CoroutineContext

const val EXPERIMENTS_JSON_FILENAME = "experiments.json"
const val EXPERIMENTS_BASE_URL = "https://settings.prod.mozaws.net/v1"
const val EXPERIMENTS_BUCKET_NAME = "main"
const val EXPERIMENTS_COLLECTION_NAME = "fftv-experiments"

class FretboardProvider(private val applicationContext: Context) : CoroutineScope {
    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val experimentsFile = File(applicationContext.filesDir, EXPERIMENTS_JSON_FILENAME)
    private val experimentSource = KintoExperimentSource(
            EXPERIMENTS_BASE_URL, EXPERIMENTS_BUCKET_NAME, EXPERIMENTS_COLLECTION_NAME
    )
    val fretboard: Fretboard = Fretboard(experimentSource, FlatFileExperimentStorage(experimentsFile),
            object : ValuesProvider() {
                override fun getClientId(context: Context): String {
                    return TelemetryIntegration.INSTANCE.clientId
                }
            })

    /**
     * Asynchronously requests new experiments from the server and
     * saves them to local storage
     */
    fun updateExperiments() = launch(Dispatchers.IO) { fretboard.updateExperiments() }

    /**
     * Synchronously loads experiments from local storage.
     * This is completed quickly, so we are comfortable blocking in order to
     * reduce complexity by making experiments always available
     */
    fun loadExperiments() = fretboard.loadExperiments()
}
