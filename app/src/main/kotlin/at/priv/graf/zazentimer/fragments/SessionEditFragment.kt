package at.priv.graf.zazentimer.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
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

    private fun handleAttach(context: Context?) {
    }

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
        @NonNull view: View,
        @Nullable savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(
            object : androidx.core.view.MenuProvider {
                override fun onCreateMenu(
                    @NonNull menu: Menu,
                    @NonNull menuInflater: MenuInflater,
                ) {
                    menuInflater.inflate(R.menu.session_edit_menu, menu)
                }

                override fun onMenuItemSelected(
                    @NonNull menuItem: MenuItem,
                ): Boolean {
                    if (menuItem.itemId == R.id.menu_session_edit_help) {
                        showHelp13()
                        return true
                    }
                    return false
                }
            },
            viewLifecycleOwner,
            androidx.lifecycle.Lifecycle.State.RESUMED,
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        handleAttach(context)
    }

    @Nullable
    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentEditSessionBinding.inflate(layoutInflater, viewGroup, false)
        this.pref = ZazenTimerActivity.getPreferences(requireActivity())

        adapter =
            SectionListAdapter(
                object : SectionListAdapter.OnItemClickListener {
                    override fun onItemClick(section: Section) {
                        this@SessionEditFragment.navigateToSectionEdit(section.id)
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

        binding.list.layoutManager = LinearLayoutManager(activity)
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
                        adapter!!.moveItem(fromPosition, toPosition)
                        return true
                    }
                },
            )
        ItemTouchHelper(callback).attachToRecyclerView(binding.list)

        binding.butNewSection.setOnClickListener {
            this@SessionEditFragment.doCreateNewSection()
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

    fun setSessionId(i: Int) {
        this.sessionId = i
    }

    private fun initSectionList() {
        adapter?.setItems(this.sections?.toList() ?: emptyList())
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "sessionId=${this.sessionId}")
        this.session = dbOperations.readSession(this.sessionId)
        if (this.session == null) {
            Log.e(TAG, "session is NULL")
        } else {
            Log.i(TAG, "session found and valid")
        }
        binding.textSitzungName.setText(this.session!!.name)
        binding.textSitzungBeschreibung.setText(this.session!!.description)
        requireActivity().invalidateOptionsMenu()
        this.sections = dbOperations.readSections(this.session!!.id)
        initSectionList()
        binding.textSitzungName.setText(this.session!!.name)
        binding.textSitzungBeschreibung.setText(this.session!!.description)
        if (this.pref?.getBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, false) == false) {
            showHelp13()
            this.pref
                ?.edit()
                ?.putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true)
                ?.apply()
        }
    }

    private fun showHelp13() {
        if (this.messageView != null) {
            return
        }
        this.messageView = MessageView(requireActivity())
        this.messageView!!.setTitle(getString(R.string.help_sectionlist_title))
        this.messageView!!.setText(getString(R.string.help_sectionlist_text))
        this.messageView!!.setOnOkListener(
            Runnable {
                this@SessionEditFragment.messageView = null
            },
        )
        this.messageView!!.show()
    }

    override fun onPause() {
        super.onPause()
        val items = adapter!!.getItems()
        for (i in items.indices) {
            val section = items[i]
            section.rank = i + 1
            dbOperations.updateSection(section)
        }
        this.session!!.name = binding.textSitzungName.text.toString()
        this.session!!.description = binding.textSitzungBeschreibung.text.toString()
        dbOperations.updateSession(this.session!!)
    }

    private fun deleteSectionAt(position: Int) {
        val deletedSection = adapter!!.getItem(position)
        val deletedPosition = position
        dbOperations.deleteSection(deletedSection.id.toLong())
        adapter!!.removeItem(position)

        Snackbar
            .make(binding.list, "Deleted '" + deletedSection.toString() + "'", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                dbOperations.insertSection(this@SessionEditFragment.session!!, deletedSection)
                adapter!!.insertItem(deletedPosition, deletedSection)
            }.show()
    }

    private fun duplicateSectionAt(position: Int) {
        val source = adapter!!.getItem(position)
        val copy = Section(source.name ?: "", source.duration)
        copy.bell = source.bell
        copy.bellUri = source.bellUri
        copy.bellcount = source.bellcount
        copy.bellpause = source.bellpause
        copy.volume = source.volume
        dbOperations.insertSection(this.session!!, copy)
        sections = dbOperations.readSections(this.session!!.id)
        initSectionList()
    }

    fun doCreateNewSection() {
        val section = Section(resources.getString(R.string.default_section_name), 60)
        dbOperations.insertSection(this.session!!, section)
        navigateToSectionEdit(section.id)
    }

    private fun navigateToSectionEdit(sectionId: Int) {
        val args = Bundle()
        args.putInt("sectionId", sectionId)
        Navigation.findNavController(requireView()).navigate(R.id.action_sessionEditFragment_to_sectionEditFragment, args)
    }

    companion object {
        private const val TAG = "ZMT_SessionEditFragment"
    }
}
