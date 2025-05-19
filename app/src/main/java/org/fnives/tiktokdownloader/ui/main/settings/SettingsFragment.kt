package org.fnives.tiktokdownloader.ui.main.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.fnives.tiktokdownloader.errortracking.ErrorTracer
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.di.provideViewModels
import org.fnives.tiktokdownloader.errortracking.SendErrorsAsEmail

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel by provideViewModels<SettingsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val alwaysOpenAppHolder = view.findViewById<View>(R.id.always_open_app_holder)
        val alwaysOpenAppSwitch = view.findViewById<SwitchCompat>(R.id.always_open_app)
        viewModel.userPreferences.observe(viewLifecycleOwner) {
            alwaysOpenAppSwitch.isChecked = it.alwaysOpenApp
        }
        alwaysOpenAppSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAlwaysOpenApp(isChecked)
        }
        alwaysOpenAppHolder.setOnClickListener {
            viewModel.setAlwaysOpenApp(!alwaysOpenAppSwitch.isChecked)
        }

        viewLifecycleOwner.lifecycle.addObserver(ErrorObserver())
        view.findViewById<View>(R.id.report_error_cta).setOnClickListener {
            SendErrorsAsEmail.send(it.context)
        }
    }


    inner class ErrorObserver : LifecycleEventObserver {
        private var subscription: (() -> Unit)? = null

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    val errorCTA = view?.findViewById<View>(R.id.report_error_cta)
                    subscription = ErrorTracer.subscribeToHasErrorChanges {
                        errorCTA?.isVisible = ErrorTracer.hasErrors
                    }
                    errorCTA?.isVisible = ErrorTracer.hasErrors
                }

                Lifecycle.Event.ON_STOP -> {
                    subscription?.invoke()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    source.lifecycle.removeObserver(this)
                }

                else -> Unit
            }
        }
    }

    companion object {

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}