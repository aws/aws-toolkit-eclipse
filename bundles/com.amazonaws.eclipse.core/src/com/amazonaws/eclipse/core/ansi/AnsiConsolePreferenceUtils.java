/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.core.ansi;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class AnsiConsolePreferenceUtils {
    private final static String DEBUG_CONSOLE_PLUGIN_ID        = "org.eclipse.debug.ui";
    private final static String DEBUG_CONSOLE_FALLBACK_BKCOLOR = "0,0,0";
    private final static String DEBUG_CONSOLE_FALLBACK_FGCOLOR = "192,192,192";

    static Color colorFromStringRgb(String strRgb) {
        Color result = null;
        String[] splitted = strRgb.split(",");
        if (splitted != null && splitted.length == 3) {
            int red = tryParseInteger(splitted[0]);
            int green = tryParseInteger(splitted[1]);
            int blue = tryParseInteger(splitted[2]);
            result = new Color(null, new RGB(red, green, blue));
        }
        return result;
    }

    public static Color getDebugConsoleBgColor() {
        IPreferencesService ps = Platform.getPreferencesService();
        String value = ps.getString(DEBUG_CONSOLE_PLUGIN_ID, "org.eclipse.debug.ui.consoleBackground",
                DEBUG_CONSOLE_FALLBACK_BKCOLOR, null);
        return colorFromStringRgb(value);
    }

    public static Color getDebugConsoleFgColor() {
        IPreferencesService ps = Platform.getPreferencesService();
        String value = ps.getString(DEBUG_CONSOLE_PLUGIN_ID, "org.eclipse.debug.ui.outColor",
                DEBUG_CONSOLE_FALLBACK_FGCOLOR, null);
        return colorFromStringRgb(value);
    }

    public static int tryParseInteger(String text) {
        if ("".equals(text))
            return -1;

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
