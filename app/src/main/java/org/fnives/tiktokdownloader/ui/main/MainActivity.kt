package org.fnives.tiktokdownloader.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.di.ServiceLocator
import org.fnives.tiktokdownloader.di.provideViewModels
import org.fnives.tiktokdownloader.ui.main.help.HelpFragment
import org.fnives.tiktokdownloader.ui.main.queue.QueueFragment
import org.fnives.tiktokdownloader.ui.permission.PermissionRequester
import org.fnives.tiktokdownloader.ui.service.QueueService

class MainActivity : AppCompatActivity() {

    private val viewModel by provideViewModels<MainViewModel>()
    private val permissionRequester: PermissionRequester by lazy {
        ServiceLocator.permissionModule.permissionRequester
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequester(this@MainActivity)
        stopService(QueueService.buildIntent(this))
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val downloadFab = findViewById<FloatingActionButton>(R.id.download_fab)
        val snackBarAnchor = findViewById<CoordinatorLayout>(R.id.snack_bar_anchor)

        setupBottomNavigationView(bottomNavigationView, savedInstanceState)
        downloadFab.setOnClickListener {
            animateFabClicked(downloadFab)
            viewModel.onFetchDownloadClicked()
        }
        viewModel.refreshActionVisibility.observe(this, {
            animateFabVisibility(downloadFab, it == true)
        })
        viewModel.errorMessage.observe(this, {
            val stringRes = it?.item?.stringRes ?: return@observe
            Snackbar.make(snackBarAnchor, stringRes, Snackbar.LENGTH_SHORT).show()
        })
    }

    private fun setupBottomNavigationView(bottomNavigationView: BottomNavigationView, savedInstanceState: Bundle?) {
        bottomNavigationView.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.help_menu_item -> HelpFragment.newInstance()
                R.id.queue_menu_item -> QueueFragment.newInstance()
                else -> return@OnNavigationItemSelectedListener false
            }
            item.toScreen()?.let(viewModel::onScreenSelected)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
            return@OnNavigationItemSelectedListener true
        })
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.queue_menu_item
        }
    }

    companion object {

        private val fabRotationInterpolator = OvershootInterpolator()
        private val fabVisibilityInterpolator = AccelerateDecelerateInterpolator()
        private const val FAB_ROTATION_ANIMATION_DURATION = 800L
        private const val FAB_VISIBILITY_ANIMATION_DURATION = 500L
        private const val FULL_ROTATION = 360f
        const val INTENT_EXTRA_URL = "INTENT_EXTRA_URL"

        fun buildIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)

        fun buildIntent(context: Context, url: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(INTENT_EXTRA_URL, url)

        private fun MenuItem.toScreen(): MainViewModel.Screen? =
            when (itemId) {
                R.id.help_menu_item -> MainViewModel.Screen.HELP
                R.id.queue_menu_item -> MainViewModel.Screen.QUEUE
                else -> null
            }

        @get:StringRes
        private val MainViewModel.ErrorMessage.stringRes: Int
            get() = when (this) {
                MainViewModel.ErrorMessage.NETWORK -> R.string.network_error
                MainViewModel.ErrorMessage.PARSING -> R.string.parsing_error
                MainViewModel.ErrorMessage.STORAGE -> R.string.storage_error
                MainViewModel.ErrorMessage.CAPTCHA -> R.string.captcha_error
                MainViewModel.ErrorMessage.UNKNOWN -> R.string.unexpected_error
            }

        private fun animateFabClicked(downloadFab: FloatingActionButton) {
            downloadFab.clearAnimation()
            downloadFab.animate()
                .rotationBy(FULL_ROTATION)
                .setDuration(FAB_ROTATION_ANIMATION_DURATION)
                .setInterpolator(fabRotationInterpolator)
                .start()
        }

        private fun animateFabVisibility(downloadFab: FloatingActionButton, visible: Boolean) {
            val scale = if (visible) 1f else 0f
            val translation = if (visible) 0f else downloadFab.height * 2 / 3f
            downloadFab.clearAnimation()
            downloadFab.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(FAB_VISIBILITY_ANIMATION_DURATION)
                .setInterpolator(fabVisibilityInterpolator)
                .translationY(translation)
                .start()
        }
    }
}