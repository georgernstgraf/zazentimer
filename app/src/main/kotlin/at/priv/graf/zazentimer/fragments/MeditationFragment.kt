package at.priv.graf.zazentimer.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.database.DbOperations
import at.priv.graf.zazentimer.databinding.FragmentMeditationBinding
import at.priv.graf.zazentimer.service.MeditationService
import at.priv.graf.zazentimer.service.MeditationUiState
import at.priv.graf.zazentimer.service.MeditationViewModel
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeditationFragment : Fragment() {
    private var _binding: FragmentMeditationBinding? = null
    private val binding get() = _binding!!

    private var viewModel: MeditationViewModel? = null
    private var meditationRunning: Boolean = false
    private var backPressedCallback: OnBackPressedCallback? = null

    @Inject
    lateinit var dbOperations: DbOperations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?,
    ): View {
        _binding = FragmentMeditationBinding.inflate(layoutInflater, viewGroup, false)

        binding.butStop.setOnClickListener {
            Log.d(TAG, "Stop button clicked")
            this@MeditationFragment.showStopConfirmationDialog()
        }

        binding.butPause.setOnClickListener {
            if (this@MeditationFragment.meditationRunning) {
                this@MeditationFragment.viewModel?.pauseMeditation()
                this@MeditationFragment.updateButtons()
            } else {
                this@MeditationFragment.startMeditationFromIdle()
            }
        }

        if (!MeditationService.isServiceRunning()) {
            showIdleState()
        } else {
            showRunningState()
            updateButtons()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        requireActivity().invalidateOptionsMenu()
        if (!MeditationService.isServiceRunning()) {
            showIdleState()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MeditationViewModel::class.java)

        backPressedCallback =
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    showStopConfirmationDialog()
                }
            }
        backPressedCallback?.let { callback ->
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        }

        viewModel?.getMeditationState()?.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MeditationUiState.Idle -> {
                    meditationRunning = false
                    backPressedCallback?.isEnabled = false
                    _binding?.let { b ->
                        b.timerView.setCurrentStartSeconds(state.currentStartSeconds)
                        b.timerView.setNumTotalSeconds(state.totalSessionTime)
                        b.timerView.setNextEndSeconds(state.nextEndSeconds)
                        b.timerView.setNextStartSeconds(state.nextStartSeconds)
                        b.timerView.setPrevStartSeconds(state.prevStartSeconds)
                        b.timerView.setSectionElapsedSeconds(state.sectionElapsedSeconds)
                        b.timerView.setSessionElapsedSeconds(state.sessionElapsedSeconds)
                        b.timerView.setSectionNamesNoAnim(state.currentSectionName, state.nextSectionName)
                        b.sessionNameText.text = state.sessionName
                    }
                    showIdleState()
                }
                is MeditationUiState.Running -> {
                    meditationRunning = true
                    backPressedCallback?.isEnabled = true
                    _binding?.let { b ->
                        b.timerView.setCurrentStartSeconds(state.currentStartSeconds)
                        b.timerView.setNumTotalSeconds(state.totalSessionTime)
                        b.timerView.setNextEndSeconds(state.nextEndSeconds)
                        b.timerView.setNextStartSeconds(state.nextStartSeconds)
                        b.timerView.setPrevStartSeconds(state.prevStartSeconds)
                        b.timerView.setSectionElapsedSeconds(state.sectionElapsedSeconds)
                        b.timerView.setSessionElapsedSeconds(state.sessionElapsedSeconds)
                        b.timerView.setSectionNames(state.currentSectionName, state.nextSectionName, state.nextNextSectionName)
                        b.sessionNameText.text = state.sessionName
                    }
                    showRunningState()
                    updateButtons()
                }
                is MeditationUiState.Paused -> {
                    meditationRunning = true
                    backPressedCallback?.isEnabled = true
                    _binding?.let { b ->
                        b.timerView.setCurrentStartSeconds(state.currentStartSeconds)
                        b.timerView.setNumTotalSeconds(state.totalSessionTime)
                        b.timerView.setNextEndSeconds(state.nextEndSeconds)
                        b.timerView.setNextStartSeconds(state.nextStartSeconds)
                        b.timerView.setPrevStartSeconds(state.prevStartSeconds)
                        b.timerView.setSectionElapsedSeconds(state.sectionElapsedSeconds)
                        b.timerView.setSessionElapsedSeconds(state.sessionElapsedSeconds)
                        b.timerView.setSectionNamesNoAnim(state.currentSectionName, state.nextSectionName)
                        b.sessionNameText.text = state.sessionName
                    }
                    showRunningState()
                    updateButtons()
                }
            }
        }
    }

    private fun showIdleState() {
        this.meditationRunning = false
        _binding?.let { b ->
            b.butStop.visibility = View.VISIBLE
            b.butStop.isEnabled = false
            b.butStop.alpha = 0.4f
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow_white_48dp, requireActivity().theme),
            )
        }
    }

    private fun showRunningState() {
        _binding?.let { b ->
            b.butStop.visibility = View.VISIBLE
            b.butStop.isEnabled = true
            b.butStop.alpha = 1.0f
        }
    }

    private fun startMeditationFromIdle() {
        val prefs = ZazenTimerActivity.getPreferences(requireContext())
        val sessionId = prefs.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1)
        if (sessionId == -1) {
            return
        }
        lifecycleScope.launch {
            if (dbOperations.readSections(sessionId).isEmpty()) {
                return@launch
            }
            viewModel?.setSelectedSessionId(sessionId)
            val activity = activity as? ZazenTimerActivity
            activity?.startMeditation()
        }
    }

    private fun showStopConfirmationDialog() {
        try {
            AlertDialog
                .Builder(requireContext())
                .setTitle(R.string.stop_meditation_title)
                .setMessage(R.string.stop_meditation_message)
                .setPositiveButton(R.string.stop_meditation_stop) { _, _ ->
                    viewModel?.stopMeditation()
                    backPressedCallback?.isEnabled = false
                    if (isAdded) {
                        Navigation.findNavController(requireView()).popBackStack()
                    }
                }.setNegativeButton(R.string.stop_meditation_cancel) { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show stop confirmation dialog", e)
        }
    }

    fun showStopDialogForTest() {
        showStopConfirmationDialog()
    }

    fun updateButtons() {
        val b = _binding ?: return
        if (!isAdded) return
        if (!meditationRunning) {
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow_white_48dp, requireContext().theme),
            )
            return
        }
        if (viewModel?.isPaused() == true) {
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow_white_48dp, requireContext().theme),
            )
        } else {
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_white_48dp, requireContext().theme),
            )
        }
    }

    companion object {
        private const val TAG = "ZMT_MeditationFragment"
    }
}
