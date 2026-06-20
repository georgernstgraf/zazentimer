package at.priv.graf.zazentimer.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.audio.BellImportException
import at.priv.graf.zazentimer.audio.BellImporter
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.BellRepository
import at.priv.graf.zazentimer.database.SectionRepository
import at.priv.graf.zazentimer.database.SessionRepository
import at.priv.graf.zazentimer.databinding.FragmentManageBellsBinding
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ManageBellsFragment : Fragment() {
    private var _binding: FragmentManageBellsBinding? = null
    private val binding get() = _binding!!

    private var bells: List<BellEntity> = emptyList()

    @Inject
    lateinit var bellRepo: BellRepository

    @Inject
    lateinit var sessionRepo: SessionRepository

    @Inject
    lateinit var sectionRepo: SectionRepository

    @Inject
    lateinit var bellImporter: BellImporter

    private val bellPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
            val data = result.data?.data ?: return@registerForActivityResult
            lifecycleScope.launch {
                try {
                    bellImporter.import(data) ?: return@launch
                    loadCustomBells()
                } catch (e: BellImportException) {
                    Toast
                        .makeText(
                            requireActivity(),
                            e.message ?: getString(R.string.bell_import_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?,
    ): View {
        _binding = FragmentManageBellsBinding.inflate(layoutInflater, viewGroup, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.bellList.layoutManager = LinearLayoutManager(requireContext())
        binding.importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.type = "audio/*"
            bellPickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_audio)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        loadCustomBells()
    }

    private fun loadCustomBells() {
        lifecycleScope.launch {
            bells = bellRepo.getNonBuiltinBells()
            if (bells.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.bellList.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.bellList.visibility = View.VISIBLE
                binding.bellList.adapter = BellAdapter(bells)
            }
        }
    }

    private fun confirmDeleteBell(bell: BellEntity) {
        lifecycleScope.launch {
            val affectedParts = mutableListOf<String>()
            for (session in sessionRepo.readSessions()) {
                for (section in sectionRepo.readSections(session.id)) {
                    if (section.bellId == bell.id) {
                        val part =
                            getString(
                                R.string.affected_section_format,
                                section.name ?: getString(R.string.unnamed),
                                session.name ?: getString(R.string.unnamed),
                            )
                        affectedParts.add(part)
                    }
                }
            }
            val message =
                if (affectedParts.isEmpty()) {
                    getString(R.string.confirm_delete_bell, bell.name)
                } else {
                    getString(
                        R.string.confirm_delete_bell_affected,
                        bell.name,
                        affectedParts.joinToString("\n"),
                    )
                }
            AlertDialog
                .Builder(requireContext())
                .setTitle(R.string.manage_bells)
                .setMessage(message)
                .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                    deleteBell(bell)
                }.setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun deleteBell(bell: BellEntity) {
        lifecycleScope.launch {
            bellRepo.deleteCustomBell(bell.id)

            if (bell.uri.startsWith("file://")) {
                val filePath = bell.uri.removePrefix("file://")
                File(filePath).delete()
            }

            loadCustomBells()
        }
    }

    private inner class BellAdapter(
        private val items: List<BellEntity>,
    ) : RecyclerView.Adapter<BellAdapter.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_manage_bell, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            val bell = items[position]
            holder.nameView.text = bell.name
            holder.deleteButton.setOnClickListener {
                confirmDeleteBell(bell)
            }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val nameView: TextView = view.findViewById(R.id.bell_name)
            val deleteButton: Button = view.findViewById(R.id.delete_button)
        }
    }
}
