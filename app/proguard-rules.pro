
# We do not want to obfuscate - It's just painful to debug without the right mapping file.
-dontobfuscate


##### Default proguard settings:

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/sebastian/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep public class com.amazon.android.webkit.android.AndroidWebKitFactory { public *; }
-keep public class com.amazon.android.webkit.embedded.EmbeddedWebKitFactory { public *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Added dontwarn command due to this warning message on Taskcluster
# Warning: com.amazon.android.webkit.android.PreJellyBeanWebViewReflection: can't find referenced class android.webkit.WebBackForwardListClient
-dontwarn android.webkit.WebBackForwardListClient

####################################################################################################
# Android architecture components
####################################################################################################

# https://developer.android.com/topic/libraries/architecture/release-notes.html
# According to the docs this won't be needed when 1.0 of the library is released.
-keep class * implements android.arch.lifecycle.GeneratedAdapter {<init>(...);}
