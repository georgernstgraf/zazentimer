package at.priv.graf.zazentimer.fragments

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
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
import at.priv.graf.zazentimer.database.BellEntity
import at.priv.graf.zazentimer.database.DbOperations
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

class BellVolumeConfigDialog : DialogFragment() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BellVolumeConfigDialogEntryPoint {
        fun dbOperations(): DbOperations
    }

    private lateinit var dbOperations: DbOperations

    private var audio: Audio? = null
    private var bellVolumes: MutableList<SessionBellVolume> = mutableListOf()
    private var sessionId: Int = 0
    private var bellEntities: Map<Int, BellEntity> = emptyMap()
    private var systemVolumeSeekBar: SeekBar? = null
    private var systemVolumeLabel: TextView? = null
    private var volumeChangeReceiver: BroadcastReceiver? = null

    companion object {
        private const val ARG_SESSION_ID = "sessionId"
        private const val ARG_IS_DARK = "isDark"
        private const val STATE_BELL_VOLUMES = "bellVolumes"
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
        val entryPoint =
            EntryPoints.get(
                requireContext().applicationContext,
                BellVolumeConfigDialogEntryPoint::class.java,
            )
        dbOperations = entryPoint.dbOperations()

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
        if (savedInstanceState != null) {
            @Suppress("DEPRECATION")
            val saved = savedInstanceState.getSerializable(STATE_BELL_VOLUMES) as? ArrayList<SessionBellVolume>
            if (saved != null && saved.isNotEmpty()) {
                bellVolumes = saved.toMutableList()
            }
        }
        val recyclerView = view.findViewById<RecyclerView>(R.id.bell_volume_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter =
            BellVolumeAdapter(bellVolumes) { position, volume ->
                bellVolumes[position].volume = volume
            }
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            val entities = dbOperations.getAllBells()
            bellEntities = entities.associateBy { it._id }
            adapter.notifyDataSetChanged()
        }

        val noBellsText = view.findViewById<TextView>(R.id.no_bells_text)
        if (bellVolumes.isEmpty()) {
            noBellsText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noBellsText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        setupSystemVolumeControl(view)
    }

    private fun setupSystemVolumeControl(view: View) {
        val seekBar = view.findViewById<SeekBar>(R.id.system_volume_seekbar)
        val label = view.findViewById<TextView>(R.id.system_volume_label)
        systemVolumeSeekBar = seekBar
        systemVolumeLabel = label

        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        seekBar.max = maxVolume
        seekBar.progress = currentVolume
        label.text = getString(R.string.system_volume_label_format, currentVolume, maxVolume)

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    label.text = getString(R.string.system_volume_label_format, progress, maxVolume)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // no-op: required by interface
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val vol = seekBar?.progress ?: return
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
                }
            },
        )
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

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        volumeChangeReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType != AudioManager.STREAM_ALARM) return
                    val volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                    val prevVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                    if (volume == prevVolume) return
                    systemVolumeSeekBar?.progress = volume
                    systemVolumeSeekBar?.max?.let { max ->
                        systemVolumeLabel?.text = getString(R.string.system_volume_label_format, volume, max)
                    }
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                volumeChangeReceiver,
                filter,
                Context.RECEIVER_EXPORTED,
            )
        } else {
            @Suppress("DEPRECATION")
            requireContext().registerReceiver(volumeChangeReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        volumeChangeReceiver?.let { requireContext().unregisterReceiver(it) }
        volumeChangeReceiver = null

        lifecycleScope.launch {
            audio?.release()
        }
        audio = null
        (parentFragment as? OnBellVolumesSavedListener)?.onBellVolumesSaved(bellVolumes)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(STATE_BELL_VOLUMES, ArrayList(bellVolumes))
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
            val entity = bellEntities[bv.bellId]
            if (entity != null) {
                val uriStr = entity.uri
                val found = BellCollection.getBellList().find { it.uri.toString() == uriStr }
                if (found != null) return found
                BellCollection.getBell(entity.name)?.let { return it }
            }

            return BellCollection.getDemoBell()
        }
    }
}
