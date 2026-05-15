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
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.util.Locale
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
    private var tvGaps: Array<TextView?> = arrayOfNulls(GAP_ARRAY_SIZE)

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
                val bArr = ByteArray(BUFFER_SIZE)
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
            } catch (e: IOException) {
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
        viewLifecycleOwner.lifecycleScope.launch {
            this@SectionEditFragment.section = dbOperations.readSection(this@SectionEditFragment.sectionId)
            if (!isAdded) return@launch
            this@SectionEditFragment.audio = Audio(requireContext())
            requireActivity().invalidateOptionsMenu()
            fillViewFromData()
            installListeners()
            val s = section ?: return@launch
            _binding?.bellGapScrollview?.post {
                val idx = s.bellpause - 1
                if (idx < 0 || idx >= tvGaps.size) return@post
                val textView = tvGaps[idx] ?: return@post
                val rect = Rect()
                textView.getDrawingRect(rect)
                _binding?.bellGapScrollview?.requestChildRectangleOnScreen(textView, rect, true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        runBlocking {
            audio?.release()
        }
        this.audio = null
        fillDataFromViews()
        section?.let { s ->
            runBlocking {
                dbOperations.updateSection(s)
            }
        }
    }

    fun setDurationSeconds(i: Int) {
        this.durationSeconds = i
        updateDurationView()
    }

    fun setDurationMinutes(i: Int) {
        this.durationMinutes = i
        updateDurationView()
    }

    fun setSectionId(i: Int) {
        this.sectionId = i
    }

    @Suppress("TooManyFunctions")
    companion object {
        private const val TAG = "ZMT_SectionEdit"
        private const val BUFFER_SIZE = 8192
        private const val SECONDS_PER_MINUTE = 60
        private const val DEFAULT_BELL_VOLUME = 100
        private const val GAP_COUNT = 14
        private const val GAP_ARRAY_SIZE = 15

        private fun SectionEditFragment.fillDataFromViews() {
            val s = section ?: return
            s.bell = binding.selectGongSound.selectedItemPosition
            val bell = binding.selectGongSound.selectedItem as Bell
            s.bellUri = bell.uri.toString()
            s.name = binding.sectionName.text.toString()
            s.duration = (this.durationMinutes * SECONDS_PER_MINUTE) + this.durationSeconds
        }

        private fun SectionEditFragment.fillViewFromData() {
            val s = section ?: return
            setViewBellCount(s.bellcount)
            setViewGap(s.bellpause)
            binding.sectionName.setText(s.name)
            setDurationMinutes(s.duration / SECONDS_PER_MINUTE)
            setDurationSeconds(s.duration % SECONDS_PER_MINUTE)
            fillBellList()
            selectBell(s.bellUri)
        }

        private fun SectionEditFragment.getViewComponents() {
            val gaps =
                arrayOf(
                    binding.gap1,
                    binding.gap2,
                    binding.gap3,
                    binding.gap4,
                    binding.gap5,
                    binding.gap6,
                    binding.gap7,
                    binding.gap8,
                    binding.gap9,
                    binding.gap10,
                    binding.gap11,
                    binding.gap12,
                    binding.gap13,
                    binding.gap14,
                    binding.gap15,
                )
            for (i in gaps.indices) {
                this.tvGaps[i] = gaps[i]
            }
        }

        private fun SectionEditFragment.installListeners() {
            installCustomBellListener()
            installBellCountListeners()
            installGapListeners()
            installPlayGongListener()
            installBellSelectionListener()
        }

        private fun SectionEditFragment.installCustomBellListener() {
            binding.addcustombell.setOnClickListener {
                val intent = Intent("android.intent.action.GET_CONTENT")
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                intent.type = "audio/*"
                bellPickerLauncher.launch(Intent.createChooser(intent, resources.getString(R.string.select_audio)))
            }
        }

        private fun SectionEditFragment.installBellCountListeners() {
            val bellViews =
                arrayOf(
                    binding.bellcount1,
                    binding.bellcount2,
                    binding.bellcount3,
                    binding.bellcount4,
                    binding.bellcount5,
                )
            for (idx in bellViews.indices) {
                val count = idx + 1
                bellViews[idx].setOnClickListener {
                    section?.bellcount = count
                    section?.let { setViewBellCount(it.bellcount) }
                }
            }
        }

        private fun SectionEditFragment.installGapListeners() {
            for (i in 0..GAP_COUNT) {
                tvGaps[i]?.setOnClickListener { view ->
                    val s = section ?: return@setOnClickListener
                    for (i2 in 0..GAP_COUNT) {
                        val gapView = tvGaps[i2]
                        if (gapView != null && view.id == gapView.id) {
                            s.bellpause = i2 + 1
                            break
                        }
                    }
                    setViewGap(s.bellpause)
                }
            }
        }

        private fun SectionEditFragment.installPlayGongListener() {
            binding.duration.setOnClickListener {
                pickDuration()
            }
            binding.playGong.setOnClickListener {
                val s = section ?: return@setOnClickListener
                val bellForSection = BellCollection.getBellForSection(s)
                bellForSection?.let { bell ->
                    lifecycleScope.launch {
                        audio?.playAbsVolume(bell, DEFAULT_BELL_VOLUME)
                    }
                }
            }
        }

        private fun SectionEditFragment.installBellSelectionListener() {
            binding.selectGongSound.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(adapterView: AdapterView<*>?) {
                        // no-op: required by interface
                    }

                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        i2: Int,
                        j: Long,
                    ) {
                        section?.let { s ->
                            BellCollection.getBell(i2)?.let { bell ->
                                if (bell.uri.toString() != s.bellUri) {
                                    s.bellUri = bell.uri.toString()
                                    s.bell = i2
                                }
                            }
                        }
                    }
                }
        }

        private fun SectionEditFragment.setViewBellCount(count: Int) {
            val bellViews =
                arrayOf(
                    binding.bellcount1,
                    binding.bellcount2,
                    binding.bellcount3,
                    binding.bellcount4,
                    binding.bellcount5,
                )
            for (idx in bellViews.indices) {
                bellViews[idx].isSelected = count >= (idx + 1)
            }
        }

        private fun SectionEditFragment.setViewGap(gapIndex: Int) {
            for (i2 in 0..GAP_COUNT) {
                val gapView = tvGaps[i2] ?: continue
                gapView.isSelected = (i2 + 1) == gapIndex
            }
        }

        private fun SectionEditFragment.pickDuration() {
            val timePickerFragment = TimePickerFragment()
            timePickerFragment.setMinutes(this.durationMinutes)
            timePickerFragment.setSeconds(this.durationSeconds)
            timePickerFragment.setOnOkListener(
                Runnable {
                    setDurationMinutes(timePickerFragment.getMinutes())
                    setDurationSeconds(timePickerFragment.getSeconds())
                },
            )
            timePickerFragment.show(parentFragmentManager, "timePicker")
        }

        private fun SectionEditFragment.updateDurationView() {
            _binding?.let { b ->
                b.time.text =
                    String.format(Locale.getDefault(), "%02d:%02d", durationMinutes, durationSeconds)
            }
        }

        private fun SectionEditFragment.fillBellList() {
            this.gongListAdapter =
                GongListAdapter(requireContext(), R.id.selectGongSound, R.id.spinnerText1)
            val bellList = BellCollection.getBellList()
            val adapter = this.gongListAdapter ?: return
            for (i in bellList.indices) {
                adapter.add(bellList[i])
            }
            binding.selectGongSound.adapter = adapter as SpinnerAdapter
        }

        private fun SectionEditFragment.selectBell(str: String?) {
            val bellList = BellCollection.getBellList()
            for (i in bellList.indices) {
                if (bellList[i].uri.toString() == str) {
                    binding.selectGongSound.setSelection(i)
                }
            }
        }
    }
}
