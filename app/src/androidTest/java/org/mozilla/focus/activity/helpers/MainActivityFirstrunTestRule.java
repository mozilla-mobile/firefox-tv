/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity.helpers;

import android.support.annotation.CallSuper;
import android.support.test.rule.ActivityTestRule;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.session.SessionManager;
import org.mozilla.focus.utils.ThreadUtils;


public class MainActivityFirstrunTestRule extends ActivityTestRule<MainActivity> {
    private boolean showFirstRun;

    public MainActivityFirstrunTestRule(boolean showFirstRun) {
        super(MainActivity.class);

        this.showFirstRun = showFirstRun;
    }

    @CallSuper
    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();
    }

    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();

        getActivity().finishAndRemoveTask();

        ThreadUtils.postToMainThread(new Runnable() {
            @Override
            public void run() {
                SessionManager.getInstance().removeAllSessions();
            }
        });
    }
}
