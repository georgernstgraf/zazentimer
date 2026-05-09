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

    private var contextRef: Context? = null
    private var mAttached: Boolean = false
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

    init {
        Log.d(TAG, "Constructor")
        this.mAttached = false
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        enterTransition = MaterialFadeThrough()
        Log.d(TAG, "onCreate")
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

        (binding.recyclerSessions as RecyclerView).layoutManager = LinearLayoutManager(this.contextRef)

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
                        this@MainFragment.onCardEditSession(position)
                    }

                    override fun onCopySession(position: Int) {
                        this@MainFragment.onCardCopySession(position)
                    }

                    override fun onDeleteSession(position: Int) {
                        this@MainFragment.onCardDeleteSession(position)
                    }
                },
            )
        (binding.recyclerSessions as RecyclerView).adapter = this.sessionListAdapter

        binding.recyclerSessions.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (_binding == null) return
                    val parent = binding.recyclerSessions.parent as? View ?: return
                    val available = parent.height - binding.butStart.height
                    if (available <= 0) return
                    val maxRecyclerHeight = (available * 0.60).toInt()
                    val rv = binding.recyclerSessions as MaxHeightRecyclerView
                    if (rv.getMaxHeight() != maxRecyclerHeight) {
                        rv.setMaxHeight(maxRecyclerHeight)
                    }
                }
            },
        )

        val callback =
            SessionTouchHelperCallback(
                object : SessionTouchHelperCallback.SessionTouchListener {
                    override fun onMove(
                        fromPosition: Int,
                        toPosition: Int,
                    ): Boolean {
                        if (fromPosition < 0 || fromPosition >= sessions.size) return false
                        if (toPosition < 0 || toPosition >= sessions.size) return false
                        val moved = sessions.removeAt(fromPosition)
                        sessions.add(toPosition, moved)
                        sessionListAdapter?.moveItem(fromPosition, toPosition)
                        return true
                    }
                },
            )
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerSessions as RecyclerView)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onFabNewSessionClicked() {
        addNewSession()
    }

    private fun addNewSession() {
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

    private fun onCardEditSession(position: Int) {
        if (!interactionsEnabled) return
        if (position < 0 || position >= sessions.size) return
        val s = sessions[position]
        navigateToSessionEdit(s.id)
    }

    private fun onCardCopySession(position: Int) {
        if (!interactionsEnabled) return
        if (position < 0 || position >= sessions.size) return
        val s = sessions[position]
        lifecycleScope.launch {
            val newId =
                dbOperations.duplicateSession(
                    s.id,
                    "${getString(R.string.copy_prefix)} ${s.name}",
                )
            updateSessionList()
            setSelectedSessionId(newId)
        }
    }

    private fun onCardDeleteSession(position: Int) {
        if (!interactionsEnabled) return
        if (position < 0 || position >= sessions.size) return
        val s = sessions[position]
        AlertDialog
            .Builder(requireContext())
            .setTitle(R.string.title_question_delete_session)
            .setMessage(R.string.text_question_delete_session)
            .setPositiveButton(R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    dbOperations.deleteSession(s.id)
                    updateSessionList()
                    selectLastSession()
                }
            }.setNegativeButton(R.string.abbrechen) { _, _ ->
            }.create()
            .show()
    }

    private fun navigateToSessionEdit(sessionId: Int) {
        val args = Bundle()
        args.putInt("sessionId", sessionId)
        Navigation.findNavController(requireView()).navigate(R.id.action_mainFragment_to_sessionEditFragment, args)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach (Context)")
        handleAttach(context)
    }

    private fun handleAttach(context: Context?) {
        if (context == null) {
            return
        }
        this.mAttached = true
        this.pref = ZazenTimerActivity.getPreferences(context)
        this.contextRef = context
        if (context is OnFragmentInteractionListener) {
            this.mListener = context
            return
        }
        throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach")
        this.mListener = null
        this.mAttached = false
    }

    override fun onResume() {
        super.onResume()
        interactionsEnabled = !MeditationService.isServiceRunning()
        updateSessionInteractions()
        Log.d(TAG, "onResume")
        requireActivity().invalidateOptionsMenu()
        updateSessionList()
        val i = this.pref?.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1) ?: -1
        if (i == -1) {
            return
        }
        val positionById = SpinnerUtil.getPositionById(this.sessions, i)
        if (positionById == -1) {
            return
        }
        Log.d(TAG, "LAST_SELECTED_SESSION was idx=$positionById id=$i")
        this.selectedSessionId = i
        this.sessionListAdapter?.setSelectedPosition(positionById)
    }

    private fun updateSessionInteractions() {
        _binding?.let { b ->
            b.butStart.isEnabled = interactionsEnabled
            b.butStart.alpha = if (interactionsEnabled) 1.0f else 0.4f
        }
        sessionListAdapter?.setInteractionsEnabled(interactionsEnabled)
    }

    fun updateSessionList() {
        lifecycleScope.launch {
            val readSessions = dbOperations.readSessions()
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
        }
    }

    fun selectLastSession() {
        if (this.sessions.isEmpty()) {
            setSelectedSessionId(-1)
        } else {
            setSelectedSessionId(this.sessions[this.sessions.size - 1].id)
        }
    }

    fun getSelectedSessionId(): Int = this.selectedSessionId

    fun setSelectedSessionId(i: Int) {
        this.selectedSessionId = i
        this.sessionListAdapter?.setSelectedPosition(SpinnerUtil.getPositionById(this.sessions, i))
    }

    companion object {
        private const val TAG = "ZMT_MainFragment"
    }
}
