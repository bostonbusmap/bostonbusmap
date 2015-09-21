/***
 * Copyright (c) 2010 readyState Software Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.readystatesoftware.mapviewballoons;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * A linearlayout which is constrained to 480dp width
 *
 * Most of android-mapviewballoons is obsoleted by the new Maps API
 * but I'm still using this class because it fits what I want to do with 
 * the balloons
 *
 * @author schneg
 *
 */
public class LimitLinearLayout extends LinearLayout {

    private final int maxWidth;

    final float SCALE = getContext().getResources().getDisplayMetrics().density;

    public LimitLinearLayout(Context context) {
        super(context);
        throw new RuntimeException("maxWidth argument required");
    }

    public LimitLinearLayout(Context context, int maxWidth) {
        super(context);

        this.maxWidth = maxWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int adjustedMaxWidth = (int)(maxWidth * SCALE + 0.5f);
        int adjustedWidth = Math.min(measuredWidth, adjustedMaxWidth);
        int adjustedWidthMeasureSpec = MeasureSpec.makeMeasureSpec(adjustedWidth, mode);
        super.onMeasure(adjustedWidthMeasureSpec, heightMeasureSpec);
    }
}