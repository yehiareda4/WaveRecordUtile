package com.yehia.phonicplayer.utils

import java.util.concurrent.TimeUnit

/**
 * Edit by Yehia Reda on 05/03/2022.
 */

object PlayerUtils {
    fun getDurationFormat(durationn: Long): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(durationn),
            TimeUnit.MILLISECONDS.toSeconds(durationn) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(durationn)
            )
        )
    }
}