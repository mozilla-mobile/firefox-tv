/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla

import kotlin.js.Promise

fun main(args: Array<String>) {
    checkJVMLicenseHeaders()
}

private fun isJVMCodeFile(fileName: String): Boolean = fileName.endsWith(".kt") || fileName.endsWith("*.java")

private fun checkJVMLicenseHeaders() {
    val expectedLicenseLines = Licenses.JVM.lines()
    fun hasValidLicense(contents: String): Boolean =
            contents.lines().take(expectedLicenseLines.size) == expectedLicenseLines

    val jvmFiles = Danger.git.createdFiles.filter(::isJVMCodeFile)
    val fileDiffsDeferred = jvmFiles.map { Danger.git.diffForFile(it) }.let { Promise.all(it.toTypedArray()) }

    fileDiffsDeferred.then { fileDiffs ->
        val filesWithoutLicenses = jvmFiles.zip(fileDiffs).filter { (_, diff) ->
            !hasValidLicense(diff.after)
        }.map { it.first }

        filesWithoutLicenses.forEach {
            fail("File contains no license or license is improperly formatted.", it, 1)
        }

        if (filesWithoutLicenses.isNotEmpty()) {
            markdown("## Expected JVM license format\n```\n${Licenses.JVM}\n```")
        }
    }
}
