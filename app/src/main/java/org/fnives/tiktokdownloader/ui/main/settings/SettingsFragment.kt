package org.fnives.tiktokdownloader.ui.main.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import org.fnives.tiktokdownloader.R
import org.fnives.tiktokdownloader.di.provideViewModels

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
    }

    companion object {

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}