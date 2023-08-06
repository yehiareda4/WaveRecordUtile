package com.yehia.wave_record_util

interface OnRecordListener {

    fun onStart() {

    }

    fun onCancel() {

    }

    fun onFinish(recordTime: Long, limitReached: Boolean, file: String?)

    fun onLessThanSecond() {

    }

}