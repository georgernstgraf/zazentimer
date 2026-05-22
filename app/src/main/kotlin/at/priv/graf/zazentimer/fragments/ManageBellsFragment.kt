package at.priv.graf.zazentimer.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.DbOperations
import at.priv.graf.zazentimer.databinding.FragmentManageBellsBinding
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class ManageBellsFragment : Fragment() {
    private var _binding: FragmentManageBellsBinding? = null
    private val binding get() = _binding!!

    private var bells: List<BellEntity> = emptyList()

    @Inject
    lateinit var dbOperations: DbOperations

    private val bellPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
            val intent = result.data ?: return@registerForActivityResult
            val data = intent.data ?: return@registerForActivityResult
            var str = "bell_unnamed"
            val query = requireActivity().contentResolver.query(data, null, null, null, null)
            if (query != null && query.count != 0) {
                val columnIndex = query.getColumnIndex("_display_name")
                if (columnIndex >= 0) {
                    query.moveToFirst()
                    val colVal = query.getString(columnIndex) ?: ""
                    str = "bell_$colVal"
                } else {
                    val segment = data.lastPathSegment ?: ""
                    str = "bell_$segment"
                }
            }
            query?.close()
            try {
                val openInputStream =
                    requireActivity().contentResolver.openInputStream(data) ?: return@registerForActivityResult
                val openFileOutput = requireActivity().openFileOutput(str, 0)
                val bArr = ByteArray(BUFFER_SIZE)
                var read = openInputStream.read(bArr)
                while (read > 0) {
                    openFileOutput.write(bArr, 0, read)
                    read = openInputStream.read(bArr)
                }
                openInputStream.close()
                openFileOutput.close()
                BellCollection.initialize(requireContext())
                val bellUri = BellCollection.getUriForName(str)
                if (bellUri != null) {
                    lifecycleScope.launch {
                        dbOperations.insertBell(
                            BellEntity(
                                name = str.removePrefix("bell_"),
                                uri = bellUri.toString(),
                            ),
                        )
                        loadCustomBells()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
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
            bells = dbOperations.getNonBuiltinBells()
            if (bells.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.list.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.list.visibility = View.VISIBLE
                binding.list.adapter = BellAdapter(bells)
            }
        }
    }

    private fun confirmDeleteBell(bell: BellEntity) {
        lifecycleScope.launch {
            val affectedParts = mutableListOf<String>()
            for (session in dbOperations.readSessions()) {
                for (section in dbOperations.readSections(session.id)) {
                    if (section.bellId == bell._id) {
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
            dbOperations.deleteCustomBell(bell._id)

            if (bell.uri.startsWith("file://")) {
                val filePath = bell.uri.removePrefix("file://")
                File(filePath).delete()
            }

            BellCollection.initialize(requireContext())

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
            val nameView: TextView = view.findViewById(R.id.bellName)
            val deleteButton: Button = view.findViewById(R.id.deleteButton)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 8192
    }
}
