package de.gaffga.android.fragments;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.MessageView;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import de.gaffga.android.zazentimer.databinding.FragmentEditSessionBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialSharedAxis;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

@AndroidEntryPoint
public class SessionEditFragment extends Fragment {
    private static final String TAG = "ZMT_SessionEditFragment";
    private FragmentEditSessionBinding binding;
    private MessageView messageView;
    private SharedPreferences pref;
    private Section[] sections;
    private Session session = null;
    private int sessionId;
    private SectionListAdapter adapter;

    @Inject DbOperations dbOperations;

    private void handleAttach(Context context) {
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setHasOptionsMenu(true);
        if (bundle != null) {
            this.sessionId = bundle.getInt("sessionId");
        } else if (getArguments() != null) {
            this.sessionId = getArguments().getInt("sessionId");
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

        adapter = new SectionListAdapter(new SectionListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Section section) {
                SessionEditFragment.this.navigateToSectionEdit(section.id);
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
                    dbOperations.deleteSection(deletedSection.id);
                    adapter.removeItem(position);

                    Snackbar.make(binding.list, "Deleted '" + deletedSection.toString() + "'", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dbOperations.insertSection(SessionEditFragment.this.session, deletedSection);
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
        this.session = dbOperations.readSession(this.sessionId);
        if (this.session == null) {
            Log.e(TAG, "session is NULL");
        } else {
            Log.i(TAG, "session found and valid");
        }
        binding.textSitzungName.setText(this.session.name);
        binding.textSitzungBeschreibung.setText(this.session.description);
        getActivity().invalidateOptionsMenu();
        this.sections = dbOperations.readSections(this.session.id);
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
            dbOperations.updateSection(section);
        }
        this.session.name = binding.textSitzungName.getText().toString();
        this.session.description = binding.textSitzungBeschreibung.getText().toString();
        dbOperations.updateSession(this.session);
    }

    public void doCreateNewSection() {
        Section section = new Section(getResources().getString(R.string.default_section_name), 60);
        dbOperations.insertSection(this.session, section);
        navigateToSectionEdit(section.id);
    }

    private void navigateToSectionEdit(int sectionId) {
        Bundle args = new Bundle();
        args.putInt("sectionId", sectionId);
        Navigation.findNavController(getView()).navigate(R.id.action_sessionEditFragment_to_sectionEditFragment, args);
    }
}
