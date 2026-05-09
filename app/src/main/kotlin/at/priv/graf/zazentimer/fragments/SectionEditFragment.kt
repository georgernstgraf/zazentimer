package at.priv.graf.zazentimer.fragments

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import at.priv.graf.zazentimer.audio.Audio
import at.priv.graf.zazentimer.audio.BellCollection
import at.priv.graf.zazentimer.bo.Bell
import at.priv.graf.zazentimer.bo.Section
import at.priv.graf.zazentimer.database.DbOperations
import at.priv.graf.zazentimer.databinding.FragmentEditSectionBinding
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@AndroidEntryPoint
class SectionEditFragment : Fragment() {
    private var audio: Audio? = null
    private var _binding: FragmentEditSectionBinding? = null
    private val binding get() = _binding!!

    private var durationMinutes: Int = 0
    private var durationSeconds: Int = 0
    private var gongListAdapter: GongListAdapter? = null
    private var pref: SharedPreferences? = null
    private var section: Section? = null
    private var sectionId: Int = 0
    private var tvGaps: Array<TextView?> = arrayOfNulls(15)

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
            if (data.scheme == "content") {
                val query: Cursor? = requireActivity().contentResolver.query(data, null, null, null, null)
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
            } else {
                data.lastPathSegment?.let { str = "bell_$it" }
            }
            try {
                val openInputStream: InputStream? = requireActivity().contentResolver.openInputStream(data)
                if (openInputStream == null) return@registerForActivityResult
                val openFileOutput = requireActivity().openFileOutput(str, 0)
                val bArr = ByteArray(8192)
                var read = openInputStream.read(bArr)
                while (read > 0) {
                    openFileOutput.write(bArr, 0, read)
                    read = openInputStream.read(bArr)
                }
                openInputStream.close()
                openFileOutput.close()
                BellCollection.initialize(requireContext())
                fillBellList()
                section?.let { s ->
                    s.bellUri = BellCollection.getUriForName(str).toString()
                    selectBell(s.bellUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        if (bundle != null) {
            this.sectionId = bundle.getInt("sectionId")
        } else if (arguments != null) {
            this.sectionId = requireArguments().getInt("sectionId")
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putInt("sectionId", this.sectionId)
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentEditSectionBinding.inflate(layoutInflater, viewGroup, false)
        this.pref = ZazenTimerActivity.getPreferences(requireContext())
        getViewComponents()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            this@SectionEditFragment.section = dbOperations.readSection(this@SectionEditFragment.sectionId)
            if (!isAdded) return@launch
            this@SectionEditFragment.audio = Audio(requireContext())
            requireActivity().invalidateOptionsMenu()
            fillViewFromData()
            installListeners()
            val s = section ?: return@launch
            binding.bellGapScrollview.post {
                val idx = s.bellpause - 1
                if (idx < 0 || idx >= tvGaps.size) return@post
                val textView = tvGaps[idx] ?: return@post
                val rect = Rect()
                textView.getDrawingRect(rect)
                binding.bellGapScrollview.requestChildRectangleOnScreen(textView, rect, true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        audio?.release()
        this.audio = null
        fillDataFromViews()
        section?.let { s ->
            lifecycleScope.launch {
                dbOperations.updateSection(s)
            }
        }
    }

    private fun fillDataFromViews() {
        val s = section ?: return
        s.bell = -2
        val bell = binding.selectGongSound.selectedItem as Bell
        s.bellUri = bell.uri.toString()
        s.volume = 100 - binding.sectionGongVolume.progress * 10
        s.name = binding.sectionName.text.toString()
        s.duration = (this.durationMinutes * 60) + this.durationSeconds
    }

    private fun fillViewFromData() {
        val s = section ?: return
        setViewBellCount(s.bellcount)
        setViewGap(s.bellpause)
        binding.sectionGongVolume.max = 9
        var step = (100 - s.volume) / 10
        step = Math.max(0, Math.min(9, step))
        binding.sectionGongVolume.progress = step
        updateDimLabel(step)
        binding.sectionName.setText(s.name)
        setDurationMinutes(s.duration / 60)
        setDurationSeconds(s.duration % 60)
        fillBellList()
        selectBell(s.bellUri)
    }

    private fun fillBellList() {
        this.gongListAdapter = GongListAdapter(requireContext(), R.id.selectGongSound, R.id.spinnerText1)
        val bellList = BellCollection.getBellList()
        val adapter = this.gongListAdapter ?: return
        for (i in bellList.indices) {
            adapter.add(bellList[i])
        }
        binding.selectGongSound.adapter = adapter as SpinnerAdapter
    }

    private fun selectBell(str: String?) {
        val bellList = BellCollection.getBellList()
        for (i in bellList.indices) {
            if (bellList[i].uri.toString() == str) {
                binding.selectGongSound.setSelection(i)
            }
        }
    }

    private fun getViewComponents() {
        this.tvGaps[0] = binding.gap1
        this.tvGaps[1] = binding.gap2
        this.tvGaps[2] = binding.gap3
        this.tvGaps[3] = binding.gap4
        this.tvGaps[4] = binding.gap5
        this.tvGaps[5] = binding.gap6
        this.tvGaps[6] = binding.gap7
        this.tvGaps[7] = binding.gap8
        this.tvGaps[8] = binding.gap9
        this.tvGaps[9] = binding.gap10
        this.tvGaps[10] = binding.gap11
        this.tvGaps[11] = binding.gap12
        this.tvGaps[12] = binding.gap13
        this.tvGaps[13] = binding.gap14
        this.tvGaps[14] = binding.gap15
    }

    protected fun installListeners() {
        binding.addcustombell.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.flags = 1
            intent.type = "audio/*"
            bellPickerLauncher.launch(Intent.createChooser(intent, resources.getString(R.string.select_audio)))
        }
        binding.bellcount1.setOnClickListener {
            section?.let { s ->
                s.bellcount = 1
                setViewBellCount(s.bellcount)
            }
        }
        binding.bellcount2.setOnClickListener {
            section?.let { s ->
                s.bellcount = 2
                setViewBellCount(s.bellcount)
            }
        }
        binding.bellcount3.setOnClickListener {
            section?.let { s ->
                s.bellcount = 3
                setViewBellCount(s.bellcount)
            }
        }
        binding.bellcount4.setOnClickListener {
            section?.let { s ->
                s.bellcount = 4
                setViewBellCount(s.bellcount)
            }
        }
        binding.bellcount5.setOnClickListener {
            section?.let { s ->
                s.bellcount = 5
                setViewBellCount(s.bellcount)
            }
        }
        for (i in 0..14) {
            tvGaps[i]?.setOnClickListener { view ->
                val s = section ?: return@setOnClickListener
                for (i2 in 0..14) {
                    val gapView = tvGaps[i2]
                    if (gapView != null && view.id == gapView.id) {
                        s.bellpause = i2 + 1
                        break
                    }
                }
                setViewGap(s.bellpause)
            }
        }
        binding.duration.setOnClickListener {
            this@SectionEditFragment.pickDuration()
        }
        binding.playGong.setOnClickListener {
            val s = section ?: return@setOnClickListener
            val bellForSection = BellCollection.getBellForSection(s)
            bellForSection?.let { bell ->
                lifecycleScope.launch {
                    audio?.playAbsVolume(bell, s.volume)
                }
            }
        }
        binding.selectGongSound.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                }

                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    i2: Int,
                    j: Long,
                ) {
                    val s = section ?: return
                    val bell = BellCollection.getBell(i2) ?: return
                    if (bell.uri.toString() == s.bellUri) {
                        return
                    }
                    s.bellUri = bell.uri.toString()
                    s.bell = -2
                }
            }
        binding.sectionGongVolume.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    this@SectionEditFragment.updateDimLabel(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val s = section ?: return
                    val progress = seekBar?.progress ?: return
                    s.volume = 100 - progress * 10
                    val bellForSection = BellCollection.getBellForSection(s)
                    bellForSection?.let { bell ->
                        lifecycleScope.launch {
                            audio?.playAbsVolume(s)
                        }
                    }
                }
            },
        )
    }

    private fun setViewBellCount(i: Int) {
        binding.bellcount1.isSelected = i >= 1
        binding.bellcount2.isSelected = i >= 2
        binding.bellcount3.isSelected = i >= 3
        binding.bellcount4.isSelected = i >= 4
        binding.bellcount5.isSelected = i >= 5
    }

    private fun setViewGap(i: Int) {
        for (i2 in 0..14) {
            val gapView = tvGaps[i2] ?: continue
            gapView.isSelected = (i2 + 1) == i
        }
    }

    private fun pickDuration() {
        val timePickerFragment = TimePickerFragment()
        timePickerFragment.setMinutes(this.durationMinutes)
        timePickerFragment.setSeconds(this.durationSeconds)
        timePickerFragment.setOnOkListener(
            Runnable {
                this@SectionEditFragment.setDurationMinutes(timePickerFragment.getMinutes())
                this@SectionEditFragment.setDurationSeconds(timePickerFragment.getSeconds())
            },
        )
        timePickerFragment.show(parentFragmentManager, "timePicker")
    }

    fun setDurationSeconds(i: Int) {
        this.durationSeconds = i
        updateDurationView()
    }

    fun setDurationMinutes(i: Int) {
        this.durationMinutes = i
        updateDurationView()
    }

    private fun updateDurationView() {
        _binding?.let { b ->
            b.time.text = String.format("%02d:%02d", durationMinutes, durationSeconds)
        }
    }

    fun setSectionId(i: Int) {
        this.sectionId = i
    }

    private fun updateDimLabel(step: Int) {
        _binding?.let { b ->
            if (step == 0) {
                b.dimBellLabel.setText(R.string.dim_bell_label_off)
            } else {
                b.dimBellLabel.text = getString(R.string.dim_bell_label_format, step)
            }
        }
    }

    companion object {
        private const val TAG = "ZMT_SectionEdit"
    }
}
