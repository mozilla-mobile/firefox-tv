/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View
import mozilla.components.browser.engine.system.SystemEngineView
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.components.locale.Locales
import org.mozilla.tv.firefox.webrender.LocalizedContent

/**
 * A generic activity that supports showing additional information in a WebView. This is useful
 * for showing any web based content, including About/Help/Rights, and also SUMO pages.
 */
class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_info)

        val url = intent.getStringExtra(EXTRA_URL)
        val title = intent.getStringExtra(EXTRA_TITLE)

        supportFragmentManager.beginTransaction()
            .replace(R.id.infofragment, InfoFragment.create(url))
            .commit()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = title

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return if (name == EngineView::class.java.name) {
            SystemEngineView(context, attrs)
        } else super.onCreateView(name, context, attrs)
    }

    companion object {
        private const val PRIVACY_NOTICE_URL = "https://www.mozilla.org/privacy/firefox-fire-tv/"

        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"

        fun getIntentFor(context: Context, url: String, title: String): Intent {
            val intent = Intent(context, InfoActivity::class.java)

            intent.putExtra(EXTRA_URL, url)
            intent.putExtra(EXTRA_TITLE, title)

            return intent
        }

        fun getAboutIntent(context: Context): Intent {
            val resources = Locales.getLocalizedResources(context)
            return getIntentFor(context, LocalizedContent.URL_ABOUT, resources.getString(R.string.menu_about))
        }

        fun getPrivacyNoticeIntent(context: Context): Intent {
            val resources = Locales.getLocalizedResources(context)
            return getIntentFor(context, PRIVACY_NOTICE_URL, resources.getString(R.string.preference_privacy_notice))
        }
    }
}
