package com.glaciersecurity.glaciermessenger.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.glaciersecurity.glaciercore.api.APIVpnProfile;
import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciercore.api.IOpenVPNStatusCallback;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
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
import java.util.List;
import java.util.Random;

public class OpenVPNFragment extends Fragment implements View.OnClickListener, Handler.Callback{

    final static String EMERGENCY_PROFILE_TAG = "emerg";
    static final int PROFILE_DIALOG_REQUEST_CODE = 8;
    static final String PROFILE_SELECTED = "PROFILE_SELECTED";

    private ConnectivityReceiver connectivityReceiver; //CMG AM-41

    private TextView mHelloWorld;
    private Button mStartVpn;
    private Button mDisconnect;
    private TextView mMyIp;
    private TextView mStatus;
    private TextView mProfile;
    private CheckBox enableEmergConnectCheckBox;
    private GlacierProfile emergencyProfile;
    private Spinner profileSpinner;
    private ArrayAdapter<GlacierProfile> spinnerAdapter;

    // variables used for random profile retries upon failure
    private boolean connectClicked = false;
    private boolean disconnectClicked = false;
    private boolean randomProfileSelected = false;
    private List<String> excludeProfileList = new ArrayList<String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.openvpn_fragment, container, false);
        v.findViewById(R.id.disconnect).setOnClickListener(this);
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
        //v.findViewById(R.id.getMyIP).setOnClickListener(this);
        //v.findViewById(R.id.startembedded).setOnClickListener(this);
        v.findViewById(R.id.addNewProfile).setOnClickListener(this);
        //mHelloWorld = (TextView) v.findViewById(R.id.helloworld);
        mStartVpn = (Button) v.findViewById(R.id.startVPN);
        mDisconnect = (Button) v.findViewById(R.id.disconnect);
        mStatus = (TextView) v.findViewById(R.id.status);
        mProfile = (TextView) v.findViewById(R.id.currentProfile);
        // mMyIp = (TextView) v.findViewById(R.id.MyIpText);
        addItemsOnProfileSpinner(v);

        mStartVpn.setEnabled(true); //DJF 08-27 Start on, and then toggle off depending on conditions
        mDisconnect.setEnabled(true); //DJF 08-27 Start on, and then toggle off depending on conditions

//        //CMG AM-41
//        offlineLayout = (LinearLayout) v.findViewById(R.id.offline_layout);
//        networkStatus = (TextView) v.findViewById(R.id.network_status);
//        offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
//        checkNetworkStatus();

        return v;

    }

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_MYIP = 1;
    private static final int START_PROFILE_EMBEDDED = 2;
    private static final int START_PROFILE_BYUUID = 3;
    private static final int ICS_OPENVPN_PERMISSION = 7;
    private static final int PROFILE_ADD_NEW = 8;


