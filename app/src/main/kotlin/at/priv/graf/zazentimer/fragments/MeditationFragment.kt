package at.priv.graf.zazentimer.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import javax.inject.Inject

@AndroidEntryPoint
class MeditationFragment : Fragment() {

    private var _binding: FragmentMeditationBinding? = null
    private val binding get() = _binding!!

    private var contextRef: Context? = null
    private var mAttached: Boolean = false
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

    override fun onCreateView(layoutInflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View {
        _binding = FragmentMeditationBinding.inflate(layoutInflater, viewGroup, false)

        binding.butStop.setOnClickListener {
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach (Context)")
        this.contextRef = context
        this.mAttached = true
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach")
        super.onDetach()
        this.mAttached = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MeditationViewModel::class.java)

        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                showStopConfirmationDialog()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)

        viewModel!!.getMeditationState().observe(viewLifecycleOwner) { state ->
            if (state == null || !state.running) {
                meditationRunning = false
                backPressedCallback?.isEnabled = false
                if (state != null) {
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
                }
                showIdleState()
                return@observe
            }
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
    }

    private fun showIdleState() {
        this.meditationRunning = false
        _binding?.let { b ->
            b.butStop.visibility = View.VISIBLE
            b.butStop.isEnabled = false
            b.butStop.alpha = 0.4f
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow_white_48dp, requireActivity().theme)
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
        if (dbOperations.readSections(sessionId).isEmpty()) {
            return
        }
        viewModel?.setSelectedSessionId(sessionId)
        val activity = activity as? ZazenTimerActivity
        activity?.startMeditation()
    }

    private fun showStopConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.stop_meditation_title)
            .setMessage(R.string.stop_meditation_message)
            .setPositiveButton(R.string.stop_meditation_stop) { _, _ ->
                viewModel?.stopMeditation()
                backPressedCallback?.isEnabled = false
                Navigation.findNavController(requireView()).popBackStack()
            }
            .setNegativeButton(R.string.stop_meditation_cancel) { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }

    fun updateButtons() {
        val b = _binding ?: return
        if (activity == null) return
        if (!meditationRunning) {
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow_white_48dp, requireActivity().theme)
            )
            return
        }
        if (viewModel != null && viewModel!!.isPaused()) {
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow_white_48dp, requireActivity().theme)
            )
        } else {
            b.butPause.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_white_48dp, requireActivity().theme)
            )
        }
    }

    companion object {
        private const val TAG = "ZMT_MeditationFragment"
    }
}
