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

import org.eclipse.swt.graphics.RGB;

import com.amazonaws.eclipse.core.util.OsPlatformUtils;

public class AnsiConsoleColorPalette {
    private static final int PALETTE_SIZE = 256;

    // From Wikipedia, http://en.wikipedia.org/wiki/ANSI_escape_code
    private final static RGB[] paletteXP = {
        new RGB(  0,   0,   0), // black
        new RGB(128,   0,   0), // red
        new RGB(  0, 128,   0), // green
        new RGB(128, 128,   0), // brown/yellow
        new RGB(  0,   0, 128), // blue
        new RGB(128,   0, 128), // magenta
        new RGB(  0, 128, 128), // cyan
        new RGB(192, 192, 192), // gray
        new RGB(128, 128, 128), // dark gray
        new RGB(255,   0,   0), // bright red
        new RGB(  0, 255,   0), // bright green
        new RGB(255, 255,   0), // yellow
        new RGB(  0,   0, 255), // bright blue
        new RGB(255,   0, 255), // bright magenta
        new RGB(  0, 255, 255), // bright cyan
        new RGB(255, 255, 255)  // white
    };
    private final static RGB[] paletteMac = {
        new RGB(  0,   0,   0), // black
        new RGB(194,  54,  33), // red
        new RGB( 37, 188,  36), // green
        new RGB(173, 173,  39), // brown/yellow
        new RGB( 73,  46, 225), // blue
        new RGB(211,  56, 211), // magenta
        new RGB( 51, 187, 200), // cyan
        new RGB(203, 204, 205), // gray
        new RGB(129, 131, 131), // dark gray
        new RGB(252,  57,  31), // bright red
        new RGB( 49, 231,  34), // bright green
        new RGB(234, 236,  35), // yellow
        new RGB( 88,  51, 255), // bright blue
        new RGB(249,  53, 248), // bright magenta
        new RGB( 20, 240, 240), // bright cyan
        new RGB(233, 235, 235)  // white
    };
    private final static RGB[] paletteXTerm = {
        new RGB(  0,   0,   0), // black
        new RGB(205,   0,   0), // red
        new RGB(  0, 205,   0), // green
        new RGB(205, 205,   0), // brown/yellow
        new RGB(  0,   0, 238), // blue
        new RGB(205,   0, 205), // magenta
        new RGB(  0, 205, 205), // cyan
        new RGB(229, 229, 229), // gray
        new RGB(127, 127, 127), // dark gray
        new RGB(255,   0,   0), // bright red
        new RGB(  0, 255,   0), // bright green
        new RGB(255, 255,   0), // yellow
        new RGB( 92,  92, 255), // bright blue
        new RGB(255,   0, 255), // bright magenta
        new RGB(  0, 255, 255), // bright cyan
        new RGB(255, 255, 255)  // white
    };
    private static RGB[]  palette            = getDefaultPalette();

    public static boolean isValidIndex(int value) {
        return value >= 0 && value < PALETTE_SIZE;
    }

    static int TRUE_RGB_FLAG = 0x10000000; // Representing true RGB colors as 0x10RRGGBB

    public static int hackRgb(int r, int g, int b) {
        if (!isValidIndex(r)) return -1;
        if (!isValidIndex(g)) return -1;
        if (!isValidIndex(b)) return -1;
        return TRUE_RGB_FLAG | r << 16 | g << 8 | b;
    }

    static int safe256(int value, int modulo) {
        int result = value * PALETTE_SIZE / modulo;
        return result < PALETTE_SIZE ? result : PALETTE_SIZE - 1;
    }

    public static RGB getColor(Integer index) {
        if (null == index)
            return null;

        if (index >= TRUE_RGB_FLAG) {
            int red = index >> 16 & 0xff;
            int green = index >> 8 & 0xff;
            int blue = index & 0xff;
            return new RGB(red, green, blue);
        }

        if (index >= 0 && index < palette.length) // basic, 16 color palette
            return palette[index];

        if (index >= 16 && index < 232) { // 6x6x6 color matrix
            int color = index - 16;
            int blue = color % 6;
            color = color / 6;
            int green = color % 6;
            int red = color / 6;

            return new RGB(safe256(red, 6), safe256(green, 6), safe256(blue, 6));
        }

        if (index >= 232 && index < PALETTE_SIZE) { // grayscale
            int gray = safe256(index - 232, 24);
            return new RGB(gray, gray, gray);
        }

        return null;
    }

    private static RGB[] getDefaultPalette() {
        if (OsPlatformUtils.isWindows()) {
            return paletteXP;
        } else if (OsPlatformUtils.isMac()) {
            return paletteMac;
        } else {
            return paletteXTerm;
        }
    }
}