package com.ambrxsh.buzzbuddy.utils

import com.google.android.material.textfield.TextInputLayout

/** Keep the password eye; Material otherwise replaces it with an error icon. */
fun TextInputLayout.setErrorKeepEndIcon(message: CharSequence?) {
    errorIconDrawable = null
    error = message
    errorIconDrawable = null
    if (endIconMode == TextInputLayout.END_ICON_PASSWORD_TOGGLE) {
        isEndIconVisible = true
    }
}
