package com.yehia.wave.views

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.github.rahatarmanahmed.cpv.CircularProgressView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.yehia.wave.handler.MediaPlayerHolder
import com.yehia.wave.listener.OnPlaybackInfoListener
import com.yehia.wave.listener.OnPlayerViewClickListener
import com.yehia.wave.utils.PlayerAdapter
import com.yehia.wave.utils.PlayerTarget
import com.yehia.wave.utils.PlayerUtils
import com.yehia.wave_record_util.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Edit by Yehia Reda on 05/03/2022.
 */
class WavePlayerView : RelativeLayout {

    private var asView: Boolean = false

    private lateinit var url: String
    private var activity: Activity? = null
    private var centerDuration: TextView? = null
    private var playerRootView: View? = null
    private var mCircleProgressBar: CustomProgressBar? = null
    private var mCircleProgressBarDownload: CustomProgressBar? = null
    private var mErrorButton: ImageView? = null
    private var mSeekBar: WaveformSeekBar? = null
    private var mPlayButton: ImageView? = null
    private var mPauseButton: ImageView? = null
    private var mChronometer: CustomChronometer? = null
    private var mDuration: TextView? = null
    private var mPlayerAdapter: PlayerAdapter? = null
    private var mTarget: PlayerTarget? = null
    private var mLoader: CircularProgressView? = null
    private var customLayout = 0
    private var mContext: Context? = null
    private var mStringName = ""
    private var mStringURL = ""
    private var mStringDirectory = ""
    private var btnIcon = 0
    private var seekSelector = 0
    private val playerViewClickListenersArray = SparseArray<OnPlayerViewClickListener>()

