package com.yehia.wave.utils

import com.yehia.wave.listener.OnPlaybackInfoListener

/**
 * Edit by Yehia Reda on 05/03/2022.
 */

interface PlayerAdapter {
    fun loadMedia(resourceId: PlayerTarget?)
    fun release()
    val isPlaying: Boolean
    fun play()
    fun reset(reload: Boolean)
    fun pause()
    fun initializeProgressCallback()
    fun seekTo(position: Int)
    fun setPlaybackInfoListener(listenerOn: OnPlaybackInfoListener?)
    fun hasTarget(audioTarget: PlayerTarget?): Boolean
}