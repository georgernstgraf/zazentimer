package at.priv.graf.zazentimer.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.bo.Session
import at.priv.graf.zazentimer.database.DbOperations
import at.priv.graf.zazentimer.databinding.FragmentEditSessionBinding
import at.priv.graf.zazentimer.views.MessageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SessionEditFragment : Fragment() {
    private var _binding: FragmentEditSessionBinding? = null
    private val binding get() = _binding!!

    private var messageView: MessageView? = null
    private var pref: SharedPreferences? = null
    private var sections: Array<Section>? = null
    private var session: Session? = null
    private var sessionId: Int = 0
    private var adapter: SectionListAdapter? = null

    @Inject
    lateinit var dbOperations: DbOperations

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        if (bundle != null) {
            this.sessionId = bundle.getInt("sessionId")
        } else if (arguments != null) {
            this.sessionId = requireArguments().getInt("sessionId")
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(
            object : androidx.core.view.MenuProvider {
                override fun onCreateMenu(
                    menu: Menu,
                    menuInflater: MenuInflater,
                ) {
                    menuInflater.inflate(R.menu.session_edit_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId == R.id.menu_session_edit_help) {
                        editHelper.showHelp13()
                        return true
                    }
                    return false
                }
            },
            viewLifecycleOwner,
            androidx.lifecycle.Lifecycle.State.RESUMED,
        )
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentEditSessionBinding.inflate(layoutInflater, viewGroup, false)
        this.pref = ZazenTimerActivity.getPreferences(requireContext())

        adapter =
            SectionListAdapter(
                object : SectionListAdapter.OnItemClickListener {
                    override fun onItemClick(section: Section) {
                        this@SessionEditFragment.editHelper.navigateToSectionEdit(section.id)
                    }
                },
                object : SectionListAdapter.OnSectionActionListener {
                    override fun onDeleteSection(position: Int) {
                        deleteSectionAt(position)
                    }

                    override fun onDuplicateSection(position: Int) {
                        duplicateSectionAt(position)
                    }
                },
            )

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        val callback =
            SectionTouchHelperCallback(
                object : SectionTouchHelperCallback.SectionTouchListener {
                    override fun onSwipe(position: Int) {
                        deleteSectionAt(position)
                    }

                    override fun onMove(
                        fromPosition: Int,
                        toPosition: Int,
                    ): Boolean {
                        adapter?.moveItem(fromPosition, toPosition) ?: return false
                        return true
                    }
                },
            )
        ItemTouchHelper(callback).attachToRecyclerView(binding.list)

        binding.butNewSection.setOnClickListener {
            editHelper.doCreateNewSection()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        bundle.putInt("sessionId", this.sessionId)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "sessionId=${this.sessionId}")
        lifecycleScope.launch {
            this@SessionEditFragment.session = dbOperations.readSession(this@SessionEditFragment.sessionId)
            if (!isAdded) return@launch
            val s = this@SessionEditFragment.session
            if (s == null) {
                Log.e(TAG, "session is NULL")
            } else {
                Log.i(TAG, "session found and valid")
                binding.textSitzungName.setText(s.name)
                binding.textSitzungBeschreibung.setText(s.description)
                this@SessionEditFragment.sections = dbOperations.readSections(s.id)
                editHelper.initSectionList()
            }
        }
        requireActivity().invalidateOptionsMenu()
        if (this.pref?.getBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, false) == false) {
            editHelper.showHelp13()
            this.pref
                ?.edit()
                ?.putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true)
                ?.apply()
        }
    }

    override fun onPause() {
        super.onPause()
        val items = adapter?.getItems() ?: return
        val sectionsToUpdate = ArrayList<Section>()
        for (i in items.indices) {
            val section = items[i]
            section.rank = i + 1
            sectionsToUpdate.add(section)
        }
        session?.let { s ->
            s.name = binding.textSitzungName.text.toString()
            s.description = binding.textSitzungBeschreibung.text.toString()
            runBlocking {
                for (section in sectionsToUpdate) {
                    dbOperations.updateSection(section)
                }
                dbOperations.updateSession(s)
            }
        }
    }

    private fun deleteSectionAt(position: Int) {
        val a = adapter ?: return
        val s = session ?: return
        val deletedSection = a.getItem(position)
        val deletedPosition = position
        lifecycleScope.launch {
            dbOperations.deleteSection(deletedSection.id.toLong())
        }
        a.removeItem(position)

        Snackbar
            .make(binding.list, getString(R.string.section_deleted, deletedSection), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.action_undo)) {
                lifecycleScope.launch {
                    dbOperations.insertSection(s, deletedSection)
                    a.insertItem(deletedPosition, deletedSection)
                }
            }.show()
    }

    private fun duplicateSectionAt(position: Int) {
        val a = adapter ?: return
        val s = session ?: return
        val source = a.getItem(position)
        val copy = Section(source.name ?: "", source.duration)
        copy.bell = source.bell
        copy.bellUri = source.bellUri
        copy.bellcount = source.bellcount
        copy.bellpause = source.bellpause
        copy.volume = source.volume
        lifecycleScope.launch {
            dbOperations.insertSection(s, copy)
            sections = dbOperations.readSections(s.id)
            editHelper.initSectionList()
        }
    }

    private val editHelper by lazy { SessionEditNavigationHelper() }

    private inner class SessionEditNavigationHelper {
        fun initSectionList() {
            adapter?.setItems(sections?.toList() ?: emptyList())
        }

        fun showHelp13() {
            if (messageView != null) {
                return
            }
            messageView = MessageView(requireActivity())
            messageView?.let { mv ->
                mv.setTitle(getString(R.string.help_sectionlist_title))
                mv.setText(getString(R.string.help_sectionlist_text))
                mv.setOnOkListener(
                    Runnable {
                        messageView = null
                    },
                )
                mv.show()
            }
        }

        fun doCreateNewSection() {
            val s = session ?: return
            val section =
                Section(
                    resources.getString(R.string.default_section_name),
                    DEFAULT_SECTION_DURATION_SECONDS,
                )
            lifecycleScope.launch {
                dbOperations.insertSection(s, section)
                navigateToSectionEdit(section.id)
            }
        }

        fun navigateToSectionEdit(sectionId: Int) {
            val args = Bundle()
            args.putInt("sectionId", sectionId)
            Navigation
                .findNavController(requireView())
                .navigate(R.id.action_sessionEditFragment_to_sectionEditFragment, args)
        }
    }

    companion object {
        private const val TAG = "ZMT_SessionEditFragment"
        private const val DEFAULT_SECTION_DURATION_SECONDS = 60
    }
}