    private var durationStart: Boolean = true
    private var durationEnd: Boolean = true
    private var duration: String = "0"

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        getAttributes(context, attrs)
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        getAttributes(context, attrs)
        init(context)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        getAttributes(context, attrs)
        init(context)
    }

    private fun getAttributes(context: Context, attrs: AttributeSet) {
        val ta = context.theme.obtainStyledAttributes(attrs, R.styleable.PhonicPlayerViewAtt, 0, 0)
        customLayout = ta.getResourceId(
            R.styleable.PhonicPlayerViewAtt_custom_layout, R.layout.view_audio_player_3
        )
        btnIcon = ta.getResourceId(
            R.styleable.PhonicPlayerViewAtt_btn_color, R.color.colorPrimary
        )
        seekSelector = ta.getResourceId(
            R.styleable.PhonicPlayerViewAtt_seek_selector, R.drawable.seekbar_bg
        )
        durationStart = ta.getBoolean(
            R.styleable.PhonicPlayerViewAtt_duration_start, true
        )
        durationEnd = ta.getBoolean(
            R.styleable.PhonicPlayerViewAtt_duration_end, true
        )
    }

    fun setAudioTarget(uri: Uri?, activity: Activity) {
        this.activity = activity
        mTarget = PlayerTarget.Builder().withLocalFile(uri).build()
        mSeekBar?.setSampleFrom(uri!!)
    }

    fun setAudioTargetAsView(uri: String?, activity: Activity, duration: String = "") {
        this.activity = activity
        asView = true
        this.url = uri ?: ""
        if (url.isNotEmpty()) {
            mTarget = PlayerTarget.Builder().withRemoteUrl(url).build()
        }
        mDuration!!.text = duration
//        runBlocking(Dispatchers.IO) {
//            mSeekBar?.setSampleFrom(url)
//        }
    }

    fun setAudioTargetAsItemRecycle(uri: String?, activity: Activity, duration: String = "") {
        this.activity = activity
        asView = false
        this.url = uri ?: ""
        if (url.isNotEmpty()) {
            mTarget = PlayerTarget.Builder().withRemoteUrl(url).build()
            mSeekBar?.setSampleFrom(uri!!)
        }

        mDuration!!.text = duration
    }

    fun setAudioTarget(resource: Int, activity: Activity) {
        this.activity = activity
        mTarget = PlayerTarget.Builder().withResource(resource).build()
    }

    fun setColor(color: Int, waveColor: Int) {
        mSeekBar?.markerColor = ContextCompat.getColor(context, waveColor)
        mSeekBar?.markerTextColor = ContextCompat.getColor(context, color)
        mSeekBar?.waveProgressColor = ContextCompat.getColor(context, waveColor)
        mPlayButton?.setColorFilter(
            ContextCompat.getColor(context, color), android.graphics.PorterDuff.Mode.MULTIPLY
        )
        mPauseButton?.setColorFilter(
            ContextCompat.getColor(context, color), android.graphics.PorterDuff.Mode.MULTIPLY
        )
        mDuration?.setTextColor(ContextCompat.getColor(context!!, color))
    }

    fun setAudioTarget(url: String, activity: Activity, duration: String = "") {
        this.activity = activity
        if (url.isNotEmpty()) {
            this.url = url
            mTarget = PlayerTarget.Builder().withRemoteUrl(url).build()
        }
        mDuration!!.text = duration
    }

    fun setAudioTarget(url: String, name: String, activity: Activity) {
        this.activity = activity
        mStringName = name
        mStringURL = url
        if (isFileExist("$folderDirectory/$mStringName")) {
            val mUri = Uri.parse("$folderDirectory/$mStringName")
            mTarget = PlayerTarget.Builder().withLocalFile(mUri).build()
        } else {
            mPlayButton!!.setImageResource(R.drawable.icon_download)
        }
    }

    /**
     * Registers click listener for view by id
     *
     * @param viewId                    view
     * @param onPlayerViewClickListener click listener.
     */
    fun registerViewClickListener(
        viewId: Int, onPlayerViewClickListener: OnPlayerViewClickListener
    ) {
        playerViewClickListenersArray.append(viewId, onPlayerViewClickListener)
    }

    fun commitClickEvents() {
        for (i in 0 until playerViewClickListenersArray.size()) {
            val key = playerViewClickListenersArray.keyAt(i)
            val view = playerRootView!!.findViewById<View>(key)
            view?.setOnClickListener { playerViewClickListenersArray[key].onPlayerViewClick(view) }
        }
    }

    /**
     * We only load the media file whe the play button is clicked the first time
     */
    private fun init(context: Context?) {
        val config = PRDownloaderConfig.newBuilder().setDatabaseEnabled(true).build()
        PRDownloader.initialize(context, config)

        mContext = context
        playerRootView = View.inflate(context, customLayout, this)
        mSeekBar = findViewById(R.id.seekbar_audio)
        mCircleProgressBar = findViewById(R.id.progressBar)
        mCircleProgressBarDownload = findViewById(R.id.progressBar_download)
        mPlayButton = findViewById(R.id.button_play)
        mErrorButton = findViewById(R.id.button_error)
        mPauseButton = findViewById(R.id.button_pause)
        mChronometer = findViewById(R.id.current_duration)
        mDuration = findViewById(R.id.total_duration)
        centerDuration = findViewById(R.id.center_duration)
        mLoader = findViewById(R.id.loader_audio)

//        mStringDirectory = mContext!!.getString(R.string.app_name)
        initializePlaybackController()
        mPlayButton?.setColorFilter(
            ContextCompat.getColor(mContext!!, btnIcon), android.graphics.PorterDuff.Mode.MULTIPLY
        )
        mPauseButton?.setColorFilter(
            ContextCompat.getColor(mContext!!, btnIcon), android.graphics.PorterDuff.Mode.MULTIPLY
        )
//        setColor(btnIcon, R.color.colorPrimary)

        if (!durationStart) {
            mChronometer!!.visibility = GONE
            centerDuration!!.visibility = GONE
        }

        if (!durationEnd) {
            mDuration!!.visibility = GONE
            centerDuration!!.visibility = GONE
        }

        mSeekBar?.sample = mSeekBar?.context?.resources?.getIntArray(R.array.wave)

        mSeekBar?.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(
                waveformSeekBar: WaveformSeekBar, progress: Float, fromUser: Boolean
            ) {
                if (fromUser) {
                    mPlayerAdapter?.seekTo(progress.toInt())
                }
            }
        }
        mSeekBar?.waveGap

        mPauseButton?.setOnClickListener {
            if (mPlayerAdapter != null) {
                mPlayerAdapter!!.pause()
                mPlayButton?.visibility = View.VISIBLE
                mPauseButton?.visibility = View.GONE
            }
        }/*mPlayButton?.setOnClickListener {
            if (!mPlayerAdapter!!.hasTarget(mTarget)) {
                val handler = Handler(Looper.myLooper()!!)
                handler.postDelayed({ // Do something after 5s = 5000ms
                    if (checkAndRequestPermissions()) {
                        if (!mPlayerAdapter?.isPlaying!!) {
                            if (mStringName.isNotEmpty() && !isFileExist("$folderDirectory/$mStringName")) {
                                downloadFile(mStringURL, mStringName)
                            } else {
                                mPlayButton?.visibility = View.GONE
                                mLoader?.visibility = View.VISIBLE
                                mLoader?.startAnimation()
                                if (mTarget != null) {
                                    if (!mPlayerAdapter!!.hasTarget(mTarget)) {
                                        val urlFile = when (mTarget!!.targetType) {
                                            PlayerTarget.Type.RESOURCE -> {
                                                (mTarget!!.resource).toString()
                                            }

                                            PlayerTarget.Type.REMOTE_FILE_URL -> {
                                                (mTarget!!.remoteUrl).toString()
                                            }

                                            PlayerTarget.Type.LOCAL_FILE_URI -> {
                                                Log.e(
                                                    "MEDIAPLAY_HOLDER_TAG", "Type is LOCAL_FILE_URI"
                                                )
                                                val audioFile = File(mTarget!!.fileUri.toString())
                                                if (audioFile.exists()) {
                                                    (mTarget!!.fileUri.toString())
                                                } else ""
                                            }

                                            else -> ""
                                        }

                                        mPlayerAdapter!!.reset(false)
                                        initializePlaybackController()
                                        mPlayerAdapter!!.loadMedia(mTarget)
                                        runBlocking(Dispatchers.IO) {
                                            mSeekBar?.setSampleFrom(url)
                                        }
                                    }
                                    mPlayerAdapter!!.play()
                                }
                            }
                        }
                    }
                }, 2000)
            } else {
                mPlayerAdapter!!.play()
            }
        }*/
        mPlayButton?.setOnClickListener {
            if (checkAndRequestPermissions()) {
                mPlayButton?.visibility = View.GONE
                if (!mPlayerAdapter!!.hasTarget(mTarget)) {
                    mLoader?.visibility = View.VISIBLE
                    mLoader?.startAnimation()
                    val handler = Handler(Looper.myLooper()!!)
                    handler.postDelayed({ // Do something after 5s = 5000ms
                        if (!mPlayerAdapter?.isPlaying!!) {
                            if (mStringName.isNotEmpty() && !isFileExist("$folderDirectory/$mStringName")) {
                                downloadFile(mStringURL, mStringName)
                            } else {
                                if (mTarget != null) {
                                    if (!mPlayerAdapter!!.hasTarget(mTarget)) {
                                        val urlFile = when (mTarget!!.targetType) {
                                            PlayerTarget.Type.RESOURCE -> {
                                                (mTarget!!.resource).toString()
                                            }

                                            PlayerTarget.Type.REMOTE_FILE_URL -> {
                                                (mTarget!!.remoteUrl).toString()
                                            }

                                            PlayerTarget.Type.LOCAL_FILE_URI -> {
                                                Log.e(
                                                    "MEDIAPLAY_HOLDER_TAG", "Type is LOCAL_FILE_URI"
                                                )
                                                val audioFile = File(mTarget!!.fileUri.toString())
                                                if (audioFile.exists()) {
                                                    (mTarget!!.fileUri.toString())
                                                } else ""
                                            }

                                            else -> ""
                                        }

                                        mPlayerAdapter!!.reset(false)
                                        initializePlaybackController()
                                        mPlayerAdapter!!.loadMedia(mTarget)
                                        runBlocking(Dispatchers.IO) {
                                            mSeekBar?.setSampleFrom(url)
                                        }
                                    }
                                    mPlayerAdapter!!.play()
                                }
                            }
                        }
                    }, 2000)
                } else {
                    mPlayerAdapter!!.play()
                }
            }
        }
    }

    private fun initializePlaybackController() {
        val mMediaPlayerHolder: MediaPlayerHolder = MediaPlayerHolder.getInstance(context)!!
        mMediaPlayerHolder.setPlaybackInfoListener(OnPlaybackListener())
        mPlayerAdapter = mMediaPlayerHolder
    }

    fun setTotalDuration(duration: Long) {
        try {
            if (mDuration != null) mDuration!!.text = PlayerUtils.getDurationFormat(duration * 1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDownloadDirectory(directoryName: String) {
        mStringDirectory = directoryName
    }

    fun setTotalDurationFromPath(path: String?) {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(mContext, Uri.parse(path))
        val duration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val millisecond = duration!!.toLong()
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisecond)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisecond)
        val totalDurations = String.format(
            "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millisecond), seconds - minutes * 60
        )
        if (mDuration != null) mDuration!!.text = totalDurations
    }

    fun reset() {
        mPlayButton!!.visibility = View.VISIBLE
        mPauseButton!!.visibility = View.GONE
        if (mChronometer != null) mChronometer!!.reset()
    }

    fun stop() {
        if (mPlayerAdapter != null) {
            mPlayerAdapter!!.reset(false)
            mPlayerAdapter!!.release()
            if (mChronometer != null) mChronometer!!.reset()
        }
    }

    inner class OnPlaybackListener : OnPlaybackInfoListener() {
        override fun onDurationChanged(duration: Int) {
//            mLoader?.visibility = View.GONE
//            mPauseButton?.visibility = VISIBLE
            mCircleProgressBar?.setMax(duration)
            mSeekBar?.maxProgress = duration.toFloat()
            if (mDuration != null) mDuration!!.text =
                PlayerUtils.getDurationFormat(duration.toLong())
        }

        override fun onPositionChanged(position: Int) {
            mCircleProgressBar?.setProgress(position.toFloat())
            mSeekBar?.progress = position.toFloat()
        }

        override fun onStateChanged(state: Int) {
            val stateToString: String = convertStateToString(state)
            onLogUpdated(String.format("onStateChanged(%s)", stateToString))
            if (state == State.RESET) {
                reset()
            } else if (state == State.COMPLETED) {
                mPlayButton!!.visibility = View.VISIBLE
                mPauseButton!!.visibility = View.GONE
                if (mChronometer != null) mChronometer!!.reset()
            } else if (state == State.PLAYING) {
                mLoader?.stopAnimation()
                mPlayButton!!.visibility = View.GONE
                mLoader?.visibility = View.GONE
                mPauseButton?.visibility = VISIBLE
                if (mChronometer != null) mChronometer!!.start()
            } else if (state == State.PAUSED) {
                if (mChronometer != null) mChronometer!!.stop()
            } else {
                if (mChronometer != null) mChronometer!!.stop()

                mPlayButton?.visibility = View.GONE
                mPauseButton?.visibility = View.GONE
                mErrorButton?.visibility = View.VISIBLE
            }
        }

        override fun onPlaybackCompleted() {
        }

        override fun onLogUpdated(message: String?) {
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    private fun downloadFile(urlFile: String, name: String) {
        val url = URL(urlFile)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        val code: Int = connection.responseCode

        if (code == 200) {
            mCircleProgressBarDownload?.visibility = View.VISIBLE
            PRDownloader.download(urlFile, folderDirectory, name).build()
                .setOnStartOrResumeListener { }.setOnProgressListener { progress ->
                    val mFloat = progress.currentBytes.toFloat()
                    val mPercentage = mFloat / progress.totalBytes * 100
                    mCircleProgressBarDownload?.setProgress(mPercentage)
                }.start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        mPlayButton!!.setImageResource(R.drawable.icon_play_audio)
                        mCircleProgressBarDownload?.visibility = View.GONE
                        val mUri = Uri.parse("$folderDirectory/$mStringName")
                        mTarget = PlayerTarget.Builder().withLocalFile(mUri).build()
                    }

                    override fun onError(error: com.downloader.Error?) {
                        mCircleProgressBarDownload?.visibility = View.GONE
                    }
                })
        } else {
            mErrorButton?.visibility = View.VISIBLE
        }
    }

    private fun isFileExist(path: String): Boolean {
        val mFile = File(path)
        return mFile.exists()
    }

    /**
     * Method call will get image folder name
     *
     * @return path
     */
    val folderDirectory: String
        get() {
            var appPath = ""
            try {
                val sdCardRoot = Environment.getExternalStorageDirectory()
                val folder = File(
                    sdCardRoot, "$mStringDirectory/audio/"
                )
                if (!folder.exists()) folder.mkdirs()
                appPath = folder.absolutePath
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return appPath
        }
    /**
     * Method call will check record and storage permission are granted or not
     *
     * @return true or false
     */
    /**
     * Method call will check camera and storage permission are granted or not
     *
     * @return true or false
     */
    private fun checkAndRequestPermissions(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            mContext!!, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it, listPermissionsNeeded.toTypedArray(), 101
                )
            }
            return false
        }
        return true
    }

    fun stopPlaying() {
        if (mPlayerAdapter!!.isPlaying) {
            mPlayerAdapter!!.pause()
            mPlayerAdapter!!.release()
        }
    }

}