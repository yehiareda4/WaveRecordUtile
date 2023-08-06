package com.yehia.wave_record_util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;


/**
 * Edit by Yehia Reda on 05/01/2022.
 */
//convert from/to DP
public class DpUtil {
    public static float toPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float toDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
