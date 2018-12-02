package com.appsci.sleep.sleeptimepicker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val sdf = SimpleDateFormat("h:mm aa", Locale.US)
    private val hourMs = TimeUnit.HOURS.toMillis(1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timePicker.listener = { bedTime: Date, wakeTime: Date ->
            Timber.d("time changed \nbedtime= $bedTime\nwaketime=$wakeTime")
            handleUpdate(bedTime, wakeTime)
        }
        handleUpdate(timePicker.getBedTime(), timePicker.getWakeTime())
    }

    private fun handleUpdate(bedTime: Date, wakeTime: Date) {
        tvBedTime.text = sdf.format(bedTime)
        tvWakeTime.text = sdf.format(wakeTime)

        val duration = if (wakeTime > bedTime) {
            wakeTime.time - bedTime.time
        } else {
            wakeTime.time - bedTime.time + TimeUnit.HOURS.toMillis(24)
        }
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration % hourMs)
        tvHours.text = hours.toString()
        tvMins.text = minutes.toString()
        if (minutes > 0) llMins.visibility = View.VISIBLE else llMins.visibility = View.GONE
    }
}
