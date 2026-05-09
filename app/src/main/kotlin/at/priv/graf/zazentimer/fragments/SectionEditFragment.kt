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
            val intent = result.data!!
            val data = intent.data
            if (data == null) return@registerForActivityResult
            var str = "bell_unnamed"
            if (data.scheme == "content") {
                val query: Cursor? = requireActivity().contentResolver.query(data, null, null, null, null)
                if (query != null && query.count != 0) {
                    val columnIndex = query.getColumnIndex("_display_name")
                    if (columnIndex >= 0) {
                        query.moveToFirst()
                        str = "bell_" + query.getString(columnIndex)
                    } else {
                        str = "bell_" + data.lastPathSegment
                    }
                }
                query?.close()
            } else {
                str = "bell_" + data.lastPathSegment
            }
            try {
                val openInputStream: InputStream = requireActivity().contentResolver.openInputStream(data)!!
                val openFileOutput = requireActivity().openFileOutput(str, 0)
                val bArr = ByteArray(8192)
                var read = openInputStream.read(bArr)
                while (read > 0) {
                    openFileOutput.write(bArr, 0, read)
                    read = openInputStream.read(bArr)
                }
                openInputStream.close()
                openFileOutput.close()
                BellCollection.initialize(requireActivity())
                fillBellList()
                this.section!!.bellUri = BellCollection.getUriForName(str).toString()
                selectBell(this.section!!.bellUri)
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
        this.pref = ZazenTimerActivity.getPreferences(requireActivity())
        getViewComponents()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        this.section = dbOperations.readSection(this.sectionId)
        this.audio = Audio(requireActivity())
        requireActivity().invalidateOptionsMenu()
        fillViewFromData()
        installListeners()
        binding.bellGapScrollview.post {
            val textView = this@SectionEditFragment.tvGaps[this@SectionEditFragment.section!!.bellpause - 1]!!
            val rect = Rect()
            textView.getDrawingRect(rect)
            this@SectionEditFragment.binding.bellGapScrollview.requestChildRectangleOnScreen(textView, rect, true)
        }
    }

    override fun onPause() {
        super.onPause()
        this.audio!!.release()
        this.audio = null
        fillDataFromViews()
        dbOperations.updateSection(this.section!!)
    }

    private fun fillDataFromViews() {
        this.section!!.bell = -2
        val bell = binding.selectGongSound.selectedItem as Bell
        this.section!!.bellUri = bell.uri.toString()
        this.section!!.volume = 100 - binding.sectionGongVolume.progress * 10
        this.section!!.name = binding.sectionName.text.toString()
        this.section!!.duration = (this.durationMinutes * 60) + this.durationSeconds
    }

    private fun fillViewFromData() {
        setViewBellCount(this.section!!.bellcount)
        setViewGap(this.section!!.bellpause)
        binding.sectionGongVolume.max = 9
        var step = (100 - this.section!!.volume) / 10
        step = Math.max(0, Math.min(9, step))
        binding.sectionGongVolume.progress = step
        updateDimLabel(step)
        binding.sectionName.setText(this.section!!.name)
        setDurationMinutes(this.section!!.duration / 60)
        setDurationSeconds(this.section!!.duration % 60)
        fillBellList()
        selectBell(this.section!!.bellUri)
    }

    private fun fillBellList() {
        this.gongListAdapter = GongListAdapter(requireActivity(), R.id.selectGongSound, R.id.spinnerText1)
        val bellList = BellCollection.getBellList()
        for (i in bellList.indices) {
            this.gongListAdapter!!.add(bellList[i])
        }
        binding.selectGongSound.adapter = this.gongListAdapter as SpinnerAdapter
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
            bellPickerLauncher.launch(Intent.createChooser(intent, this@SectionEditFragment.resources.getString(R.string.select_audio)))
        }
        binding.bellcount1.setOnClickListener {
            this@SectionEditFragment.section!!.bellcount = 1
            this@SectionEditFragment.setViewBellCount(this@SectionEditFragment.section!!.bellcount)
        }
        binding.bellcount2.setOnClickListener {
            this@SectionEditFragment.section!!.bellcount = 2
            this@SectionEditFragment.setViewBellCount(this@SectionEditFragment.section!!.bellcount)
        }
        binding.bellcount3.setOnClickListener {
            this@SectionEditFragment.section!!.bellcount = 3
            this@SectionEditFragment.setViewBellCount(this@SectionEditFragment.section!!.bellcount)
        }
        binding.bellcount4.setOnClickListener {
            this@SectionEditFragment.section!!.bellcount = 4
            this@SectionEditFragment.setViewBellCount(this@SectionEditFragment.section!!.bellcount)
        }
        binding.bellcount5.setOnClickListener {
            this@SectionEditFragment.section!!.bellcount = 5
            this@SectionEditFragment.setViewBellCount(this@SectionEditFragment.section!!.bellcount)
        }
        for (i in 0..14) {
            this.tvGaps[i]!!.setOnClickListener { view ->
                var i2 = 0
                while (true) {
                    if (i2 >= 15) {
                        break
                    }
                    if (view.id == this@SectionEditFragment.tvGaps[i2]!!.id) {
                        this@SectionEditFragment.section!!.bellpause = i2 + 1
                        break
                    }
                    i2++
                }
                this@SectionEditFragment.setViewGap(this@SectionEditFragment.section!!.bellpause)
            }
        }
        binding.duration.setOnClickListener {
            this@SectionEditFragment.pickDuration()
        }
        binding.playGong.setOnClickListener {
            val bellForSection = BellCollection.getBellForSection(this@SectionEditFragment.section!!)
            if (bellForSection != null) {
                this@SectionEditFragment.audio!!.playAbsVolume(bellForSection, this@SectionEditFragment.section!!.volume)
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
                    val bell = BellCollection.getBell(i2)!!
                    if (bell.uri.toString() == this@SectionEditFragment.section!!.bellUri) {
                        return
                    }
                    this@SectionEditFragment.section!!.bellUri = bell.uri.toString()
                    this@SectionEditFragment.section!!.bell = -2
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
                    val progress = seekBar!!.progress
                    this@SectionEditFragment.section!!.volume = 100 - progress * 10
                    if (BellCollection.getBellForSection(this@SectionEditFragment.section!!) != null) {
                        this@SectionEditFragment.audio!!.playAbsVolume(this@SectionEditFragment.section!!)
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
        var i2 = 0
        while (i2 < 15) {
            val i3 = i2 + 1
            this.tvGaps[i2]!!.isSelected = i3 == i
            i2 = i3
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
            b.time.text = String.format("%02d", this.durationMinutes) + ":" + String.format("%02d", this.durationSeconds)
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
