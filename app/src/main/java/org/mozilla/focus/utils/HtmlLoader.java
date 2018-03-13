package org.mozilla.focus.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HtmlLoader {

    /**
     * Load a given (html or css) resource file into a String. The input can contain tokens that will
     * be replaced with localised strings.
     *
     * @param substitutionTable A table of substitions, e.g. %shortMessage% -> "Error loading page..."
     *                          Can be null, in which case no substitutions will be made.
     * @return The file content, with all substitutions having being made.
     */
    public static String loadResourceFile(@NonNull final Context context,
                                           @NonNull final @RawRes int resourceID,
                                           @Nullable final Map<String, String> substitutionTable) {

        try (final BufferedReader fileReader =
                     new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resourceID), StandardCharsets.UTF_8))) {

            final StringBuilder outputBuffer = new StringBuilder();

            String line;
            while ((line = fileReader.readLine()) != null) {
                if (substitutionTable != null) {
                    for (final Map.Entry<String, String> entry : substitutionTable.entrySet()) {
                        line = line.replace(entry.getKey(), entry.getValue());
                    }
                }

                outputBuffer.append(line);
            }

            return outputBuffer.toString();
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to load error page data", e);
        }
    }

}
