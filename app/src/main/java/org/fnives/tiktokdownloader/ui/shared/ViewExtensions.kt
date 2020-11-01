package org.fnives.tiktokdownloader.ui.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.core.net.toUri
import com.bumptech.glide.Glide

val View.inflater get() = LayoutInflater.from(context)

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, addToParent: Boolean = false) : View =
    inflater.inflate(layoutRes, this, addToParent)

fun ImageView.loadUri(uri: String) {
    Glide.with(this)
        .load(uri.toUri())
        .into(this)
}