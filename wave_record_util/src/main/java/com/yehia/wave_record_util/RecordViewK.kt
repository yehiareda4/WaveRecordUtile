//package com.yehia.record_view
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.ContentValues
//import android.content.Context
//import android.content.res.AssetFileDescriptor
//import android.database.Cursor
//import android.media.MediaMetadataRetriever
//import android.media.MediaPlayer
//import android.media.MediaRecorder
//import android.net.Uri
//import android.os.*
//import android.provider.MediaStore
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.view.ViewGroup
//import android.widget.Chronometer
//import android.widget.ImageView
//import android.widget.RelativeLayout
//import android.widget.TextView
//import androidx.appcompat.content.res.AppCompatResources
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.content.PermissionChecker
//import io.supercharge.shimmerlayout.ShimmerLayout
//import java.io.IOException
//import java.util.*
//
//open class RecordViewK : RelativeLayout {
//
//    private var contexts: Context
//    private lateinit var handlers: Handler
//
//    private var audiouri: Uri? = null
//    val DEFAULT_CANCEL_BOUNDS = 8 //8dp
//    var fileName: String? = null
//    private var file: ParcelFileDescriptor? = null
//
//    private var smallBlinkingMic: ImageView? = null
//    private var basketImg: ImageView? = null
//    private var counterTime: Chronometer? = null
//    private var slideToCancel: TextView? = null
//    private var slideToCancelLayout: ShimmerLayout? = null
//    private var arrow: ImageView? = null
//    private var initialX = 0f
//    private var basketInitialY: Float = 0f
//    private var difX: Float = 0f
//    private var cancelBounds = DEFAULT_CANCEL_BOUNDS.toFloat()
//    private var startTime: Long = 0
//    private var elapsedTime: Long = 0
//    private var recordListener: OnRecordListener? = null
//    private var recordPermissionHandler: RecordPermissionHandler? = null
//    private var isSwiped = false
//    private var isLessThanSecondAllowed: kotlin.Boolean = false
//    private var isSoundEnabled = true
//    private var RECORD_START = R.raw.record_start
//    private var RECORD_FINISHED = R.raw.record_finished
//    private var RECORD_ERROR = R.raw.record_error
//    private var player: MediaPlayer? = null
//    private var animationHelper: AnimationHelper? = null
//    private var isRecordButtonGrowingAnimationEnabled = true
//    private var shimmerEffectEnabled = true
//    private var timeLimit: Long = -1
//    private var runnable: Runnable? = null
//    private var recordButton: RecordButton? = null
//    private var mediaRecorder: MediaRecorder? = null
//
//    private var canRecord = true
//    private var recordPath = ""
//    var type = "mp4"
//    private var activity: Activity? = null
//
//    constructor(context: Context?) : super(context) {
//        this.contexts = context!!
//        init(this.contexts, null, -1, -1)
//    }
//
//    constructor(context: Context?, attrs: AttributeSet?) : super(
//        context,
//        attrs
//    ) {
//        this.contexts = context!!
//        init(this.contexts, attrs, -1, -1)
//    }
//
//    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
//        context,
//        attrs,
//        defStyleAttr
//    ) {
//        this.contexts = context!!
//        init(this.contexts, attrs, defStyleAttr, -1)
//    }
//
//    constructor(
//        context: Context?,
//        attrs: AttributeSet?,
//        defStyleAttr: Int,
//        defStyleRes: Int
//    ) : super(
//        context,
//        attrs,
//        defStyleAttr, defStyleRes
//    ) {
//        this.contexts = context!!
//        init(this.contexts, attrs, defStyleAttr, defStyleRes)
//    }
//
//    @SuppressLint("CustomViewStyleable")
//    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
//        val view = inflate(context, R.layout.record_view_layout, null)
//        addView(view)
//        val viewGroup = view.parent as ViewGroup
//        viewGroup.clipChildren = false
//        arrow = view.findViewById(R.id.arrow)
//        slideToCancel = view.findViewById(R.id.slide_to_cancel)
//        smallBlinkingMic = view.findViewById(R.id.glowing_mic)
//        counterTime = view.findViewById(R.id.counter_tv)
//        basketImg = view.findViewById(R.id.basket_img)
//        slideToCancelLayout = view.findViewById(R.id.shimmer_layout)
//        hideViews(true)
//        if (attrs != null && defStyleAttr == -1 && defStyleRes == -1) {
//            val typedArray = context.obtainStyledAttributes(
//                attrs, R.styleable.RecordView,
//                defStyleAttr, defStyleRes
//            )
//            val slideArrowResource =
//                typedArray.getResourceId(R.styleable.RecordView_slide_to_cancel_arrow, -1)
//            val slideToCancelText =
//                typedArray.getString(R.styleable.RecordView_slide_to_cancel_text)
//            val slideMarginRight =
//                typedArray.getDimension(R.styleable.RecordView_slide_to_cancel_margin_right, 30f)
//                    .toInt()
//            val counterTimeColor =
//                typedArray.getColor(R.styleable.RecordView_counter_time_color, -1)
//            val arrowColor =
//                typedArray.getColor(R.styleable.RecordView_slide_to_cancel_arrow_color, -1)
//            val cancelBounds =
//                typedArray.getDimensionPixelSize(R.styleable.RecordView_slide_to_cancel_bounds, -1)
//            if (cancelBounds != -1) setCancelBounds(
//                cancelBounds.toFloat(),
//                false
//            ) //don't convert it to pixels since it's already in pixels
//            if (slideArrowResource != -1) {
//                val slideArrow = AppCompatResources.getDrawable(contexts, slideArrowResource)
//                arrow!!.setImageDrawable(slideArrow)
//            }
//            if (slideToCancelText != null) {
//                slideToCancel!!.text = slideToCancelText
//            }
//            if (counterTimeColor != -1) setCounterTimeColor(counterTimeColor)
//            if (arrowColor != -1) setSlideToCancelArrowColor(arrowColor)
//            setMarginRight(slideMarginRight, true)
//            typedArray.recycle()
//        }
//        animationHelper = AnimationHelper(
//            contexts,
//            basketImg,
//            smallBlinkingMic,
//            isRecordButtonGrowingAnimationEnabled
//        )
//    }
//
//    private fun isTimeLimitValid(): Boolean {
//        return timeLimit > 0
//    }
//
//    private fun initTimeLimitHandler() {
//        handlers = Handler(Looper.getMainLooper())
//        runnable = Runnable {
//            if (recordListener != null && !isSwiped) recordListener!!.onFinish(
//                elapsedTime,
//                true,
//                recordPath
//            )
//            removeTimeLimitCallbacks()
//            animationHelper!!.setStartRecorded(false)
//            if (!isSwiped) playSound(RECORD_FINISHED)
//            recordButton?.let { resetRecord(it) }
//            isSwiped = true
//        }
//    }
//
//
//    private fun hideViews(hideSmallMic: Boolean) {
//        slideToCancelLayout!!.visibility = GONE
//        counterTime!!.visibility = GONE
//        if (hideSmallMic) smallBlinkingMic!!.visibility = GONE
//    }
//
//    private fun showViews() {
//        slideToCancelLayout!!.visibility = VISIBLE
//        smallBlinkingMic!!.visibility = VISIBLE
//        counterTime!!.visibility = VISIBLE
//    }
//
//
//    private fun isLessThanOneSecond(time: Long): Boolean {
//        return time <= 1000
//    }
//
//
//    private fun playSound(soundRes: Int) {
//        if (isSoundEnabled) {
//            if (soundRes == 0) return
//            try {
//                player = MediaPlayer()
//                val afd: AssetFileDescriptor = context!!.resources.openRawResourceFd(soundRes)
//                    ?: return
//                player!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
//                afd.close()
//                player!!.prepare()
//                player!!.start()
//                player!!.setOnCompletionListener { mp -> mp.release() }
//                player!!.isLooping = false
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//
//    protected fun onActionDown(recordBtn: RecordButton, motionEvent: MotionEvent?) {
//        val audio = EX.checkPermission(Manifest.permission.RECORD_AUDIO, contexts)
//        val readStorage =
//            EX.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, contexts)
//        val writeStorage =
//            EX.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, contexts)
//        if (!audio || !readStorage || !writeStorage) {
//            onPermission()
//            return
//        }
//        if (!isRecordPermissionGranted()) {
//            return
//        }
//        recordButton = recordBtn
//        if (recordListener != null) {
//            startRecord()
//            recordListener!!.onStart()
//        }
//        if (isTimeLimitValid()) {
//            removeTimeLimitCallbacks()
//            handlers.postDelayed(runnable!!, timeLimit)
//        }
//        animationHelper!!.setStartRecorded(true)
//        animationHelper!!.resetBasketAnimation()
//        animationHelper!!.resetSmallMic()
//        if (isRecordButtonGrowingAnimationEnabled) {
//            recordBtn.startScale()
//        }
//        if (shimmerEffectEnabled) {
//            slideToCancelLayout!!.startShimmerAnimation()
//        }
//        initialX = recordBtn.x
//        basketInitialY = basketImg!!.y + 90
//        playSound(RECORD_START)
//        showViews()
//        animationHelper!!.animateSmallMicAlpha()
//        counterTime!!.base = SystemClock.elapsedRealtime()
//        startTime = System.currentTimeMillis()
//        counterTime!!.start()
//        isSwiped = false
//    }
//
//
//    fun onPermission() {
//        val perms = arrayOf(
//            Manifest.permission.RECORD_AUDIO,
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//        )
//        ActivityCompat.requestPermissions(
//            activity!!, perms, 100
//        )
//    }
//
//    private fun startRecord() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val values = ContentValues(4)
//            values.put(MediaStore.Audio.Media.TITLE, fileName)
//            values.put(
//                MediaStore.Audio.Media.DATE_ADDED,
//                (System.currentTimeMillis() / 1000).toInt()
//            )
//            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/$type")
//            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Recordings/")
//
//            audiouri = context!!.contentResolver.insert(
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                values
//            )!!
//            file = context!!.contentResolver.openFileDescriptor(audiouri!!, "w")
//        } else {
//            recordPath =
//                context!!.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/${UUID.randomUUID()}.$type"
//        }
//
//
//        mediaRecorder = MediaRecorder()
//        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
//        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//        mediaRecorder!!.setAudioSamplingRate(16000)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
//            mediaRecorder?.setOutputFile(file!!.fileDescriptor)
//        else {
//            try {
//                mediaRecorder?.setOutputFile(recordPath)
//            } catch (e: Exception) {
//            }
//        }
//        mediaRecorder?.setAudioChannels(1)
//        try {
//            mediaRecorder!!.prepare()
//        } catch (e: IOException) {
//        }
//        mediaRecorder!!.start()
//    }
//
//    private fun stopRecording() {
//        mediaRecorder!!.stop()
//        mediaRecorder!!.reset()
//        mediaRecorder!!.release()
//    }
//
//    private fun getRecordDuration(): Long {
//        val retriever = MediaMetadataRetriever()
//        val uri = Uri.parse(recordPath)
//        retriever.setDataSource(context, uri)
//        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
//        return time!!.toLong()
//    }
//
//    protected fun onActionMove(recordBtn: RecordButton, motionEvent: MotionEvent) {
//        if (!canRecord) {
//            return
//        }
//        val time = System.currentTimeMillis() - startTime
//        if (!isSwiped) {
//
//            //Swipe To Cancel
//            if (slideToCancelLayout!!.x != 0f && slideToCancelLayout!!.x <= counterTime!!.right + cancelBounds) {
//
//                //if the time was less than one second then do not start basket animation
//                if (isLessThanOneSecond(time)) {
//                    hideViews(true)
//                    animationHelper!!.clearAlphaAnimation(false)
//                    animationHelper!!.onAnimationEnd()
//                } else {
//                    hideViews(false)
//                    animationHelper!!.animateBasket(basketInitialY)
//                }
//                animationHelper!!.moveRecordButtonAndSlideToCancelBack(
//                    recordBtn,
//                    slideToCancelLayout,
//                    initialX,
//                    difX
//                )
//                counterTime!!.stop()
//                if (shimmerEffectEnabled) {
//                    slideToCancelLayout!!.stopShimmerAnimation()
//                }
//                isSwiped = true
//                animationHelper!!.setStartRecorded(false)
//                if (recordListener != null) {
//                    stopRecording()
//                    recordListener!!.onCancel()
//                }
//                if (isTimeLimitValid()) {
//                    removeTimeLimitCallbacks()
//                }
//            } else {
//                //if statement is to Prevent Swiping out of bounds
//                if (motionEvent.rawX < initialX) {
//                    recordBtn.animate()
//                        .x(motionEvent.rawX)
//                        .setDuration(0)
//                        .start()
//                    if (difX == 0f) difX = initialX - slideToCancelLayout!!.x
//                    slideToCancelLayout!!.animate()
//                        .x(motionEvent.rawX - difX)
//                        .setDuration(0)
//                        .start()
//                }
//            }
//        }
//    }
//
//    protected fun onActionUp(recordBtn: RecordButton) {
//        val audio = EX.checkPermission(Manifest.permission.RECORD_AUDIO, getContext())
//        val readStorage =
//            EX.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getContext())
//        val writeStorage =
//            EX.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getContext())
//        if (!audio || !readStorage || !writeStorage) {
//            return
//        }
//        if (!canRecord) {
//            return
//        }
//        elapsedTime = System.currentTimeMillis() - startTime
//        if (!isLessThanSecondAllowed && isLessThanOneSecond(elapsedTime) && !isSwiped) {
//            recordListener?.onLessThanSecond()
//            removeTimeLimitCallbacks()
//            animationHelper!!.setStartRecorded(false)
//            playSound(RECORD_ERROR)
//        } else {
//            if (recordListener != null && !isSwiped) {
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    recordPath = getRealPathFromUri(context!!, audiouri)!!
//                }
//
//                recordListener!!.onFinish(
//                    elapsedTime,
//                    false,
//                    recordPath
//                )
//            }
//            removeTimeLimitCallbacks()
//            animationHelper!!.setStartRecorded(false)
//            if (!isSwiped) playSound(RECORD_FINISHED)
//        }
//        resetRecord(recordBtn)
//    }
//
//    private fun getRealPathFromUri(context: Context, contentUri: Uri?): String? {
//        var cursor: Cursor? = null
//        return try {
//            val proj = arrayOf(MediaStore.Images.Media.DATA)
//            cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
//            val columnIndex: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
//            cursor.moveToFirst()
//            cursor.getString(columnIndex)
//        } finally {
//            cursor?.close()
//        }
//    }
//
//    private fun resetRecord(recordBtn: RecordButton) {
//        //if user has swiped then do not hide SmallMic since it will be hidden after swipe Animation
//        hideViews(!isSwiped)
//        if (!isSwiped) animationHelper!!.clearAlphaAnimation(true)
//        animationHelper!!.moveRecordButtonAndSlideToCancelBack(
//            recordBtn,
//            slideToCancelLayout,
//            initialX,
//            difX
//        )
//        counterTime!!.stop()
//        if (shimmerEffectEnabled) {
//            slideToCancelLayout!!.stopShimmerAnimation()
//        }
//    }
//
//    private fun removeTimeLimitCallbacks() {
//        if (isTimeLimitValid()) {
//            handlers.removeCallbacks(runnable!!)
//        }
//    }
//
//    private fun isRecordPermissionGranted(): Boolean {
//        canRecord = recordPermissionHandler?.isPermissionGranted ?: true
//        return canRecord
//    }
//
//    private fun setMarginRight(marginRight: Int, convertToDp: Boolean) {
//        val layoutParams = slideToCancelLayout!!.layoutParams as LayoutParams
//        if (convertToDp) {
//            layoutParams.rightMargin = DpUtil.toPixel(marginRight.toFloat(), context).toInt()
//        } else layoutParams.rightMargin = marginRight
//        slideToCancelLayout!!.layoutParams = layoutParams
//    }
//
//    fun setOnRecordListener(activity: Activity?, recrodListener: OnRecordListener?) {
//        this.activity = activity
//        setRecordPermissionHandler(RecordPermissionHandler {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//                return@RecordPermissionHandler true
//            }
//            val recordPermissionAvailable = ContextCompat.checkSelfPermission(
//                activity!!, Manifest.permission.RECORD_AUDIO
//            ) == PermissionChecker.PERMISSION_GRANTED
//            if (recordPermissionAvailable) {
//                return@RecordPermissionHandler true
//            }
//            ActivityCompat.requestPermissions(
//                activity, arrayOf(Manifest.permission.RECORD_AUDIO),
//                0
//            )
//            false
//        })
//        recordListener = recrodListener
//    }
//
//    fun setRecordPermissionHandler(recordPermissionHandler: RecordPermissionHandler?) {
//        this.recordPermissionHandler = recordPermissionHandler
//    }
//
//    fun setOnBasketAnimationEndListener(onBasketAnimationEndListener: OnBasketAnimationEnd?) {
//        animationHelper!!.setOnBasketAnimationEndListener(onBasketAnimationEndListener)
//    }
//
//    fun setSoundEnabled(isEnabled: Boolean) {
//        isSoundEnabled = isEnabled
//    }
//
//    fun setLessThanSecondAllowed(isAllowed: Boolean) {
//        isLessThanSecondAllowed = isAllowed
//    }
//
//    fun setSlideToCancelText(text: String?) {
//        slideToCancel!!.text = text
//    }
//
//    fun setSlideToCancelTextColor(color: Int) {
//        slideToCancel!!.setTextColor(color)
//    }
//
//    fun setSmallMicColor(color: Int) {
//        smallBlinkingMic!!.setColorFilter(color)
//    }
//
//    fun setSmallMicIcon(icon: Int) {
//        smallBlinkingMic!!.setImageResource(icon)
//    }
//
//    fun setSlideMarginRight(marginRight: Int) {
//        setMarginRight(marginRight, true)
//    }
//
//    fun setCustomSounds(startSound: Int, finishedSound: Int, errorSound: Int) {
//        //0 means do not play sound
//        RECORD_START = startSound
//        RECORD_FINISHED = finishedSound
//        RECORD_ERROR = errorSound
//    }
//
//    fun getCancelBounds(): Float {
//        return cancelBounds
//    }
//
//    fun setCancelBounds(cancelBounds: Float) {
//        setCancelBounds(cancelBounds, true)
//    }
//
//    //set Chronometer color
//    fun setCounterTimeColor(color: Int) {
//        counterTime!!.setTextColor(color)
//    }
//
//    fun setSlideToCancelArrowColor(color: Int) {
//        arrow!!.setColorFilter(color)
//    }
//
//
//    private fun setCancelBounds(cancelBounds: Float, convertDpToPixel: Boolean) {
//        val bounds = if (convertDpToPixel) DpUtil.toPixel(cancelBounds, context) else cancelBounds
//        this.cancelBounds = bounds
//    }
//
//    fun isRecordButtonGrowingAnimationEnabled(): Boolean {
//        return isRecordButtonGrowingAnimationEnabled
//    }
//
//    fun setRecordButtonGrowingAnimationEnabled(recordButtonGrowingAnimationEnabled: Boolean) {
//        isRecordButtonGrowingAnimationEnabled = recordButtonGrowingAnimationEnabled
//        animationHelper!!.setRecordButtonGrowingAnimationEnabled(recordButtonGrowingAnimationEnabled)
//    }
//
//    fun isShimmerEffectEnabled(): Boolean {
//        return shimmerEffectEnabled
//    }
//
//    fun setShimmerEffectEnabled(shimmerEffectEnabled: Boolean) {
//        this.shimmerEffectEnabled = shimmerEffectEnabled
//    }
//
//    fun getTimeLimit(): Long {
//        return timeLimit
//    }
//
//    fun setTimeLimit(timeLimit: Long) {
//        this.timeLimit = timeLimit
//        if (runnable != null) {
//            removeTimeLimitCallbacks()
//        }
//        initTimeLimitHandler()
//    }
//
//    fun setTrashIconColor(color: Int) {
//        animationHelper!!.setTrashIconColor(color)
//    }
//
//}
