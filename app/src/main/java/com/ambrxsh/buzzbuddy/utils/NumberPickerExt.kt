package com.ambrxsh.buzzbuddy.utils

import android.graphics.Paint
import android.os.Build
import android.widget.EditText
import android.widget.NumberPicker
import timber.log.Timber

fun NumberPicker.setTwoDigitRange(min: Int, max: Int) {
    displayedValues = null
    minValue = min
    maxValue = max
    displayedValues = (min..max).map { value -> "%02d".format(value) }.toTypedArray()
}

fun NumberPicker.setPickerTextColor(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        setTextColor(color)
        return
    }
    try {
        val selectorWheelPaintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
        selectorWheelPaintField.isAccessible = true
        val paint = selectorWheelPaintField.get(this) as Paint
        paint.color = color

        val inputTextField = NumberPicker::class.java.getDeclaredField("mInputText")
        inputTextField.isAccessible = true
        val inputText = inputTextField.get(this) as EditText
        inputText.setTextColor(color)
        invalidate()
    } catch (e: Exception) {
        Timber.w(e, "Could not set NumberPicker text color on this API")
    }
}
