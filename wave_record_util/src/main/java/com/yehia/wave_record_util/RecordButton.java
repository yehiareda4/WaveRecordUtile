package com.yehia.wave_record_util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;

/**
 * Edit by Yehia Reda on 05/01/2022.
 */
public class RecordButton extends AppCompatImageView implements View.OnTouchListener, View.OnClickListener {

    private ScaleAnim scaleAnim;
    private RecordView recordView;
    private boolean listenForRecord = true;
    private OnRecordClickListener onRecordClickListener;
    private int imageResource;

    public void setRecordView(RecordView recordView) {
        this.recordView = recordView;
    }

    public RecordButton(Context context) {
        super(context);
        init(context, null);
    }

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RecordButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordButton);
            imageResource = typedArray.getResourceId(R.styleable.RecordButton_mic_icon, -1);

            if (imageResource != -1) {
                setTheImageResource(imageResource);
            }

            typedArray.recycle();
        }
        scaleAnim = new ScaleAnim(this);
        this.setOnTouchListener(this);
        this.setOnClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setClip(this);
    }

    public void setClip(View v) {
        if (v.getParent() == null) {
            return;
        }

        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }

        if (v.getParent() instanceof View) {
            setClip((View) v.getParent());
        }
    }

    public void setWithEditText(EditText etMessage, final int drawable) {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    setTheImageResource(drawable);
                } else {
                    if (imageResource != -1) {
                        setTheImageResource(imageResource);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void setTheImageResource(int imageResource) {
        Drawable image = AppCompatResources.getDrawable(getContext(), imageResource);
        setImageDrawable(image);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isListenForRecord()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    recordView.onActionDown((RecordButton) v, event);
                    break;

                case MotionEvent.ACTION_MOVE:
                    recordView.onActionMove((RecordButton) v, event);
                    break;

                case MotionEvent.ACTION_UP:
                    recordView.onActionUp((RecordButton) v);
                    break;
            }
        }
        return isListenForRecord();
    }

    protected void startScale() {
        scaleAnim.start();
    }

    protected void stopScale() {
        scaleAnim.stop();
    }

    public void setListenForRecord(boolean listenForRecord) {
        this.listenForRecord = listenForRecord;
    }

    public boolean isListenForRecord() {
        return listenForRecord;
    }

    public void setOnRecordClickListener(OnRecordClickListener onRecordClickListener) {
        this.onRecordClickListener = onRecordClickListener;
    }

    @Override
    public void onClick(View v) {

        boolean audio = EX.checkPermission(Manifest.permission.RECORD_AUDIO, getContext());
        boolean readStorage = EX.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getContext());
        boolean writeStorage = EX.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getContext());
        if (!audio || (Build.VERSION.SDK_INT < 33 && (!readStorage || !writeStorage))) {
            onPermission();
        } else {
            if (onRecordClickListener != null) onRecordClickListener.onClick(v);
        }
    }

    public void onPermission() {

        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions((Activity) getContext(), perms, 100);
    }
}
