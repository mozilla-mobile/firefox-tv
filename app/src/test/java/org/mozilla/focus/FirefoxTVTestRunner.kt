package org.mozilla.focus

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.manifest.AndroidManifest
import org.robolectric.res.FileFsFile
import java.io.File

private const val APP_MAIN_PATH = "app/src/main"

/**
 * A test runner that configures our project correctly: the default test runner appears to
 * misconfigure AndroidManifest and assets. This workaround is based off of:
 * https://github.com/nenick/AndroidStudioAndRobolectric/blob/master/app/src/test/java/com/example/myapplication/CustomRobolectricRunner.java
 *
 * Misconfigured assets is a known issue: https://github.com/robolectric/robolectric/issues/2647
 *
 * I tried various other workarounds but this is the only one I could get working.
 */
class FirefoxTVTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {

    private val rootPath: String by lazy {
        val workingDir = File(System.getProperty("user.dir"))

        // When running from the IDE, the working directory is already the project root.
        (if (isProjectRoot(workingDir)) {
            workingDir
        } else {
            // When running from the command line, the working directory is the current module
            // under test. This is a known issue: https://issuetracker.google.com/issues/37030346
            workingDir.parentFile
        }).path
    }

    override fun getAppManifest(config: Config): AndroidManifest {
        val appManifest = super.getAppManifest(config)
        val preconfiguredManifestFile = appManifest.androidManifestFile

        if (preconfiguredManifestFile.exists()) {
            return appManifest
        }

        // TODO: It's bad practice to hard-code module paths. In fact,
        // we might actually want to be looking in the build dir for these.
        // TODO: The real manifest fails to inflate so we pass in null.
        val resDirectory = FileFsFile.from(rootPath, APP_MAIN_PATH, "res")
        val assetsDirectory = FileFsFile.from(rootPath, APP_MAIN_PATH, "assets")
        return AndroidManifest(null, resDirectory, assetsDirectory)
    }
}

private fun isProjectRoot(file: File) = file.list().contains("gradlew")
