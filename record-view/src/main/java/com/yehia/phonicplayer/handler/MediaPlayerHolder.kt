package com.yehia.phonicplayer.handler

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.yehia.phonicplayer.listener.OnPlaybackInfoListener
import com.yehia.phonicplayer.utils.PlayerAdapter
import com.yehia.phonicplayer.utils.PlayerTarget
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Edit by Yehia Reda on 05/03/2022.
 */

class MediaPlayerHolder(context: Context) : PlayerAdapter, MediaActionHandler {
    private val mContext: Context = context.applicationContext
    private var mMediaPlayer: MediaPlayer? = null
    private var mOnPlaybackInfoListener: OnPlaybackInfoListener? = null
    private var mExecutor: ScheduledExecutorService? = null
    private var mSeekbarPositionUpdateTask: Runnable? = null
    private var mTarget: PlayerTarget? = null

    private fun initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer!!.setOnCompletionListener {
                stopUpdatingCallbackWithPosition(true)
                logToUI("MediaPlayer playback completed")
                if (mOnPlaybackInfoListener != null) {
                    mOnPlaybackInfoListener!!.onStateChanged(OnPlaybackInfoListener.State.COMPLETED)
                    mOnPlaybackInfoListener!!.onPlaybackCompleted()
                }
            }
            logToUI("mMediaPlayer = new MediaPlayer()")
        }
    }

    override fun setPlaybackInfoListener(listenerOn: OnPlaybackInfoListener?) {
        mOnPlaybackInfoListener = listenerOn
    }

    override fun hasTarget(audioTarget: PlayerTarget?): Boolean {
        return mTarget === audioTarget
    }


    private fun removeTarget() {
        mTarget = null
    }

    override fun loadMedia(target: PlayerTarget?) {
        mTarget = target
        initializeMediaPlayer()
        try {
            logToUI("load() {1. setDataSource}")
            when (target!!.targetType) {
                PlayerTarget.Type.RESOURCE -> {
                    Log.e("MEDIAPLAY_HOLDER_TAG", "Type is RESOURCE")
                    val assetFileDescriptor =
                        mContext.resources.openRawResourceFd(target.resource)
                }
                PlayerTarget.Type.REMOTE_FILE_URL -> {
                    Log.e("MEDIAPLAY_HOLDER_TAG", "Type is REMOTE_FILE_URL")
                    mMediaPlayer!!.setDataSource(target.remoteUrl)
                }
                PlayerTarget.Type.LOCAL_FILE_URI -> {
                    Log.e("MEDIAPLAY_HOLDER_TAG", "Type is LOCAL_FILE_URI")
                    val audioFile = File(target.fileUri.toString())
                    if (audioFile.exists()) mMediaPlayer!!.setDataSource(target.fileUri.toString())
                }
            }
        } catch (e: Exception) {
            logToUI(e.toString())
            e.printStackTrace()
        }
        try {
            logToUI("load() {2. prepare}")

            mMediaPlayer!!.prepare()
        } catch (e: Exception) {
            logToUI(e.toString())
            e.printStackTrace()
        }
        initializeProgressCallback()
        logToUI("initializeProgressCallback()")
    }

    override fun release() {
        if (mMediaPlayer != null) {
            logToUI("release() and mMediaPlayer = null")
            mMediaPlayer!!.release()
            mMediaPlayer = null
            removeTarget()
        }
    }

    override val isPlaying: Boolean
        get() = if (mMediaPlayer != null) {
            mMediaPlayer!!.isPlaying
        } else false

    override fun play() {
        if (mMediaPlayer != null && !mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.start()
            if (mOnPlaybackInfoListener != null) {
                mOnPlaybackInfoListener!!.onStateChanged(OnPlaybackInfoListener.State.PLAYING)
            }
            startUpdatingCallbackWithPosition()
        }
    }

    override fun reset(reload: Boolean) {
        if (mMediaPlayer != null) {
            logToUI("playbackReset()")
            mMediaPlayer!!.reset()
            if (reload) loadMedia(mTarget)
            if (mOnPlaybackInfoListener != null) {
                mOnPlaybackInfoListener!!.onStateChanged(OnPlaybackInfoListener.State.RESET)
            }
            stopUpdatingCallbackWithPosition(true)
        }
    }

    override fun pause() {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.pause()
            if (mOnPlaybackInfoListener != null) {
                mOnPlaybackInfoListener!!.onStateChanged(OnPlaybackInfoListener.State.PAUSED)
            }
            logToUI("playbackPause()")
        }
    }

    override fun seekTo(position: Int) {
        if (mMediaPlayer != null) {
            logToUI(String.format("seekTo() %d ms", position))
            mMediaPlayer!!.seekTo(position)
        }
    }

    /**
     * Syncs the mMediaPlayer position with mPlaybackProgressCallback via recurring task.
     */
    private fun startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor()
        }
        if (mSeekbarPositionUpdateTask == null) {
            mSeekbarPositionUpdateTask = Runnable {
                try {
                    updateProgressCallbackTask()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        mExecutor!!.scheduleAtFixedRate(
            mSeekbarPositionUpdateTask,
            0,
            PLAYBACK_POSITION_REFRESH_INTERVAL_MS.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    // Reports media playback position to mPlaybackProgressCallback.
    private fun stopUpdatingCallbackWithPosition(resetUIPlaybackPosition: Boolean) {
        if (mExecutor != null) {
            mExecutor!!.shutdownNow()
            mExecutor = null
            mSeekbarPositionUpdateTask = null
            if (resetUIPlaybackPosition && mOnPlaybackInfoListener != null) {
                mOnPlaybackInfoListener!!.onPositionChanged(0)
            }
        }
    }

    private fun updateProgressCallbackTask() {
        if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
            val currentPosition = mMediaPlayer!!.currentPosition
            if (mOnPlaybackInfoListener != null) {
                mOnPlaybackInfoListener!!.onPositionChanged(currentPosition)
            }
        }
    }

    override fun initializeProgressCallback() {
        val duration = mMediaPlayer!!.duration
        if (mOnPlaybackInfoListener != null) {
            mOnPlaybackInfoListener!!.onDurationChanged(duration)
            mOnPlaybackInfoListener!!.onPositionChanged(0)
        }
    }

    private fun logToUI(message: String) {
        Log.i("MEDIAPLAY_HOLDER_TAG", message)
        if (mOnPlaybackInfoListener != null) {
            mOnPlaybackInfoListener!!.onLogUpdated(message)
        }
    }

    override fun onAction() {
        reset(false)
        release()
    }

    companion object {
        const val PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000

        @SuppressLint("StaticFieldLeak")
        private var mInstance: MediaPlayerHolder? = null

        @Synchronized
        fun getInstance(context: Context): MediaPlayerHolder? {
            if (mInstance == null) {
                mInstance = MediaPlayerHolder(context)
                PlayerListObserver.instance?.registerActionHandler(mInstance)
            }
            return mInstance
        }
    }

}