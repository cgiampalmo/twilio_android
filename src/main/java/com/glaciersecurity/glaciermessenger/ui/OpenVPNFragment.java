package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.glaciersecurity.glaciercore.api.APIVpnProfile;
import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciercore.api.IOpenVPNStatusCallback;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.ui.adapter.ProfileSelectListAdapter;
import com.glaciersecurity.glaciermessenger.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static android.view.View.VISIBLE;

public class OpenVPNFragment extends Fragment implements View.OnClickListener, Handler.Callback{

    final static String EMERGENCY_PROFILE_TAG = "emerg";
    static final int PROFILE_DIALOG_REQUEST_CODE = 8;
    static final String PROFILE_SELECTED = "PROFILE_SELECTED";
    static final String USE_CORE_CONNECT = "use_core_connect";


    private ConnectivityReceiver connectivityReceiver; //CMG AM-41

    private TextView mMyIp;
    private TextView mStatus;
    private TextView mVpnConnectionStatus;
    private RelativeLayout mVpnStatusBar;
    private TextView mProfile;
    private CheckBox enableEmergConnectCheckBox;
    private GlacierProfile emergencyProfile;
    private Switch mUseVpnToggle;
    private Button mDisconnectVpn;
    private TextView mCoreLink;
    private LinearLayout mDisableVpnView;
    private LinearLayout mNoVpnProfilesView;
    private ListView profileSpinner;
    private ProfileSelectListAdapter<GlacierProfile> spinnerAdapter;

    // variables used for random profile retries upon failure
    private boolean randomProfileSelected = false;
    private List<String> excludeProfileList = new ArrayList<String>();
    private OpenVPNActivity activity;

    //TODO getConnectedProf

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OpenVPNActivity) {
            this.activity = (OpenVPNActivity) activity;

        } else {
            throw new IllegalStateException("Trying to attach fragment to activity that is not the ConversationsActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.openvpn_fragment, container, false);
        /* GOOBER CORE - Remove emergency profile enableEmergConnectCheckBox = (CheckBox) v.findViewById(R.id.enable_emergconnect);
        enableEmergConnectCheckBox.setChecked(false);
        enableEmergConnectCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Log.d("GOOBER", "CheckChange: " + isChecked);

                // make sure we have an emergency profile
                if (emergencyProfile != null) {
                    if (isChecked) {
                        if (spinnerAdapter.getPosition(emergencyProfile) < 0) {
                            spinnerAdapter.add(emergencyProfile);
                            spinnerAdapter.notifyDataSetChanged();
                        }
                    } else {
                        spinnerAdapter.remove(emergencyProfile);
                        spinnerAdapter.notifyDataSetChanged();
                    }
                } else {
                    displayNoEmergencyProfile();
                    enableEmergConnectCheckBox.setChecked(false);
                }
            }
        }); */
        v.findViewById(R.id.addNewProfile).setOnClickListener(this);
        //mHelloWorld = (TextView) v.findViewById(R.id.helloworld);
        mStatus = (TextView) v.findViewById(R.id.status);
        mProfile = (TextView) v.findViewById(R.id.currentProfile);
        mVpnConnectionStatus = (TextView) v.findViewById(R.id.vpn_connection_status);
        mVpnStatusBar = (RelativeLayout) v.findViewById(R.id.vpn_status);
        mDisableVpnView = (LinearLayout) v.findViewById(R.id.disabled_vpn_layout);
        mCoreLink = (TextView) v.findViewById(R.id.glacier_chat_core_link);
        mNoVpnProfilesView = (LinearLayout) v.findViewById(R.id.no_vpn_profiles_layout);
        mDisconnectVpn= (Button) v.findViewById(R.id.disconnet_button);
        v.findViewById(R.id.disconnet_button).setOnClickListener(mOnDisconnectListener);


        // mMyIp = (TextView) v.findViewById(R.id.MyIpText);
        v.findViewById(R.id.glacier_chat_core_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.core_link)));
                startActivity(intent);
            }
        });

        mProfile = (TextView) v.findViewById(R.id.currentProfile);
        mUseVpnToggle = (Switch) v.findViewById(R.id.use_vpn_status_toggle);
        v.findViewById(R.id.use_vpn_status_toggle).setOnClickListener(mOnToggleSwitchListener);


        // mMyIp = (TextView) v.findViewById(R.id.MyIpText);
        addItemsOnProfileSpinner(v);

