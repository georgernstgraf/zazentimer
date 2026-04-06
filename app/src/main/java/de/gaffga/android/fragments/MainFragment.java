package de.gaffga.android.fragments;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import de.gaffga.android.base.SpinnerUtil;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
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
        this.sessionListAdapter = new SessionListAdapter(new SessionListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, SessionWithTimeInfo session) {
                Session s = sessions.get(position);
                MainFragment.this.selectedSessionId = s.id;
                MainFragment.this.pref.edit().putInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, s.id).apply();
            }
        });
        this.recyclerSessions.setAdapter(this.sessionListAdapter);
        return inflate;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach (Activity)");
        handleAttach(activity);
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
