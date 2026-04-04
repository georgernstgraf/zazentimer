package de.gaffga.android.fragments;

import androidx.fragment.app.Fragment;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import de.gaffga.android.base.SpinnerUtil;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import java.util.ArrayList;

public class MainFragment extends androidx.fragment.app.Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "ZMT_MainFragment";
    private Button butStart;
    private Context context;
    private Spinner listSessions;
    private boolean mAttached;
    private OnFragmentInteractionListener mListener;
    private SharedPreferences pref;
    private SessionListAdapter sessionListAdapter;
    private ArrayList<Session> sessions = new ArrayList<>();
    private int selectedSessionId = -1;

    public interface OnFragmentInteractionListener {
        void onStartPressed();
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public MainFragment() {
        Log.d(TAG, "Constructor");
        this.mAttached = false;
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG, "onCreate");
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.d(TAG, "onCreateView");
        View inflate = layoutInflater.inflate(R.layout.fragment_main, viewGroup, false);
        this.butStart = (Button) inflate.findViewById(R.id.but_start);
        this.listSessions = (Spinner) inflate.findViewById(R.id.spin_sessions);
        this.butStart.setOnClickListener(new View.OnClickListener() { // from class: de.gaffga.android.fragments.MainFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainFragment.this.mListener.onStartPressed();
            }
        });
        this.sessionListAdapter = new SessionListAdapter(this.context, R.layout.main_session_list_item, R.id.spinnerText1);
        this.listSessions.setAdapter((SpinnerAdapter) this.sessionListAdapter);
        this.listSessions.setOnItemSelectedListener(this);
        return inflate;
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach (Activity)");
        handleAttach(activity);
    }

    @Override // android.app.Fragment
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

    @Override // android.app.Fragment
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        this.mListener = null;
        this.mAttached = false;
    }

    @Override // android.app.Fragment
    public void onResume() {
        int positionById;
        super.onResume();
        Log.d(TAG, "onResume");
        getActivity().invalidateOptionsMenu();
        updateSessionList();
        int i = this.pref.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1);
        if (i == -1 || (positionById = SpinnerUtil.getPositionById(this.sessions, i)) == -1) {
            return;
        }
        Log.d(TAG, "LAST_SELECTED_SESSION was idx=" + positionById + " id=" + i);
        this.selectedSessionId = i;
        this.listSessions.setSelection(positionById);
    }

    public void updateSessionList() {
        Session[] readSessions = DbOperations.readSessions();
        ArrayList<SessionWithTimeInfo> arrayList = new ArrayList<>();
        this.sessions.clear();
        for (Session session : readSessions) {
            int i = 0;
            for (Section section : DbOperations.readSections(session.id)) {
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
        this.listSessions.setSelection(SpinnerUtil.getPositionById(this.sessions, i));
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        Session session = this.sessions.get(i);
        this.selectedSessionId = session.id;
        this.pref.edit().putInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, session.id).apply();
    }
}
