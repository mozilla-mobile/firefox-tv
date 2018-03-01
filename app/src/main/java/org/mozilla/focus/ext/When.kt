/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

/**
 * `when` is only exhaustive when its expression returns a value. Appending this
 * value gives it a return value, forcing the compiler to check `when` for
 * exhaustiveness.
 *
 * One downside is that it pollutes autocomplete, but let's try it out.
 *
 * Idea from SO: https://stackoverflow.com/a/44383076
 */
val Any?.forceExhaustive get() = Unit