/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.artwork;

import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;

/**
 * Created by drew on 10/31/14.
 */
public class Artwork {
    public final Bitmap bitmap;
    public final Palette palette;
    public Artwork(Bitmap bitmap, Palette palette) {
        this.bitmap = bitmap;
        this.palette = palette;
    }
}
