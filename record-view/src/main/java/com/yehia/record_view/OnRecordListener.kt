package com.yehia.record_view

interface OnRecordListener {

    fun onStart() {

    }

    fun onCancel() {

    }

    fun onFinish(recordTime: Long, limitReached: Boolean, file: String?)

    fun onLessThanSecond() {

    }

}