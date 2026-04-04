package de.gaffga.android.fragments;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.MessageView;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import de.gaffga.betterlist.BetterListView;
import de.gaffga.betterlist.IBetterListElementHandler;

/* loaded from: classes.dex */
public class SessionEditFragment extends androidx.fragment.app.Fragment implements BetterListView.BetterListListener<Section> {
    private static final String TAG = "ZMT_SessionEditFragment";
    private MessageView messageView;
    private SharedPreferences pref;
    private SectionEditFragment sectionEditFragment;
    private Section[] sections;
    private Session session = null;
    private int sessionId;
    private BetterListView<Section> vblvList;
    private EditText vetBeschreibung;
    private EditText vetName;
    private FloatingActionButton vfabCreateSection;
    private TextView vtvEmpty;

    private void handleAttach(Context context) {
    }

    @Override // de.gaffga.betterlist.BetterListView.BetterListListener
    public void onReorder() {
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        if (bundle != null) {
            this.sessionId = bundle.getInt("sessionId");
        }
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_session_edit_help) {
            showHelp13();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // android.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.session_edit_menu, menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override // android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        handleAttach(context);
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        handleAttach(activity);
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.d(TAG, "onCreateView");
        View inflate = layoutInflater.inflate(R.layout.fragment_edit_session, viewGroup, false);
        this.pref = ZazenTimerActivity.getPreferences(getActivity());
        this.sectionEditFragment = new SectionEditFragment();
        DbOperations.init(getActivity());
        this.vtvEmpty = (TextView) inflate.findViewById(android.R.id.empty);
        this.vetName = (EditText) inflate.findViewById(R.id.text_sitzung_name);
        this.vetBeschreibung = (EditText) inflate.findViewById(R.id.text_sitzung_beschreibung);
        this.vfabCreateSection = (FloatingActionButton) inflate.findViewById(R.id.but_new_section);
        this.vblvList = (BetterListView) inflate.findViewById(R.id.list);
        this.vblvList.setListElementHandler(new SectionListHandler());
        this.vblvList.setListener(this);
        this.vfabCreateSection.setOnClickListener(new View.OnClickListener() { // from class: de.gaffga.android.fragments.SessionEditFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SessionEditFragment.this.doCreateNewSection();
            }
        });
        return inflate;
    }

    @Override // android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("sessionId", this.sessionId);
    }

    public void setSessionId(int i) {
        this.sessionId = i;
    }

    /* loaded from: classes.dex */
    private class SectionListHandler implements IBetterListElementHandler<Section> {
        @Override // de.gaffga.betterlist.IBetterListElementHandler
        public int getListItemResourceId() {
            return R.layout.session_list_item;
        }

        private SectionListHandler() {
        }

        @Override // de.gaffga.betterlist.IBetterListElementHandler
        public void setupView(View view, Section section) {
            String string;
            String str;
            if (section.name != null && section.name.length() > 0) {
                string = section.name;
            } else {
                string = SessionEditFragment.this.getResources().getString(R.string.unnamed);
            }
            String str2 = section.getDurationString() + ", ";
            if (section.bellcount == 1) {
                str = str2 + SessionEditFragment.this.getResources().getString(R.string.section_info_string_1_sg);
            } else {
                String str3 = (str2 + String.format(SessionEditFragment.this.getResources().getString(R.string.section_info_string_1_pl), Integer.valueOf(section.bellcount))) + " ";
                if (section.bellpause == 1) {
                    str = str3 + SessionEditFragment.this.getResources().getString(R.string.section_info_string_2_sg);
                } else {
                    str = str3 + String.format(SessionEditFragment.this.getResources().getString(R.string.section_info_string_2_pl), Integer.valueOf(section.bellpause));
                }
            }
            TextView textView = (TextView) view.findViewById(R.id.spinnerText1);
            TextView textView2 = (TextView) view.findViewById(R.id.spinnerText2);
            textView.setText(string);
            textView2.setText(str);
        }
    }

    private void initSectionList() {
        this.vblvList.clear();
        for (int i = 0; i < this.sections.length; i++) {
            this.vblvList.add(this.sections[i]);
        }
        this.vblvList.setEmptyView(this.vtvEmpty);
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        Log.d(TAG, "sessionId=" + this.sessionId);
        this.session = DbOperations.readSession(this.sessionId);
        if (this.session == null) {
            Log.e(TAG, "session is NULL");
        } else {
            Log.i(TAG, "session found and valid");
        }
        this.vetName.setText(this.session.name);
        this.vetBeschreibung.setText(this.session.description);
        getActivity().invalidateOptionsMenu();
        this.sections = DbOperations.readSections(this.session.id);
        initSectionList();
        this.vetName.setText(this.session.name);
        this.vetBeschreibung.setText(this.session.description);
        if (this.pref.getBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, false)) {
            showHelp13();
            this.pref.edit().putBoolean(ZazenTimerActivity.PREF_KEY_SHOW_SESSION_EDIT_HELP_V13, true).apply();
        }
    }

    private void showHelp13() {
        if (this.messageView != null) {
            return;
        }
        this.messageView = new MessageView(getActivity());
        this.messageView.setTitle(getString(R.string.help_sectionlist_title));
        this.messageView.setText(getString(R.string.help_sectionlist_text));
        this.messageView.setOnOkListener(new Runnable() { // from class: de.gaffga.android.fragments.SessionEditFragment.2
            @Override // java.lang.Runnable
            public void run() {
                SessionEditFragment.this.messageView = null;
            }
        });
        this.messageView.show();
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        int i = 0;
        while (i < this.vblvList.getCount()) {
            Section elementAt = this.vblvList.getElementAt(i);
            i++;
            elementAt.rank = i;
            DbOperations.updateSection(elementAt);
        }
        this.session.name = this.vetName.getText().toString();
        this.session.description = this.vetBeschreibung.getText().toString();
        DbOperations.updateSession(this.session);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doCreateNewSection() {
        Section section = new Section(getResources().getString(R.string.default_section_name), 60);
        DbOperations.insertSection(this.session, section);
        this.sectionEditFragment.setSectionId(section.id);
        showSectionEditFragment();
    }

    @Override // de.gaffga.betterlist.BetterListView.BetterListListener
    public void onDeleteItem(Section section) {
        DbOperations.deleteSection(section.id);
    }

    @Override // de.gaffga.betterlist.BetterListView.BetterListListener
    public void onUndoDelete(Section section) {
        DbOperations.insertSection(this.session, section);
    }

    @Override // de.gaffga.betterlist.BetterListView.BetterListListener
    public void onItemClick(Section section) {
        this.sectionEditFragment.setSectionId(section.id);
        showSectionEditFragment();
    }

    public void showSectionEditFragment() {
        androidx.fragment.app.FragmentTransaction beginTransaction = getParentFragmentManager().beginTransaction();
        beginTransaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out, R.animator.fade_in, R.animator.fade_out);
        beginTransaction.replace(R.id.content, this.sectionEditFragment, ZazenTimerActivity.FRAGMENT_SECTION_EDIT);
        beginTransaction.addToBackStack(null);
        beginTransaction.commit();
    }
}
