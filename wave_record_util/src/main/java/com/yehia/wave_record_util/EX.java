package com.yehia.wave_record_util;

import android.content.Context;
import android.content.pm.PackageManager;

class EX {

    public static Boolean checkPermission(String permission, Context context) {
        int result = context.checkCallingOrSelfPermission(permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

}