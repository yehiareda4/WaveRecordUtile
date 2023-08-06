package com.yehia.phonicplayer.views

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.Chronometer

/**
 * Edit by Yehia Reda on 05/03/2022.
 */
class CustomChronometer : Chronometer {
    private var timeWhenStopped: Long = 0

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context, attrs, defStyle
    ) {
    }

    override fun start() {
        base = SystemClock.elapsedRealtime() + timeWhenStopped
        super.start()
    }

    override fun stop() {
        super.stop()
        timeWhenStopped = base - SystemClock.elapsedRealtime()
    }

    fun reset() {
        stop()
        base = SystemClock.elapsedRealtime()
        timeWhenStopped = 0
    }

    var currentTime: Long
        get() = timeWhenStopped
        set(time) {
            timeWhenStopped = time
            base = SystemClock.elapsedRealtime() + timeWhenStopped
        }
}