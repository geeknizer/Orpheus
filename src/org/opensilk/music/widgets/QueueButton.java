/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.opensilk.common.util.ThemeUtils;
import org.opensilk.music.R;

/**
 * Created by drew on 3/16/14.
 */
public class QueueButton extends ImageView {

    private final int mQueueButton;
    private final Drawable mQueueButtonActiveDrawable;

    public QueueButton(Context context) {
        this(context, null);
    }

    public QueueButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QueueButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final boolean isLightTheme = ThemeUtils.isLightTheme(getContext());
        if (isLightTheme) {
            mQueueButton = R.drawable.ic_action_queue_holo_light;
        } else {
            mQueueButton = R.drawable.ic_action_queue_holo_dark;
        }
        mQueueButtonActiveDrawable = ThemeUtils.colorizeBitmapDrawableCopy(
                    getContext(),
                    R.drawable.ic_action_queue_holo_dark,
                    ThemeUtils.getColorAccent(getContext()
                )
        );
    }

    public void setQueueShowing(boolean showing) {
        if (showing) {
            setImageDrawable(mQueueButtonActiveDrawable);
        } else {
            setImageResource(mQueueButton);
        }
    }
}
