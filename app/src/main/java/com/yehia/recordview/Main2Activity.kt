package com.yehia.recordview

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.yehia.record_view.OnRecordListener
import com.yehia.record_view.RecordButton
import com.yehia.record_view.RecordView

class Main2Activity : AppCompatActivity() {

    var recyclerView: RecyclerView? = null
    private var btnChangeOnclick: Button? = null
    private var recordView: RecordView? = null
    private var recordButton: RecordButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //        recyclerView = findViewById(R.id.list_item);
        recordButton = findViewById(R.id.record_button)
        recordView = findViewById(R.id.record_view)
        btnChangeOnclick = findViewById(R.id.btn_change_onclick)


        recordButton?.setRecordView(recordView)

        recordView?.apply {
            cancelBounds = 8f
            setLessThanSecondAllowed(false)
            setOnRecordListener(this@Main2Activity, object : OnRecordListener {
                override fun onFinish(recordTime: Long, limitReached: Boolean, file: String?) {
//                    layoutMessage.toVisible()

                    file?.takeIf { it.isNotEmpty() }?.let {
                        Log.d(TAG, "onFinish: $it")
                    }
                }

                override fun onLessThanSecond() {
                    Log.d(TAG, "onLessThanSecond: ")
//                    layoutMessage.toVisible()
                }
            })
        }
    }

}
