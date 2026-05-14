package at.priv.graf.zazentimer.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.base.SpinnerUtil
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.database.DbOperations
import at.priv.graf.zazentimer.databinding.FragmentMainBinding
import at.priv.graf.zazentimer.service.MeditationService
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private var mListener: OnFragmentInteractionListener? = null
    private var pref: SharedPreferences? = null
    private var sessionListAdapter: SessionListAdapter? = null
    private var sessions: ArrayList<Session> = ArrayList()
    private var selectedSessionId: Int = -1
    private var interactionsEnabled: Boolean = true

    @Inject
    lateinit var dbOperations: DbOperations

    interface OnFragmentInteractionListener {
        fun onStartPressed()
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        enterTransition = MaterialFadeThrough()
        Log.d(TAG, "onCreate")
    }

    private val globalLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener {
            _binding?.let { b ->
                val parent = b.recyclerSessions.parent as? View ?: return@let
                val available = parent.height - b.butStart.height
                if (available <= 0) return@let
                val maxRecyclerHeight = (available * MAX_RECYCLER_HEIGHT_RATIO).toInt()
                val rv = b.recyclerSessions as MaxHeightRecyclerView
                if (rv.getMaxHeight() != maxRecyclerHeight) {
                    rv.setMaxHeight(maxRecyclerHeight)
                }
            }
        }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentMainBinding.inflate(layoutInflater, viewGroup, false)

        binding.butStart.setOnClickListener {
            this@MainFragment.mListener?.onStartPressed()
        }

        (binding.recyclerSessions as RecyclerView).layoutManager = LinearLayoutManager(requireContext())

        val menuHandler = sessionListMenuHandler
        this.sessionListAdapter =
            SessionListAdapter(
                object : SessionListAdapter.OnItemClickListener {
                    override fun onItemClick(
                        position: Int,
                        session: SessionWithTimeInfo,
                    ) {
                        if (!interactionsEnabled) return
                        val s = sessions[position]
                        this@MainFragment.selectedSessionId = s.id
                        this@MainFragment
                            .pref
                            ?.edit()
                            ?.putInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, s.id)
                            ?.apply()
                    }
                },
                object : SessionListAdapter.OnSessionActionListener {
                    override fun onEditSession(position: Int) {
                        menuHandler.onCardEditSession(position)
                    }

                    override fun onCopySession(position: Int) {
                        menuHandler.onCardCopySession(position)
                    }

                    override fun onDeleteSession(position: Int) {
                        menuHandler.onCardDeleteSession(position)
                    }
                },
            )
        (binding.recyclerSessions as RecyclerView).adapter = this.sessionListAdapter

        binding.recyclerSessions.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        ItemTouchHelper(
            SessionTouchHelperCallback(
                object : SessionTouchHelperCallback.SessionTouchListener {
                    override fun onMove(
                        fromPosition: Int,
                        toPosition: Int,
                    ): Boolean {
                        val validFrom = fromPosition in sessions.indices
                        val validTo = toPosition in sessions.indices
                        if (!validFrom || !validTo) return false
                        val moved = sessions.removeAt(fromPosition)
                        sessions.add(toPosition, moved)
                        sessionListAdapter?.moveItem(fromPosition, toPosition)
                        return true
                    }
                },
            ),
        ).attachToRecyclerView(binding.recyclerSessions as RecyclerView)

        return binding.root
    }

    override fun onDestroyView() {
        binding.recyclerSessions.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        super.onDestroyView()
        _binding = null
    }

    fun onFabNewSessionClicked() {
        sessionListMenuHandler.addNewSession()
    }

    private val sessionListMenuHandler by lazy { SessionListMenuHandler() }

    private inner class SessionListMenuHandler {
        fun addNewSession() {
            if (!interactionsEnabled) return
            val session = Session()
            session.name = ""
            session.description = ""
            lifecycleScope.launch {
                dbOperations.insertSession(session)
                updateSessionList()
                setSelectedSessionId(session.id)
                navigateToSessionEdit(session.id)
            }
        }

        fun onCardEditSession(position: Int) {
            if (!interactionsEnabled) return
            if (position !in sessions.indices) return
            navigateToSessionEdit(sessions[position].id)
        }

        fun onCardCopySession(position: Int) {
            if (!interactionsEnabled) return
            if (position !in sessions.indices) return
            val s = sessions[position]
            lifecycleScope.launch {
                val newId =
                    dbOperations.duplicateSession(
                        s.id,
                        "${getString(R.string.copy_prefix)} ${s.name}",
                    )
                if (!isAdded) return@launch
                updateSessionList()
                setSelectedSessionId(newId)
            }
        }

        fun onCardDeleteSession(position: Int) {
            if (!interactionsEnabled) return
            if (position !in sessions.indices) return
            val s = sessions[position]
            AlertDialog
                .Builder(requireContext())
                .setTitle(R.string.title_question_delete_session)
                .setMessage(R.string.text_question_delete_session)
                .setPositiveButton(R.string.ok) { _, _ ->
                    lifecycleScope.launch {
                        dbOperations.deleteSession(s.id)
                        if (!isAdded) return@launch
                        updateSessionList()
                        selectLastSession()
                    }
                }.setNegativeButton(R.string.abbrechen) { _, _ ->
                }.create()
                .show()
        }

        fun navigateToSessionEdit(sessionId: Int) {
            val args = Bundle()
            args.putInt("sessionId", sessionId)
            Navigation
                .findNavController(requireView())
                .navigate(R.id.action_mainFragment_to_sessionEditFragment, args)
        }

        fun setSelectedSessionId(i: Int) {
            selectedSessionId = i
            sessionListAdapter?.setSelectedPosition(SpinnerUtil.getPositionById(sessions, i))
        }

        fun selectLastSession() {
            if (sessions.isEmpty()) {
                setSelectedSessionId(-1)
            } else {
                setSelectedSessionId(sessions[sessions.size - 1].id)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach (Context)")
        this.pref = ZazenTimerActivity.getPreferences(context)
        if (context is OnFragmentInteractionListener) {
            this.mListener = context
            return
        }
        throw IllegalStateException(context.toString() + " must implement OnFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach")
        this.mListener = null
    }

    override fun onResume() {
        super.onResume()
        interactionsEnabled = !MeditationService.isServiceRunning()
        _binding?.let { b ->
            b.butStart.isEnabled = interactionsEnabled
            b.butStart.alpha = if (interactionsEnabled) ALPHA_ENABLED else ALPHA_DISABLED
        }
        sessionListAdapter?.setInteractionsEnabled(interactionsEnabled)
        Log.d(TAG, "onResume")
        requireActivity().invalidateOptionsMenu()
        val lastId = this.pref?.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1) ?: -1
        updateSessionList(lastId)
    }

    fun updateSessionList(restoreSelectionId: Int = -1) {
        lifecycleScope.launch {
            suspendUpdateSessionList(restoreSelectionId)
        }
    }

    suspend fun suspendUpdateSessionList(restoreSelectionId: Int = -1) {
        val readSessions = dbOperations.readSessions()
        if (!isAdded) return
        val arrayList = ArrayList<SessionWithTimeInfo>()
        this@MainFragment.sessions.clear()
        for (session in readSessions) {
            var total = 0
            for (section in dbOperations.readSections(session.id)) {
                total += section.duration
            }
            arrayList.add(SessionWithTimeInfo(session, total))
            this@MainFragment.sessions.add(session)
        }
        this@MainFragment.sessionListAdapter?.setSessions(arrayList)
        if (restoreSelectionId != -1) {
            val positionById = SpinnerUtil.getPositionById(this@MainFragment.sessions, restoreSelectionId)
            if (positionById != -1) {
                Log.d(TAG, "LAST_SELECTED_SESSION was idx=$positionById id=$restoreSelectionId")
                this@MainFragment.selectedSessionId = restoreSelectionId
                this@MainFragment.sessionListAdapter?.setSelectedPosition(positionById)
            }
        }
    }

    fun getSelectedSessionId(): Int = this.selectedSessionId

    companion object {
        private const val TAG = "ZMT_MainFragment"
        private const val MAX_RECYCLER_HEIGHT_RATIO = 0.60
        private const val ALPHA_ENABLED = 1.0f
        private const val ALPHA_DISABLED = 0.4f
    }
}
