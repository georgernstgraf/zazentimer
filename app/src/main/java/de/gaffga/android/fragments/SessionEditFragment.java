package de.gaffga.android.fragments;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.MessageView;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import de.gaffga.android.zazentimer.databinding.FragmentEditSessionBinding;
import java.util.Arrays;
import java.util.List;

public class SessionEditFragment extends Fragment {
    private static final String TAG = "ZMT_SessionEditFragment";
    private FragmentEditSessionBinding binding;
    private MessageView messageView;
    private SharedPreferences pref;
    private SectionEditFragment sectionEditFragment;
    private Section[] sections;
    private Session session = null;
    private int sessionId;
    private SectionListAdapter adapter;

    private void handleAttach(Context context) {
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        if (bundle != null) {
            this.sessionId = bundle.getInt("sessionId");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_session_edit_help) {
            showHelp13();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.session_edit_menu, menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handleAttach(context);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        handleAttach(activity);
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.d(TAG, "onCreateView");
        binding = FragmentEditSessionBinding.inflate(layoutInflater, viewGroup, false);
        this.pref = ZazenTimerActivity.getPreferences(getActivity());
        this.sectionEditFragment = new SectionEditFragment();
        DbOperations.init(getActivity());

        adapter = new SectionListAdapter(new SectionListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Section section) {
                SessionEditFragment.this.sectionEditFragment.setSectionId(section.id);
                SessionEditFragment.this.showSectionEditFragment();
            }
        });

        binding.list.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.list.setAdapter(adapter);

        SectionTouchHelperCallback callback = new SectionTouchHelperCallback(
            new SectionTouchHelperCallback.SectionTouchListener() {
                @Override
                public void onSwipe(int position) {
                    final Section deletedSection = adapter.getItem(position);
                    final int deletedPosition = position;
                    DbOperations.deleteSection(deletedSection.id);
                    adapter.removeItem(position);

                    Snackbar.make(binding.list, "Deleted '" + deletedSection.toString() + "'", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                DbOperations.insertSection(SessionEditFragment.this.session, deletedSection);
                                adapter.insertItem(deletedPosition, deletedSection);
                            }
                        })
                        .show();
                }

                @Override
                public boolean onMove(int fromPosition, int toPosition) {
                    adapter.moveItem(fromPosition, toPosition);
                    return true;
                }
            }
        );
        new ItemTouchHelper(callback).attachToRecyclerView(binding.list);

        binding.butNewSection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SessionEditFragment.this.doCreateNewSection();
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("sessionId", this.sessionId);
    }

    public void setSessionId(int i) {
        this.sessionId = i;
    }

    private void initSectionList() {
        adapter.setItems(Arrays.asList(this.sections));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "sessionId=" + this.sessionId);
        this.session = DbOperations.readSession(this.sessionId);
        if (this.session == null) {
            Log.e(TAG, "session is NULL");
        } else {
            Log.i(TAG, "session found and valid");
        }
        binding.textSitzungName.setText(this.session.name);
        binding.textSitzungBeschreibung.setText(this.session.description);
        getActivity().invalidateOptionsMenu();
        this.sections = DbOperations.readSections(this.session.id);
        initSectionList();
        binding.textSitzungName.setText(this.session.name);
        binding.textSitzungBeschreibung.setText(this.session.description);
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
        this.messageView.setOnOkListener(new Runnable() {
            @Override
            public void run() {
                SessionEditFragment.this.messageView = null;
            }
        });
        this.messageView.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        List<Section> items = adapter.getItems();
        for (int i = 0; i < items.size(); i++) {
            Section section = items.get(i);
            section.rank = i + 1;
            DbOperations.updateSection(section);
        }
        this.session.name = binding.textSitzungName.getText().toString();
        this.session.description = binding.textSitzungBeschreibung.getText().toString();
        DbOperations.updateSession(this.session);
    }

    public void doCreateNewSection() {
        Section section = new Section(getResources().getString(R.string.default_section_name), 60);
        DbOperations.insertSection(this.session, section);
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