//    //CMG AM-41
//    private LinearLayout offlineLayout;
//    private TextView networkStatus;
//
//    private View.OnClickListener mRefreshNetworkClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            networkStatus.setCompoundDrawables(null, null, null, null);
//            networkStatus.setText(getActivity().getResources().getString(R.string.refreshing));
//            if (ConnectivityReceiver.isConnected(getActivity())){
//                final Handler handler = new Handler();
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        reconfigureOfflineText();
//                        offlineLayout.setVisibility(View.GONE);
//                    }
//                }, 1000);
//            }else{
//                final Handler handler = new Handler();
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        reconfigureOfflineText();
//                        offlineLayout.setVisibility(View.VISIBLE);
//                    }
//                }, 1000);
//            }
//        }
//    };
//
//    // CMG AM-41
//    private void checkNetworkStatus() {
//        if (ConnectivityReceiver.isConnected(getActivity())){
//            onConnected();
//        }else{
//            onDisconnected();
//        }
//    }
//
//    public void onConnected(){
//        offlineLayout.setVisibility(View.GONE);
//    }
//
//    public void onDisconnected(){
//        offlineLayout.setVisibility(View.VISIBLE);
//    }
//
//    private void reconfigureOfflineText() {
//        networkStatus.setText(getActivity().getResources().getString(R.string.offline));
//        Drawable refreshIcon =
//                ContextCompat.getDrawable(getActivity(), R.drawable.ic_refresh_black_24dp);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
//            networkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(refreshIcon, null, null, null);
//        } else{
//            refreshIcon.setBounds(0, 0, refreshIcon.getIntrinsicWidth(), refreshIcon.getIntrinsicHeight());
//            networkStatus.setCompoundDrawables(refreshIcon, null, null, null);
//        }
//    }

    protected IOpenVPNAPIService mService=null;
    private Handler mHandler;

    /**
     * GOOBER - Display no emergency profile exist
     */
    private void displayNoEmergencyProfile() {
        AlertDialog.Builder d = new AlertDialog.Builder(this.getActivity());

        d.setIconAttribute(android.R.attr.alertDialogIcon);
        d.setTitle("Emergency Profile");
        d.setMessage("Cannot enable Emergency Profile.  No such profile exists!!");
        d.setPositiveButton(android.R.string.ok, null);
        d.show();

    }

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
            }
            catch(Exception e2){
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
            return gp.getName();
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
        profileSpinner = (Spinner) v.findViewById(R.id.profileSpinner);
        List<String> list = new ArrayList<String>();
        list.add("No Profiles Found");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this.getActivity(),android.R.layout.simple_spinner_item, list);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(spinnerAdapter);
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

            spinnerAdapter = new ArrayAdapter<GlacierProfile>(this.getActivity(),android.R.layout.simple_spinner_item, nameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            profileSpinner.setAdapter(spinnerAdapter);

            if(list.size()> 0) {
                Button b = mStartVpn;
                b.setOnClickListener(this);
                b.setVisibility(View.VISIBLE);
                // b.setText("Submit");
                mStartUUID = list.get(0).mUUID;
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startVPN:
                isConnected_EnableDisconnect();
                disconnectClicked = false;
                // GOOBER
                GlacierProfile glacierProfile = (GlacierProfile) profileSpinner.getSelectedItem();
                mStartUUID = glacierProfile.getUuid();

                // GOOBER retrieve previous profile selected
                SharedPreferences sp = this.getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
                sp.edit().putString("last_spinner_profile", mStartUUID).commit();
                // Log.d("GOOBER", "This is uuid set: " + mStartUUID);

                // see if random profile selected
                if (mStartUUID.compareTo("random") == 0) {
                    excludeProfileList.clear();
                    randomProfileSelected = true;
                    connectClicked = true;
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
                break;
            case R.id.disconnect:
                Log.d("RemoteExample", "DAVID Disconnect");
                try {
                    mService.disconnect();
                } catch (RemoteException e) {
                    //CMG AM-240
                    Log.d("RemoteException", "at mService.disconnect");
                    e.printStackTrace();
                }
                isDisconnected_EnableConnect();
                disconnectClicked = true;

                break;
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

    public void isConnected_EnableDisconnect(){
        mDisconnect.setEnabled(true);
        mStartVpn.setEnabled(false);

    }
    public void isDisconnected_EnableConnect(){
        mDisconnect.setEnabled(false);
        mStartVpn.setEnabled(true);

    }
    /**
     * HONEYBADGER AM-76
     */
    public void launchPlayStoreCore(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("glacier_core_https"));
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
                    Log.d("RemoteException or Security Exception", "register status callback");
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

    @Override
    public boolean handleMessage(Message msg) {
        Log.d("GOOBER", "OpenVPNFragment::handleMessage(): " + msg.obj.toString() + "::What = " + msg.what);

        //Log.d("GOOBER", "** UPDATED MESSAGE: " + ((CharSequence) msg.obj).subSequence(0, ((CharSequence) msg.obj).length() - 1) + "**" + msg.obj.toString());
        if(msg.what == MSG_UPDATE_STATE) {
            // GOOBER - check for NOPROCESS string and change it to NOT CONNECTED
            if (msg.obj.toString().startsWith("NOPROCESS")) {
                mStatus.setText("NOT CONNECTED");

                // DJF 08-27
                //if (profileSpinner == null) {
                mStartVpn.setEnabled(true);
                mDisconnect.setEnabled(false);
                //} else {
                //    mStartVpn.setEnabled(false);
                //    mDisconnect.setEnabled(false);
                //}
                //Log.d("GOOBER", "NOT CONNECTED: connectClicked = " + connectClicked + "::randomProfileSelected = " + randomProfileSelected);

                // check if this is a start of trying random profiles and it failed
                if (disconnectClicked != true) {
                    if ((connectClicked) && (randomProfileSelected)) {
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
                            connectClicked = false;
                            excludeProfileList.clear();
                        }
                    }
                }
            } else {
                // found profile that works, so reset variables
                //Log.d("GOOBER", "Done trying, reset flags-2");
                if (msg.obj.toString().startsWith("CONNECTED")) {
                    //Log.d("GOOBER", "CONNECTED: connectClicked: Says we're connected");
                    randomProfileSelected = false;
                    connectClicked = false;
                    disconnectClicked = false;
                    excludeProfileList.clear();
                    mStartVpn.setEnabled(false);
                    mDisconnect.setEnabled(true); // DJF 08-27
                    // GOOBER - Generally don't want stuff after text when CONNECTED
                    mStatus.setText("CONNECTED");
                } else if ((msg.obj.toString().startsWith("NONETWORK")) || (msg.obj.toString().startsWith("AUTH_FAILED")) || (msg.obj.toString().startsWith("EXITING"))) {
                    randomProfileSelected = false;
                    connectClicked = false;
                    disconnectClicked = false;
                    excludeProfileList.clear();
                    mStartVpn.setEnabled(true);
                    mDisconnect.setEnabled(false); // DJF 08-27
                    // GOOBER - get rid of pipe ("|") from end of message
                    mStatus.setText(((CharSequence) msg.obj).subSequence(0, ((CharSequence) msg.obj).length() - 1));
                } else { // all other messages are in-process messages so disable "Connect" button
                    mStartVpn.setEnabled(false);
                    mDisconnect.setEnabled(false); // DJF 08-27
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

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * GOOBER - Import VPN from AWS
     */
    private void showImportProfileVPNDialogFragment() {
        DialogFragment dialogFragment = ImportVPNProfileDialogFragment.newInstance("No VPN");
        dialogFragment.setTargetFragment(this, PROFILE_DIALOG_REQUEST_CODE);
        dialogFragment.show(getFragmentManager(), "dialog");
        // dialogFragment.showWaitDialog(getString(R.string.load_vpn_profile_dialog_message));
    }
}