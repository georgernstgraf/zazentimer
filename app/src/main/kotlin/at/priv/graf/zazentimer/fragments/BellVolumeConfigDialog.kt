package at.priv.graf.zazentimer.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.audio.Audio
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Bell
import at.priv.graf.zazentimer.bo.SessionBellVolume
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class BellVolumeConfigDialog : DialogFragment() {
    private var audio: Audio? = null
    private var bellVolumes: MutableList<SessionBellVolume> = mutableListOf()
    private var sessionId: Int = 0

    companion object {
        private const val ARG_SESSION_ID = "sessionId"
        private const val ARG_IS_DARK = "isDark"
        private const val VOLUME_MAX = 100
        private const val VOLUME_STEP_SIZE = 10
        private const val VOLUME_MAX_STEP = 9

        fun newInstance(
            sessionId: Int,
            isDark: Boolean,
        ): BellVolumeConfigDialog {
            val args = Bundle()
            args.putInt(ARG_SESSION_ID, sessionId)
            args.putBoolean(ARG_IS_DARK, isDark)
            val dialog = BellVolumeConfigDialog()
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = arguments?.getInt(ARG_SESSION_ID) ?: 0
        val isDark = arguments?.getBoolean(ARG_IS_DARK) ?: false
        val theme = if (isDark) R.style.DarkZenTheme else R.style.LightZenTheme
        setStyle(STYLE_NORMAL, theme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(R.layout.dialog_bell_volume_config)
        val toolbar = dialog.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar?.setTitle(R.string.title_bell_volumes)
        toolbar?.setNavigationOnClickListener { dismiss() }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.dialog_bell_volume_config, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.bell_volume_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter =
            BellVolumeAdapter(bellVolumes) { position, volume ->
                bellVolumes[position].volume = volume
            }
        recyclerView.adapter = adapter

        val noBellsText = view.findViewById<TextView>(R.id.no_bells_text)
        if (bellVolumes.isEmpty()) {
            noBellsText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noBellsText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onResume() {
        super.onResume()
        audio = Audio(requireContext())
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            audio?.release()
        }
        audio = null
        (parentFragment as? OnBellVolumesSavedListener)?.onBellVolumesSaved(bellVolumes)
    }

    fun setBellVolumes(volumes: List<SessionBellVolume>) {
        bellVolumes = volumes.map { it.copy() }.toMutableList()
    }

    interface OnBellVolumesSavedListener {
        fun onBellVolumesSaved(volumes: List<SessionBellVolume>)
    }

    inner class BellVolumeAdapter(
        private val items: List<SessionBellVolume>,
        private val onVolumeChanged: (Int, Int) -> Unit,
    ) : RecyclerView.Adapter<BellVolumeAdapter.ViewHolder>() {
        inner class ViewHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val bellName: TextView = view.findViewById(R.id.bell_name)
            val seekBar: SeekBar = view.findViewById(R.id.bell_volume_seekbar)
            val volumeLabel: TextView = view.findViewById(R.id.bell_volume_label)
            val previewButton: ImageButton = view.findViewById(R.id.bell_preview)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            val view =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_bell_volume, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            val bv = items[position]
            val bell = findBellForVolume(bv)
            holder.bellName.text = bell?.getName() ?: ""

            val step = (VOLUME_MAX - bv.volume) / VOLUME_STEP_SIZE
            holder.seekBar.max = VOLUME_MAX_STEP
            holder.seekBar.progress = step.coerceIn(0, VOLUME_MAX_STEP)
            updateVolumeLabel(holder, bv.volume)

            holder.seekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean,
                    ) {
                        val vol = VOLUME_MAX - progress * VOLUME_STEP_SIZE
                        updateVolumeLabel(holder, vol)
                        onVolumeChanged(holder.adapterPosition, vol)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        // no-op: required by interface
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        val vol = VOLUME_MAX - (seekBar?.progress ?: 0) * VOLUME_STEP_SIZE
                        bell?.let { b ->
                            lifecycleScope.launch {
                                audio?.playAbsVolume(b, vol)
                            }
                        }
                    }
                },
            )

            holder.previewButton.setOnClickListener {
                val vol = VOLUME_MAX - holder.seekBar.progress * VOLUME_STEP_SIZE
                bell?.let { b ->
                    lifecycleScope.launch {
                        audio?.playAbsVolume(b, vol)
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        private fun updateVolumeLabel(
            holder: ViewHolder,
            volume: Int,
        ) {
            holder.volumeLabel.text = getString(R.string.bell_volume_label_format, volume)
        }

        @Suppress("ReturnCount")
        private fun findBellForVolume(bv: SessionBellVolume): Bell? {
            val uri = bv.bellUri
            if (uri != null) {
                BellCollection.getBellList().find { it.uri.toString() == uri }?.let { return it }
            }
            val idx = bv.bell ?: return null
            return BellCollection.getBell(idx)
        }
    }
}
