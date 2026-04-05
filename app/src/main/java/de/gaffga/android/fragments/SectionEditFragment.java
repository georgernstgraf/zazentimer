package de.gaffga.android.fragments;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import de.gaffga.android.zazentimer.Bell;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.audio.Audio;
import de.gaffga.android.zazentimer.audio.BellCollection;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.databinding.FragmentEditSectionBinding;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class SectionEditFragment extends Fragment {
    public static int INTENT_GET_BELL = 99;
    private static final String TAG = "ZMT_SectionEdit";
    private Audio audio;
    private FragmentEditSectionBinding binding;
    private int durationMinutes;
    private int durationSeconds;
    private GongListAdapter gongListAdapter;
    private SharedPreferences pref;
    private Section section;
    private int sectionId;
    private TextView tvGaps[] = new TextView[15];

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.sectionId = bundle.getInt("sectionId");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("sectionId", this.sectionId);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.d(TAG, "onCreateView");
        binding = FragmentEditSectionBinding.inflate(layoutInflater, viewGroup, false);
        this.pref = ZazenTimerActivity.getPreferences(getActivity());
        DbOperations.init(getActivity());
        getViewComponents();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.section = DbOperations.readSection(this.sectionId);
        this.audio = new Audio(getActivity());
        getActivity().invalidateOptionsMenu();
        fillViewFromData();
        installListeners();
        binding.bellGapScrollview.post(new Runnable() {
            @Override
            public void run() {
                TextView textView = SectionEditFragment.this.tvGaps[SectionEditFragment.this.section.bellpause - 1];
                Rect rect = new Rect();
                textView.getDrawingRect(rect);
                SectionEditFragment.this.binding.bellGapScrollview.requestChildRectangleOnScreen(textView, rect, true);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        this.audio.release();
        this.audio = null;
        fillDataFromViews();
        DbOperations.updateSection(this.section);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i2 == -1 && i == INTENT_GET_BELL) {
            Uri data = intent.getData();
            String str = "bell_unnamed";
            if (data.getScheme().equals("content")) {
                Cursor query = getActivity().getContentResolver().query(data, null, null, null, null);
                if (query != null && query.getCount() != 0) {
                    int columnIndex = query.getColumnIndex("_display_name");
                    if (columnIndex >= 0) {
                        query.moveToFirst();
                        str = "bell_" + query.getString(columnIndex);
                    } else {
                        str = "bell_" + data.getLastPathSegment();
                    }
                }
                if (query != null) {
                    query.close();
                }
            } else {
                str = "bell_" + data.getLastPathSegment();
            }
            try {
                InputStream openInputStream = getActivity().getContentResolver().openInputStream(data);
                FileOutputStream openFileOutput = getActivity().openFileOutput(str, 0);
                byte[] bArr = new byte[8192];
                for (int read = openInputStream.read(bArr); read > 0; read = openInputStream.read(bArr)) {
                    openFileOutput.write(bArr, 0, read);
                }
                openInputStream.close();
                openFileOutput.close();
                BellCollection.getInstance().init(getActivity());
                fillBellList();
                this.section.bellUri = BellCollection.getInstance().getUriForName(str).toString();
                selectBell(this.section.bellUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fillDataFromViews() {
        BellCollection.getInstance();
        this.section.bell = -2;
        Bell bell = (Bell) binding.selectGongSound.getSelectedItem();
        this.section.bellUri = bell.getUri().toString();
        this.section.volume = binding.sectionGongVolume.getProgress();
        this.section.name = binding.sectionName.getText().toString();
        this.section.duration = (this.durationMinutes * 60) + this.durationSeconds;
    }

    private void fillViewFromData() {
        setViewBellCount(this.section.bellcount);
        setViewGap(this.section.bellpause);
        binding.sectionGongVolume.setMax(100);
        binding.sectionGongVolume.setProgress(this.section.volume);
        binding.sectionName.setText(this.section.name);
        setDurationMinutes(this.section.duration / 60);
        setDurationSeconds(this.section.duration % 60);
        fillBellList();
        selectBell(this.section.bellUri);
    }

    private void fillBellList() {
        this.gongListAdapter = new GongListAdapter(getActivity(), R.id.selectGongSound, R.id.spinnerText1);
        ArrayList<Bell> bellList = BellCollection.getInstance().getBellList();
        for (int i = 0; i < bellList.size(); i++) {
            this.gongListAdapter.add(bellList.get(i));
        }
        binding.selectGongSound.setAdapter((SpinnerAdapter) this.gongListAdapter);
    }

    private void selectBell(String str) {
        ArrayList<Bell> bellList = BellCollection.getInstance().getBellList();
        for (int i = 0; i < bellList.size(); i++) {
            if (bellList.get(i).getUri().toString().equals(str)) {
                binding.selectGongSound.setSelection(i);
            }
        }
    }

    private void getViewComponents() {
        this.tvGaps[0] = binding.gap1;
        this.tvGaps[1] = binding.gap2;
        this.tvGaps[2] = binding.gap3;
        this.tvGaps[3] = binding.gap4;
        this.tvGaps[4] = binding.gap5;
        this.tvGaps[5] = binding.gap6;
        this.tvGaps[6] = binding.gap7;
        this.tvGaps[7] = binding.gap8;
        this.tvGaps[8] = binding.gap9;
        this.tvGaps[9] = binding.gap10;
        this.tvGaps[10] = binding.gap11;
        this.tvGaps[11] = binding.gap12;
        this.tvGaps[12] = binding.gap13;
        this.tvGaps[13] = binding.gap14;
        this.tvGaps[14] = binding.gap15;
    }

    protected void installListeners() {
        binding.addcustombell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setFlags(1);
                intent.setType("audio/*");
                SectionEditFragment.this.startActivityForResult(Intent.createChooser(intent, SectionEditFragment.this.getResources().getString(R.string.select_audio)), SectionEditFragment.INTENT_GET_BELL);
            }
        });
        binding.bellcount1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 1;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        binding.bellcount2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 2;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        binding.bellcount3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 3;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        binding.bellcount4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 4;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        binding.bellcount5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 5;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        for (int i = 0; i < 15; i++) {
            this.tvGaps[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int i2 = 0;
                    while (true) {
                        if (i2 >= 15) {
                            break;
                        }
                        if (view.getId() == SectionEditFragment.this.tvGaps[i2].getId()) {
                            SectionEditFragment.this.section.bellpause = i2 + 1;
                            break;
                        }
                        i2++;
                    }
                    SectionEditFragment.this.setViewGap(SectionEditFragment.this.section.bellpause);
                }
            });
        }
        binding.duration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.pickDuration();
            }
        });
        binding.playGong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bell bellForSection = BellCollection.getInstance().getBellForSection(SectionEditFragment.this.section);
                if (bellForSection != null) {
                    SectionEditFragment.this.audio.playAbsVolume(bellForSection, SectionEditFragment.this.section.volume);
                }
            }
        });
        binding.selectGongSound.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i2, long j) {
                Bell bell = BellCollection.getInstance().getBell(i2);
                if (bell.getUri().toString().equals(SectionEditFragment.this.section.bellUri)) {
                    return;
                }
                SectionEditFragment.this.section.bellUri = bell.getUri().toString();
                SectionEditFragment.this.section.bell = -2;
            }
        });
        binding.sectionGongVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i2, boolean z) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SectionEditFragment.this.section.volume = SectionEditFragment.this.binding.sectionGongVolume.getProgress();
                if (BellCollection.getInstance().getBellForSection(SectionEditFragment.this.section) != null) {
                    SectionEditFragment.this.audio.playAbsVolume(SectionEditFragment.this.section);
                }
            }
        });
    }

    private void setViewBellCount(int i) {
        binding.bellcount1.setSelected(i >= 1);
        binding.bellcount2.setSelected(i >= 2);
        binding.bellcount3.setSelected(i >= 3);
        binding.bellcount4.setSelected(i >= 4);
        binding.bellcount5.setSelected(i >= 5);
    }

    private void setViewGap(int i) {
        int i2 = 0;
        while (i2 < 15) {
            int i3 = i2 + 1;
            this.tvGaps[i2].setSelected(i3 == i);
            i2 = i3;
        }
    }

    private void pickDuration() {
        final TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setMinutes(this.durationMinutes);
        timePickerFragment.setSeconds(this.durationSeconds);
        timePickerFragment.setOnOkListener(new Runnable() {
            @Override
            public void run() {
                SectionEditFragment.this.setDurationMinutes(timePickerFragment.getMinutes());
                SectionEditFragment.this.setDurationSeconds(timePickerFragment.getSeconds());
            }
        });
        timePickerFragment.show(getParentFragmentManager(), "timePicker");
    }

    public void setDurationSeconds(int i) {
        this.durationSeconds = i;
        updateDurationView();
    }

    public void setDurationMinutes(int i) {
        this.durationMinutes = i;
        updateDurationView();
    }

    private void updateDurationView() {
        binding.time.setText(String.format("%02d", Integer.valueOf(this.durationMinutes)) + ":" + String.format("%02d", Integer.valueOf(this.durationSeconds)));
    }

    public void setSectionId(int i) {
        this.sectionId = i;
    }
}