//        //CMG AM-41
//        offlineLayout = (LinearLayout) v.findViewById(R.id.offline_layout);
//        networkStatus = (TextView) v.findViewById(R.id.network_status);
//        offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
//        checkNetworkStatus();
        //TODO
            mUseVpnToggle.setChecked(true);
            if(mUseVpnToggle.isChecked()){
                profileSpinner.setVisibility(View.VISIBLE);
                mVpnStatusBar.setVisibility(View.VISIBLE);
                mDisableVpnView.setVisibility(View.GONE);
           } else {
                mDisableVpnView.setVisibility(View.VISIBLE);
                profileSpinner.setVisibility(View.GONE);
                mVpnStatusBar.setVisibility(View.GONE);
            }

        return v;

    }

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_MYIP = 1;
    private static final int START_PROFILE_EMBEDDED = 2;
    private static final int START_PROFILE_BYUUID = 3;
    private static final int ICS_OPENVPN_PERMISSION = 7;
    private static final int PROFILE_ADD_NEW = 8;

    private View.OnClickListener mOnToggleSwitchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mUseVpnToggle.isChecked()){
                mVpnStatusBar.setVisibility(View.VISIBLE);
                mDisableVpnView.setVisibility(View.GONE);
                profileSpinner.setVisibility(View.VISIBLE);

            } else {
                disconnectVpn();
                mDisableVpnView.setVisibility(View.VISIBLE);
                profileSpinner.setVisibility(View.GONE);
                mVpnStatusBar.setVisibility(View.GONE);
            }
        }
    };

    private View.OnClickListener mOnDisconnectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mDisconnectVpn.setVisibility(View.GONE);
            disconnectVpn();

        }
    };



    private void disconnectVpn(){
        try {
            mService.disconnect();
        } catch (RemoteException e) {
            //CMG AM-240
            Log.d(Config.LOGTAG, "at mService.disconnect");
            e.printStackTrace();
        }
    }


    protected IOpenVPNAPIService mService=null;
    private Handler mHandler;

    private void startEmbeddedProfile(boolean addNew)
    {
        try {
            InputStream conf = getActivity().getAssets().open("dave-vpn.ovpn");
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String config="";
            String line;
            while(true) {
                line = br.readLine();
                if(line == null)
                    break;
                config += line + "\n";
            }
            br.readLine();

            if (addNew)
                mService.addNewVPNProfile("newDaveProfile", true, config);
            else
                mService.startVPN(config);
        } catch (IOException | RemoteException e) {
            //CMG AM-240
            Log.d("RemoteException", "at mService.startVpn");
            e.printStackTrace();
        }
    }

    /**
     * HONEYBADGER AM-76
     */
    private void doCoreErrorAction() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this.getContext());
        builder.setTitle(R.string.core_missing);
        builder.setMessage(R.string.glacier_core_install);
        builder.setPositiveButton(R.string.next, (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.glacier_core_https))); //ALF getString fix
                startActivity(intent);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        });
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }



    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(this);
        bindService();
    }


    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */

        @Override
        public void newStatus(String uuid, String state, String message, String level)
                throws RemoteException {
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
            msg.sendToTarget();

            // GOOBER - Retrieve name of uuid and set the profile text
            String profileName = getProfileName(uuid);
            if (profileName != null) {
                mProfile.setText(profileName);
            }
            if (uuid != null){
                profileSpinner.setSelection(getSpinnerIndex(uuid));
            }
            if (state != null){
                handleStatusMessage(state);
            }

            /*if (index >= 0) {

                Log.d("GOOBER", "Setting to current used profile");
                profileSpinner.setSelection(index);
                currentProfile = (GlacierProfile) profileSpinner.getSelectedItem();

                if (isEmergencyProfile(currentProfile.getName())) {
                    enableEmergConnectCheckBox.setChecked(true);
                }
            } else if (state.toLowerCase().contains("connected")) {
                displayProfileNoLongerExists();
                mService = null;
            }*/
        }
    };

    /**
     * GOOBER - Retrieve profile name based on uuid.  We first check
     * the spinner and then check the emergency node
     * @param uuid
     * @return
     */
    private String getProfileName(String uuid) {
        int index = getSpinnerIndex(uuid);

        if (index >= 0) {
            GlacierProfile gp = (GlacierProfile) profileSpinner.getItemAtPosition(index);
            //TODO CHECK
            profileSpinner.setItemChecked(index, true);
            return gp.getName();
 //           return gp.getName();
        } else if (uuid.compareTo(emergencyProfile.getUuid()) == 0) {
            return emergencyProfile.getName();
        }
        return null;
    }

    /**
     * GOOBER - Retrieve index in spinner for matching uuid.
     * Return -1 if nothing found
     */
    private int getSpinnerIndex(String uuid) {
        GlacierProfile tmpProfile = null;
        String tmpUuid = null;
        int i = 0;
        for (i = 0; i < profileSpinner.getAdapter().getCount(); i++) {
            // Log.d("GOOBER", "THIS IS IT: " + profileSpinner.getItemAtPosition(i) + "::" + profileSpinner.getAdapter().getCount());
            tmpProfile = (GlacierProfile) profileSpinner.getItemAtPosition(i);
            tmpUuid = tmpProfile.getUuid();

            //CMG AM-240 add null check
            if (tmpUuid != null){
                // compare lower cases
                if (uuid.toLowerCase().compareTo(tmpUuid.toLowerCase()) == 0) {
                    //TODO CHECK
                    if (profileSpinner != null  && profileSpinner.getChildCount()>0) {
                        View v = profileSpinner.getChildAt(i);
                        spinnerAdapter.select(v);
                        profileSpinner.setItemChecked(i, true);
                    }

                    break;
                }
            }
        }

        // cold not find uuid being used
        if (i == profileSpinner.getAdapter().getCount()) {
            return -1;
        }
        return i;
    }


    /**
     * GOOBER - Add items to spinner
     *
     * @param v
     */
    private void addItemsOnProfileSpinner(View v) {
        profileSpinner = (ListView) v.findViewById(R.id.profileSpinner);
        List<GlacierProfile> gp = new ArrayList<GlacierProfile>();

        spinnerAdapter = new ProfileSelectListAdapter<GlacierProfile>(this.getActivity(),R.layout.radio, gp);
        profileSpinner.setAdapter(spinnerAdapter);
        profileSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,final int position, long id) {
                //TODO CHECK
                spinnerAdapter.select(position, view, parent);
                GlacierProfile glacierProfile = (GlacierProfile) parent.getItemAtPosition(position);
                confirmProfileSelection(position, glacierProfile);
                //spinnerAdapter.notifyDataSetChanged();

            }

        });
        spinnerAdapter.notifyDataSetChanged();
    }


    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.

            mService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                // Request permission to use the API
                Intent i = mService.prepare(getActivity().getPackageName());
                if (i!=null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK,null);
                }

            } catch (RemoteException e) {
                doCoreErrorAction(); //HONEYBADGER AM-76
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };
    private String mStartUUID=null;

    private void bindService() {

        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("com.glaciersecurity.glaciercore");

        getActivity().bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void listVPNs() {

        List<GlacierProfile> nameList = new ArrayList<GlacierProfile>();

        try {
            List<APIVpnProfile> list = mService.getProfiles();
            String all="Profile List:\n";
            for(APIVpnProfile vp:list.subList(0, Math.min(5, list.size()))) {
                all = all + vp.mName + ":" + vp.mUUID + "\n";
            }

            if (list.size() > 5)
                all +="\n And some profiles....";

            // add rest of vpn profiles to list
            for (int j = 0; j < list.size();j++) {
                // do not add emergency profile yet
                if (!isEmergencyProfile(list.get(j).mName.toLowerCase())) {
                    nameList.add(new GlacierProfile(list.get(j).mName, list.get(j).mUUID));
                } else {
                    emergencyProfile = new GlacierProfile(list.get(j).mName, list.get(j).mUUID);
                }
            }
            Collections.sort(nameList, new Comparator<GlacierProfile>() {
                @Override
                public int compare(GlacierProfile glacierProfile, GlacierProfile t1) {
                    return glacierProfile.name.compareTo(t1.name);
                }
            });

            // create random "profile" if there's enough in the list to randomize
            // we have to account for emergency profile since list contains everything
            /* GOOBER CORE - Remove random profile
            if (((emergencyProfile != null) && (list.size() > 2)) || ((emergencyProfile == null) && (list.size() > 1))) {
                // create random profile
                nameList.add(0, new GlacierProfile("RANDOM", "random"));
            }*/

            // add emergency profile to end and check the box as being enabled
            /* GOOBER CORE - Remove emergency profile
            if (enableEmergConnectCheckBox.isChecked()) {
                nameList.add(emergencyProfile);
            }*/


            spinnerAdapter = new ProfileSelectListAdapter<GlacierProfile>(this.getActivity(),R.layout.radio, nameList);
            //spinnerAdapter.setDropDownViewResource(R.layout.radio);
            profileSpinner.setAdapter(spinnerAdapter);
            spinnerAdapter.notifyDataSetChanged();
            spinnerAdapter.setOnItemClickListener(new ProfileSelectListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, GlacierProfile glacierProfile, int position) {
                    profileSpinner.setSelection(position);
                    profileSpinner.setItemChecked(position, true);
                    confirmProfileSelection(position, glacierProfile);
                    //TODO CHECK
                }

            });

            if(list.size()> 0) {
                mNoVpnProfilesView.setVisibility(View.GONE);

            } else {
                profileSpinner.setVisibility(View.GONE);
                mVpnStatusBar.setVisibility(View.GONE);
                mNoVpnProfilesView.setVisibility(View.VISIBLE);
            }

            /* GOOBER CORE - Remove emergency profile
            enableEmergConnectCheckBox.setChecked(false);
             */
            // mHelloWorld.setText(all);

        } catch (RemoteException e) {
            Log.d("RemoteException", "at listVpns");
            e.printStackTrace();
        }
    }

    private void confirmProfileSelection(int position, GlacierProfile glacierProfile){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.core_profile));
        builder.setMessage(getText(R.string.change_vpn_profile)+" "+ glacierProfile.getParcedName() +"?");
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setPositiveButton(getString(R.string.connect_button_label),
                (dialog, which) -> {
                    selectProfile(position, glacierProfile);
                });
        builder.create().show();
    }
    private void selectProfile(int position, GlacierProfile glacierProfile){
        //clearSelectedVpn();
        //selectedVpn(position);
        mDisconnectVpn.setVisibility(View.VISIBLE);
        startVpn(glacierProfile);
    }
    private void unbindService() {
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    private boolean isEmergencyProfile(String name) {
        if (name != null) {
            if (name.toLowerCase().contains(EMERGENCY_PROFILE_TAG))
                return true;
        }
        return false;
    }

    private void startVpn(GlacierProfile glacierProfile){
        mStartUUID = glacierProfile.getUuid();

        // GOOBER retrieve previous profile selected
        SharedPreferences sp = this.getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
        sp.edit().putString("last_spinner_profile", mStartUUID).commit();
        // Log.d("GOOBER", "This is uuid set: " + mStartUUID);

        // see if random profile selected
        if (mStartUUID.compareTo("random") == 0) {
            excludeProfileList.clear();
            randomProfileSelected = true;
            mStartUUID = getRandomUuid();
        }

        if (isEmergencyProfile(glacierProfile.getName())) {
            AlertDialog.Builder d = new AlertDialog.Builder(this.getActivity());

            d.setIconAttribute(android.R.attr.alertDialogIcon);
            d.setTitle("Emergency Profile");
            d.setMessage("This is an emergency profile.  Are you sure you want to continue?");
            d.setNegativeButton(getString(R.string.cancel), null);
            d.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        prepareStartProfile(START_PROFILE_BYUUID);
                    } catch (RemoteException e) {
                        //CMG AM-240
                        Log.d("RemoteException", "at prepareStartProfile");
                        e.printStackTrace();
                    }
                }
            });
            d.show();
        } else {
            try {
                prepareStartProfile(START_PROFILE_BYUUID);
            } catch (RemoteException e) {
                //CMG AM-240
                Log.d("RemoteException", "at prepareStartProfile");
                e.printStackTrace();
            }
        }
    }

    public String parseVpnName(String name){
        try {
            String [] splitname = name.split("_");
            return splitname[1];
        } catch (Exception e){
        }
        return name;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addNewProfile:
                // CMG AM-240
                try {
                    showImportProfileVPNDialogFragment();
                } catch (Exception e){
                    Log.d("Exception", "at showImportProfileVPNDialogFragment");
                    e.printStackTrace();
                }
            /* case R.id.getMyIP:
                Log.d("RemoteExample", "DAVID getMyIP");
                // Socket handling is not allowed on main thread
                new Thread() {

                    @Override
                    public void run() {
                        try {
                            String myip = getMyOwnIP();
                            Message msg = Message.obtain(mHandler,MSG_UPDATE_MYIP,myip);
                            msg.sendToTarget();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }.start();

                break;*/
                /** case R.id.startembedded:
                 Log.d("RemoteExample", "DAVID StartEmbedded");
                 try {
                 prepareStartProfile(START_PROFILE_EMBEDDED);
                 } catch (RemoteException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
                 }
                 break;

                 case R.id.addNewProfile:
                 Log.d("RemoteExample", "addNewProfile");
                 try {
                 prepareStartProfile(PROFILE_ADD_NEW);
                 } catch (RemoteException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
                 }*/
            default:
                break;
        }

    }

    /**
     * HONEYBADGER AM-76
     */
    public void launchPlayStoreCore(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(R.string.glacier_core_https)));
        startActivity(intent);
    }

    /**
     * Retreive random profile
     *
     * @return
     */
    private String getRandomUuid() {
        Random randomGenerator = new Random();

        // retrieve number of profiles
        int count = spinnerAdapter.getCount();
        int randomInt = -1;
        GlacierProfile glacierProfile = null;

        while (true) {

            //Log.d("GOOBER", "Count:  size: " + excludeProfileList.size() + "::" + count);
            if (enableEmergConnectCheckBox.isChecked() == true) {
                // do not include random profile in the beginning and the
                // emergency profile at the end.
                randomInt = randomGenerator.nextInt(count - 2);

                if (excludeProfileList.size() == (count - 2)) {
                    // Log.d("GOOBER", "Return null-1");
                    return null;
                }
            } else {
                // do not include the random profile in the beginning
                randomInt = randomGenerator.nextInt(count - 1);
                if (excludeProfileList.size() == (count-1)) {
                    //Log.d("GOOBER", "Return null-2");
                    return null;
                }
            }

            // get random profile, don't forget to skip the first one ("random")
            glacierProfile = (GlacierProfile) spinnerAdapter.getItem(randomInt + 1);

            // check if we're excluding
            if (!excludeProfileList.contains(glacierProfile.getUuid())) {
                //Log.d("GOOBER", "Found a profile " + excludeProfileList.size());
                excludeProfileList.add(glacierProfile.getUuid());
                return glacierProfile.getUuid();
            }
        }
    }

    private void prepareStartProfile(int requestCode) throws RemoteException {
        Intent requestpermission = mService.prepareVPNService();
        if(requestpermission == null) {
            onActivityResult(requestCode, Activity.RESULT_OK, null);
        } else {
            // Have to call an external Activity since services cannot used onActivityResult
            startActivityForResult(requestpermission, requestCode);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode==START_PROFILE_EMBEDDED)
                startEmbeddedProfile(false);
            if(requestCode==START_PROFILE_BYUUID)
                try {
                    if (mStartUUID!= null ||mStartUUID.isEmpty()) {
                        mService.startProfile(mStartUUID);
                    }
                } catch (RemoteException e) {
                    Log.d("RemoteException", "at start profile byuuid");
                    e.printStackTrace();
                }
            if (requestCode == ICS_OPENVPN_PERMISSION) {

                listVPNs();

                // GOOBER retrieve previous profile selected
                SharedPreferences sp = this.getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
                String lastSelectedProfile = sp.getString("last_spinner_profile", null);
                int spinnerIndex = 0;
                if (lastSelectedProfile != null) {
                    spinnerIndex = getSpinnerIndex(lastSelectedProfile);
                }
                profileSpinner.setSelection(spinnerIndex);

                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException | SecurityException e) { //ALF AM-194 added Security
                    Log.d(Config.LOGTAG, "RemoteException or Security Exception register status callback");
                    e.printStackTrace();
                }

            }
            if (requestCode == PROFILE_ADD_NEW) {
                startEmbeddedProfile(true);
            }
        }
    };

    String getMyOwnIP() throws UnknownHostException, IOException, RemoteException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        String resp="";
        Socket client = new Socket();
        // Setting Keep Alive forces creation of the underlying socket, otherwise getFD returns -1
        client.setKeepAlive(true);


        client.connect(new InetSocketAddress("v4address.com", 23),20000);
        client.shutdownOutput();
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        while (true) {
            String line = in.readLine();
            if( line == null)
                return resp;
            resp+=line;
        }

    }


    public void handleStatusMessage(String status){
        if (status.startsWith("NOPROCESS")) {
            mVpnConnectionStatus.setText("Not Connected");
            mDisconnectVpn.setVisibility(View.GONE);
            return;
        } else if (status.startsWith("CONNECTED")) {
            mVpnConnectionStatus.setText("Connected");
            mDisconnectVpn.setVisibility(VISIBLE);
            return;
        } else if ((status.startsWith("NONETWORK")) || (status.startsWith("AUTH_FAILED")) || (status.startsWith("EXITING"))) {
            mVpnConnectionStatus.setText("Connection failed");
            mDisconnectVpn.setVisibility(View.GONE);
            return;
        }
        else {
            mVpnConnectionStatus.setText("Configuring connection...");
            mDisconnectVpn.setVisibility(VISIBLE);
            return;
        }
    }
    @Override
    public boolean handleMessage(Message msg) {
        Log.d(Config.LOGTAG, "OpenVPNFragment::handleMessage(): " + msg.obj.toString() + "::What = " + msg.what);
        //TODO use message to update connection status
        //Log.d("GOOBER", "** UPDATED MESSAGE: " + ((CharSequence) msg.obj).subSequence(0, ((CharSequence) msg.obj).length() - 1) + "**" + msg.obj.toString());
        handleStatusMessage(msg.obj.toString());
        if(msg.what == MSG_UPDATE_STATE) {
            // GOOBER - check for NOPROCESS string and change it to NOT CONNECTED
            if (msg.obj.toString().startsWith("NOPROCESS")) {
                mStatus.setText("NOT CONNECTED");

                // DJF 08-27
                //if (profileSpinner == null) {
                //} else {
                //    mStartVpn.setEnabled(false);
                //    mDisconnect.setEnabled(false);
                //}
                //Log.d("GOOBER", "NOT CONNECTED: connectClicked = " + connectClicked + "::randomProfileSelected = " + randomProfileSelected);

                // check if this is a start of trying random profiles and it failed

                if ((randomProfileSelected)) {
                    mStartUUID = getRandomUuid();
                    if (mStartUUID != null) {
                        try {
                            //Log.d("GOOBER", "Attempting to start UUID: " + mStartUUID);
                            prepareStartProfile(START_PROFILE_BYUUID);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //Log.d("GOOBER", "Exhausted all ids = Done trying, reset flags-1");
                        // reset flags b/c done trying all profiles
                        randomProfileSelected = false;
                        excludeProfileList.clear();
                    }
                }

            } else {
                // found profile that works, so reset variables
                //Log.d("GOOBER", "Done trying, reset flags-2");
                if (msg.obj.toString().startsWith("CONNECTED")) {
                    //Log.d("GOOBER", "CONNECTED: connectClicked: Says we're connected");
                    randomProfileSelected = false;
//                    excludeProfileList.clear();
//                    mStartVpn.setEnabled(false);
//                    mDisconnect.setEnabled(true); // DJF 08-27
                    // GOOBER - Generally don't want stuff after text when CONNECTED
                    mStatus.setText("CONNECTED");
                } else if ((msg.obj.toString().startsWith("NONETWORK")) || (msg.obj.toString().startsWith("AUTH_FAILED")) || (msg.obj.toString().startsWith("EXITING"))) {
                    randomProfileSelected = false;
                    excludeProfileList.clear();
//                    mStartVpn.setEnabled(true);
//                    mDisconnect.setEnabled(false); // DJF 08-27
                    // GOOBER - get rid of pipe ("|") from end of message
                    mStatus.setText(((CharSequence) msg.obj).subSequence(0, ((CharSequence) msg.obj).length() - 1));
                } else { // all other messages are in-process messages so disable "Connect" button
//                    mStartVpn.setEnabled(false);
//                    mDisconnect.setEnabled(false); // DJF 08-27
                    // GOOBER - get rid of pipe ("|") from end of message
                    mStatus.setText(((CharSequence) msg.obj).subSequence(0, ((CharSequence) msg.obj).length() - 1));
                }
            }
        } else if (msg.what == MSG_UPDATE_MYIP) {

            mMyIp.setText((CharSequence) msg.obj);
        }
        return true;
    }



    /**
     * track glacier profile name and uuid pair
     */
    public class GlacierProfile {
        private String name;
        private String uuid;

        public GlacierProfile(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getParcedName(){
            try {
                String [] splitname = name.split("_");
                return splitname[1];
            } catch (Exception e){
            }
            return name;

        }

        @Override
        public String toString() {
            return getName();
        }


    }

    /**
     * GOOBER - Import VPN from AWS
     */
    private void showImportProfileVPNDialogFragment() {
        //TODO
        DialogFragment dialogFragment = ImportVPNProfileDialogFragment.newInstance(getString(R.string.load_vpn_profile_dialog_message));
        dialogFragment.setTargetFragment(this, PROFILE_DIALOG_REQUEST_CODE);
        dialogFragment.show(getFragmentManager(), "dialog");

    }
}