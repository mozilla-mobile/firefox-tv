/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla

import kotlin.js.Promise

// Types for the JavaScript API provided by Danger: https://danger.systems/js/reference.html

external fun fail(markdownStr: String, fileName: String? = definedExternally, lineNumber: Int? = definedExternally)
external fun markdown(markdownStr: String, fileName: String? = definedExternally, lineNumber: Int? = definedExternally)
external fun message(markdownStr: String, fileName: String? = definedExternally, lineNumber: Int? = definedExternally)
external fun warn(markdownStr: String, fileName: String? = definedExternally, lineNumber: Int? = definedExternally)

@JsName("danger")
external object Danger {
    val git: GitDSL

    val bitbucket_server: BitBucketServerDSL?
    val github: GitHubDSL?

    val utils: DangerUtilsDSL
}

external class GitDSL {
    val commits: Array<GitCommit>

    @JsName("created_files")
    val createdFiles: Array<String>

    @JsName("deleted_files")
    val deletedFiles: Array<String>

    @JsName("modified_files")
    val modifiedFiles: Array<String>

    @JsName("JSONDiffForFile")
    fun jsonDiffForFile(fileName: String): Promise<JSONDiff>

    @JsName("JSONPatchForFile")
    fun jsonPatchForFile(fileName: String): Promise<JSONPatch>

    fun diffForFile(fileName: String): Promise<TextDiff>
    fun structuredDiffForFile(fileName: String): Promise<StructuredFileDiff>
}


external class GitCommit
external class JSONPatch
external class JSONDiff

external class TextDiff {
    val added: String
    val after: String
    val before: String
    val diff: String
    val removed: String
}

external class StructuredFileDiff

external class GitHubDSL
external class BitBucketServerDSL

external class DangerUtilsDSL {
    fun href(href: String?, text: String?): String?
}
