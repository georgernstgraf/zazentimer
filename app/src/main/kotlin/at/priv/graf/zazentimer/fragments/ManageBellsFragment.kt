package at.priv.graf.zazentimer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import javax.inject.Inject

@AndroidEntryPoint
class ManageBellsFragment : Fragment() {
    private var _binding: FragmentManageBellsBinding? = null
    private val binding get() = _binding!!

    private var bells: List<BellEntity> = emptyList()

    @Inject
    lateinit var dbOperations: DbOperations

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
        AlertDialog
            .Builder(requireContext())
            .setTitle(R.string.manage_bells)
            .setMessage(getString(R.string.confirm_delete_bell, bell.name))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                deleteBell(bell)
            }
    }

    private fun deleteBell(bell: BellEntity) {
        lifecycleScope.launch {
            dbOperations.deleteCustomBell(bell._id)

            val fileUri = bell.uri
            if (fileUri.startsWith("file://")) {
                val filePath = fileUri.removePrefix("file://")
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
}
