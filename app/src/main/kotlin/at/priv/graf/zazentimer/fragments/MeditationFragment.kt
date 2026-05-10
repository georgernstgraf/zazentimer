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
import at.priv.graf.zazentimer.views.TimerView
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

    private val buttonStateUpdater by lazy { ButtonStateUpdater() }

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
                this@MeditationFragment.buttonStateUpdater.update()
            } else {
                this@MeditationFragment.startMeditationFromIdle()
            }
        }

        if (!MeditationService.isServiceRunning()) {
            showIdleState()
        } else {
            showRunningState()
            buttonStateUpdater.update()
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
        observeMeditationState()
    }

    private fun observeMeditationState() {
        viewModel?.getMeditationState()?.observe(viewLifecycleOwner) { state ->
            meditationRunning = state !is MeditationUiState.Idle
            backPressedCallback?.isEnabled = state !is MeditationUiState.Idle
            _binding?.let { b ->
                b.timerView.animateProperty(TimerView.PROP_CURRENT_START_SECONDS, state.currentStartSeconds)
                b.timerView.animateProperty(TimerView.PROP_SESSION_TOTAL_SECONDS, state.totalSessionTime)
                b.timerView.animateProperty(TimerView.PROP_NEXT_END_SECONDS, state.nextEndSeconds)
                b.timerView.animateProperty(TimerView.PROP_NEXT_START_SECONDS, state.nextStartSeconds)
                b.timerView.animateProperty(TimerView.PROP_PREV_START_SECONDS, state.prevStartSeconds)
                b.timerView.setSectionElapsedSeconds(state.sectionElapsedSeconds)
                b.timerView.animateProperty(TimerView.PROP_SESSION_ELAPSED_SECONDS, state.sessionElapsedSeconds)
                b.sessionNameText.text = state.sessionName
                when (state) {
                    is MeditationUiState.Running ->
                        b.timerView.setSectionNames(
                            state.currentSectionName,
                            state.nextSectionName,
                        )
                    else ->
                        b.timerView.setSectionNamesNoAnim(
                            state.currentSectionName,
                            state.nextSectionName,
                        )
                }
            }
            when (state) {
                is MeditationUiState.Idle -> showIdleState()
                is MeditationUiState.Running -> {
                    showRunningState()
                    buttonStateUpdater.update()
                }
                is MeditationUiState.Paused -> {
                    showRunningState()
                    buttonStateUpdater.update()
                }
            }
        }
    }

    private fun showIdleState() {
        this.meditationRunning = false
        _binding?.let { b ->
            b.butStop.visibility = View.VISIBLE
            b.butStop.isEnabled = false
            b.butStop.alpha = DISABLED_ALPHA
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
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to show stop confirmation dialog", e)
        }
    }

    private inner class ButtonStateUpdater {
        fun update() {
            val b = _binding ?: return
            if (!isAdded) return
            val drawableId =
                if (!meditationRunning || viewModel?.isPaused() == true) {
                    R.drawable.ic_play_arrow_white_48dp
                } else {
                    R.drawable.ic_pause_white_48dp
                }
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, drawableId, requireContext().theme),
            )
        }
    }

    companion object {
        private const val TAG = "ZMT_MeditationFragment"
        private const val DISABLED_ALPHA = 0.4f
    }
}
