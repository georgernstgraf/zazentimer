package at.priv.graf.fragments;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import at.priv.graf.base.SpinnerUtil;
import com.google.android.material.transition.MaterialFadeThrough;
import at.priv.graf.zazentimer.DbOperations;
import at.priv.graf.zazentimer.R;
import at.priv.graf.zazentimer.ZazenTimerActivity;
import at.priv.graf.zazentimer.service.MeditationService;
import at.priv.graf.zazentimer.bo.Section;
import at.priv.graf.zazentimer.bo.Session;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainFragment extends Fragment {
    private static final String TAG = "ZMT_MainFragment";
    private Button butStart;
    private Context context;
    private RecyclerView recyclerSessions;
    private boolean mAttached;
    private OnFragmentInteractionListener mListener;
    private SharedPreferences pref;
    private SessionListAdapter sessionListAdapter;
    private ArrayList<Session> sessions = new ArrayList<>();
    private int selectedSessionId = -1;
    private boolean interactionsEnabled = true;

    @Inject DbOperations dbOperations;

    public interface OnFragmentInteractionListener {
        void onStartPressed();
    }

    public MainFragment() {
        Log.d(TAG, "Constructor");
        this.mAttached = false;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setEnterTransition(new MaterialFadeThrough());
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.d(TAG, "onCreateView");
        View inflate = layoutInflater.inflate(R.layout.fragment_main, viewGroup, false);
        this.butStart = (Button) inflate.findViewById(R.id.but_start);
        this.recyclerSessions = (RecyclerView) inflate.findViewById(R.id.recycler_sessions);
        this.butStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainFragment.this.mListener.onStartPressed();
            }
        });
        this.recyclerSessions.setLayoutManager(new LinearLayoutManager(this.context));
        this.sessionListAdapter = new SessionListAdapter(
            new SessionListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position, SessionWithTimeInfo session) {
                    if (!interactionsEnabled) return;
                    Session s = sessions.get(position);
                    MainFragment.this.selectedSessionId = s.id;
                    MainFragment.this.pref.edit().putInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, s.id).apply();
                }
            },
            new SessionListAdapter.OnSessionActionListener() {
                @Override
                public void onEditSession(int position) {
                    MainFragment.this.onCardEditSession(position);
                }

                @Override
                public void onCopySession(int position) {
                    MainFragment.this.onCardCopySession(position);
                }

                @Override
                public void onDeleteSession(int position) {
                    MainFragment.this.onCardDeleteSession(position);
                }
            }
        );
        this.recyclerSessions.setAdapter(this.sessionListAdapter);
        this.recyclerSessions.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (recyclerSessions == null || butStart == null) return;
                    View parent = (View) recyclerSessions.getParent();
                    if (parent == null) return;
                    int available = parent.getHeight() - butStart.getHeight();
                    if (available <= 0) return;
                    int maxRecyclerHeight = (int) (available * 0.60);
                    MaxHeightRecyclerView rv = (MaxHeightRecyclerView) recyclerSessions;
                    if (rv.getMaxHeight() != maxRecyclerHeight) {
                        rv.setMaxHeight(maxRecyclerHeight);
                    }
                }
            }
        );
        SessionTouchHelperCallback callback = new SessionTouchHelperCallback(
            new SessionTouchHelperCallback.SessionTouchListener() {
                @Override
                public boolean onMove(int fromPosition, int toPosition) {
                    if (fromPosition < 0 || fromPosition >= sessions.size()) return false;
                    if (toPosition < 0 || toPosition >= sessions.size()) return false;
                    Session moved = sessions.remove(fromPosition);
                    sessions.add(toPosition, moved);
                    sessionListAdapter.moveItem(fromPosition, toPosition);
                    return true;
                }
            }
        );
        new ItemTouchHelper(callback).attachToRecyclerView(this.recyclerSessions);
        return inflate;
    }

    public void onFabNewSessionClicked() {
        addNewSession();
    }

    private void addNewSession() {
        if (!interactionsEnabled) return;
        Session session = new Session();
        session.name = "";
        session.description = "";
        dbOperations.insertSession(session);
        updateSessionList();
        setSelectedSessionId(session.id);
        navigateToSessionEdit(session.id);
    }

    private void onCardEditSession(int position) {
        if (!interactionsEnabled) return;
        if (position < 0 || position >= sessions.size()) return;
        Session s = sessions.get(position);
        navigateToSessionEdit(s.id);
    }

    private void onCardCopySession(int position) {
        if (!interactionsEnabled) return;
        if (position < 0 || position >= sessions.size()) return;
        Session s = sessions.get(position);
        int newId = dbOperations.duplicateSession(s.id,
            getString(R.string.copy_prefix) + " " + s.name);
        updateSessionList();
        setSelectedSessionId(newId);
    }

    private void onCardDeleteSession(int position) {
        if (!interactionsEnabled) return;
        if (position < 0 || position >= sessions.size()) return;
        Session s = sessions.get(position);
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_question_delete_session)
            .setMessage(R.string.text_question_delete_session)
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                dbOperations.deleteSession(s.id);
                updateSessionList();
                selectLastSession();
            })
            .setNegativeButton(R.string.abbrechen, (dialog, which) -> {})
            .create()
            .show();
    }

    private void navigateToSessionEdit(int sessionId) {
        Bundle args = new Bundle();
        args.putInt("sessionId", sessionId);
        Navigation.findNavController(getView()).navigate(
            R.id.action_mainFragment_to_sessionEditFragment, args);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach (Context)");
        handleAttach(context);
    }

    private void handleAttach(Context context) {
        if (context == null) {
            return;
        }
        this.mAttached = true;
        this.pref = ZazenTimerActivity.getPreferences(context);
        this.context = context;
        if (context instanceof OnFragmentInteractionListener) {
            this.mListener = (OnFragmentInteractionListener) context;
            return;
        }
        throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        this.mListener = null;
        this.mAttached = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        interactionsEnabled = !MeditationService.isServiceRunning();
        updateSessionInteractions();
        Log.d(TAG, "onResume");
        getActivity().invalidateOptionsMenu();
        updateSessionList();
        int i = this.pref.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1);
        if (i == -1) {
            return;
        }
        int positionById = SpinnerUtil.getPositionById(this.sessions, i);
        if (positionById == -1) {
            return;
        }
        Log.d(TAG, "LAST_SELECTED_SESSION was idx=" + positionById + " id=" + i);
        this.selectedSessionId = i;
        this.sessionListAdapter.setSelectedPosition(positionById);
    }

    private void updateSessionInteractions() {
        if (butStart != null) {
            butStart.setEnabled(interactionsEnabled);
            butStart.setAlpha(interactionsEnabled ? 1.0f : 0.4f);
        }
        if (sessionListAdapter != null) {
            sessionListAdapter.setInteractionsEnabled(interactionsEnabled);
        }
    }

    public void updateSessionList() {
        Session[] readSessions = dbOperations.readSessions();
        ArrayList<SessionWithTimeInfo> arrayList = new ArrayList<>();
        this.sessions.clear();
        for (Session session : readSessions) {
            int i = 0;
            for (Section section : dbOperations.readSections(session.id)) {
                i += section.duration;
            }
            arrayList.add(new SessionWithTimeInfo(session, i));
            this.sessions.add(session);
        }
        this.sessionListAdapter.setSessions(arrayList);
    }

    public void selectLastSession() {
        if (this.sessions.size() == 0) {
            setSelectedSessionId(-1);
        } else {
            setSelectedSessionId(this.sessions.get(this.sessions.size() - 1).id);
        }
    }

    public int getSelectedSessionId() {
        return this.selectedSessionId;
    }

    public void setSelectedSessionId(int i) {
        this.selectedSessionId = i;
        this.sessionListAdapter.setSelectedPosition(SpinnerUtil.getPositionById(this.sessions, i));
    }
}
