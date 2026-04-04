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
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import de.gaffga.android.zazentimer.Bell;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.audio.Audio;
import de.gaffga.android.zazentimer.audio.BellCollection;
import de.gaffga.android.zazentimer.bo.Section;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class SectionEditFragment extends Fragment {
    public static int INTENT_GET_BELL = 99;
    private static final String TAG = "ZMT_SectionEdit";
    private Audio audio;
    private int durationMinutes;
    private int durationSeconds;
    private GongListAdapter gongListAdapter;
    private SharedPreferences pref;
    private Section section;
    private int sectionId;
    private EditText etName = null;
    private SeekBar volume = null;
    private Spinner gongSelect = null;
    private ViewGroup lDuration = null;
    private ImageView ivBellCount1 = null;
    private ImageView ivBellCount2 = null;
    private ImageView ivBellCount3 = null;
    private ImageView ivBellCount4 = null;
    private ImageView ivBellCount5 = null;
    private TextView tvTime = null;
    private TextView[] tvGaps = new TextView[15];
    private ImageButton butAddCustomBell = null;
    private ImageButton butPlayGong = null;
    private HorizontalScrollView svBellGap = null;

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
        View inflate = layoutInflater.inflate(R.layout.fragment_edit_section, viewGroup, false);
        this.pref = ZazenTimerActivity.getPreferences(getActivity());
        DbOperations.init(getActivity());
        getViewComponents(inflate);
        return inflate;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.section = DbOperations.readSection(this.sectionId);
        this.audio = new Audio(getActivity());
        getActivity().invalidateOptionsMenu();
        fillViewFromData();
        installListeners();
        this.svBellGap.post(new Runnable() {
            @Override
            public void run() {
                TextView textView = SectionEditFragment.this.tvGaps[SectionEditFragment.this.section.bellpause - 1];
                Rect rect = new Rect();
                textView.getDrawingRect(rect);
                SectionEditFragment.this.svBellGap.requestChildRectangleOnScreen(textView, rect, true);
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
        Bell bell = (Bell) this.gongSelect.getSelectedItem();
        this.section.bellUri = bell.getUri().toString();
        this.section.volume = this.volume.getProgress();
        this.section.name = this.etName.getText().toString();
        this.section.duration = (this.durationMinutes * 60) + this.durationSeconds;
    }

    private void fillViewFromData() {
        setViewBellCount(this.section.bellcount);
        setViewGap(this.section.bellpause);
        this.volume.setMax(100);
        this.volume.setProgress(this.section.volume);
        this.etName.setText(this.section.name);
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
        this.gongSelect.setAdapter((SpinnerAdapter) this.gongListAdapter);
    }

    private void selectBell(String str) {
        ArrayList<Bell> bellList = BellCollection.getInstance().getBellList();
        for (int i = 0; i < bellList.size(); i++) {
            if (bellList.get(i).getUri().toString().equals(str)) {
                this.gongSelect.setSelection(i);
            }
        }
    }

    private void getViewComponents(View view) {
        this.etName = (EditText) view.findViewById(R.id.section_name);
        this.volume = (SeekBar) view.findViewById(R.id.sectionGongVolume);
        this.gongSelect = (Spinner) view.findViewById(R.id.selectGongSound);
        this.lDuration = (ViewGroup) view.findViewById(R.id.duration);
        this.ivBellCount1 = (ImageView) view.findViewById(R.id.bellcount1);
        this.ivBellCount2 = (ImageView) view.findViewById(R.id.bellcount2);
        this.ivBellCount3 = (ImageView) view.findViewById(R.id.bellcount3);
        this.ivBellCount4 = (ImageView) view.findViewById(R.id.bellcount4);
        this.ivBellCount5 = (ImageView) view.findViewById(R.id.bellcount5);
        this.tvGaps[0] = (TextView) view.findViewById(R.id.gap1);
        this.tvGaps[1] = (TextView) view.findViewById(R.id.gap2);
        this.tvGaps[2] = (TextView) view.findViewById(R.id.gap3);
        this.tvGaps[3] = (TextView) view.findViewById(R.id.gap4);
        this.tvGaps[4] = (TextView) view.findViewById(R.id.gap5);
        this.tvGaps[5] = (TextView) view.findViewById(R.id.gap6);
        this.tvGaps[6] = (TextView) view.findViewById(R.id.gap7);
        this.tvGaps[7] = (TextView) view.findViewById(R.id.gap8);
        this.tvGaps[8] = (TextView) view.findViewById(R.id.gap9);
        this.tvGaps[9] = (TextView) view.findViewById(R.id.gap10);
        this.tvGaps[10] = (TextView) view.findViewById(R.id.gap11);
        this.tvGaps[11] = (TextView) view.findViewById(R.id.gap12);
        this.tvGaps[12] = (TextView) view.findViewById(R.id.gap13);
        this.tvGaps[13] = (TextView) view.findViewById(R.id.gap14);
        this.tvGaps[14] = (TextView) view.findViewById(R.id.gap15);
        this.butAddCustomBell = (ImageButton) view.findViewById(R.id.addcustombell);
        this.tvTime = (TextView) view.findViewById(R.id.time);
        this.butPlayGong = (ImageButton) view.findViewById(R.id.play_gong);
        this.svBellGap = (HorizontalScrollView) view.findViewById(R.id.bell_gap_scrollview);
    }

    protected void installListeners() {
        this.butAddCustomBell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setFlags(1);
                intent.setType("audio/*");
                SectionEditFragment.this.startActivityForResult(Intent.createChooser(intent, SectionEditFragment.this.getResources().getString(R.string.select_audio)), SectionEditFragment.INTENT_GET_BELL);
            }
        });
        this.ivBellCount1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 1;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        this.ivBellCount2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 2;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        this.ivBellCount3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 3;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        this.ivBellCount4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.section.bellcount = 4;
                SectionEditFragment.this.setViewBellCount(SectionEditFragment.this.section.bellcount);
            }
        });
        this.ivBellCount5.setOnClickListener(new View.OnClickListener() {
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
        this.lDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionEditFragment.this.pickDuration();
            }
        });
        this.butPlayGong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bell bellForSection = BellCollection.getInstance().getBellForSection(SectionEditFragment.this.section);
                if (bellForSection != null) {
                    SectionEditFragment.this.audio.playAbsVolume(bellForSection, SectionEditFragment.this.section.volume);
                }
            }
        });
        this.gongSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        this.volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i2, boolean z) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SectionEditFragment.this.section.volume = SectionEditFragment.this.volume.getProgress();
                if (BellCollection.getInstance().getBellForSection(SectionEditFragment.this.section) != null) {
                    SectionEditFragment.this.audio.playAbsVolume(SectionEditFragment.this.section);
                }
            }
        });
    }

    private void setViewBellCount(int i) {
        this.ivBellCount1.setSelected(i >= 1);
        this.ivBellCount2.setSelected(i >= 2);
        this.ivBellCount3.setSelected(i >= 3);
        this.ivBellCount4.setSelected(i >= 4);
        this.ivBellCount5.setSelected(i >= 5);
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
        this.tvTime.setText(String.format("%02d", Integer.valueOf(this.durationMinutes)) + ":" + String.format("%02d", Integer.valueOf(this.durationSeconds)));
    }

    public void setSectionId(int i) {
        this.sectionId = i;
    }
}
