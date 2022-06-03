package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.glaciersecurity.glaciermessenger.R;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;
import com.twilio.conversations.Message;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

interface toggleInputMethodListner {
    void toggleInputMethod();
}

public class smsConvActivity extends XmppActivity implements ConversationsManagerListener, Serializable,toggleInputMethodListner {

    public final static String TAG = "TwilioConversations";
    private String identity,convSid,Convtoken,newMessageBody;

    private MessagesAdapter messagesAdapter;

    private EditText writeMessageEditText;
    android.text.format.DateFormat df = new android.text.format.DateFormat();
    private final ConversationsManager ConversationsManager = new ConversationsManager(this);
    Toolbar toolbar;
    ConversationModel model;
    private Context mContext = this;
    RecyclerView recyclerView;
    //    Med
    Map<String, String> convContList;
    Map<String, String> cList;
    ImageButton attach;
    String cameraPermission[];
    String storagePermission[];
    Uri imageuri = null;
    private static final int IMAGEPICK_GALLERY_REQUEST = 300;
    private static final int IMAGE_PICKCAMERA_REQUEST = 400;
    private static final int CAMERA_REQUEST = 100;
    private static final int STORAGE_REQUEST = 200;
    String uid, myuid, image;
    boolean notify = false;
    boolean isBlocked = false;
    private MMSmediaAdapter mediaPreviewAdapter;
    RecyclerView mediaPreview;
    private boolean groupConv = false;
    private String[] groupParticipants;

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onBackendConnected() {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_conv);
        model = (ConversationModel) getApplicationContext();
        cList = model.getcList();
        toolbar = (Toolbar) findViewById(R.id.aToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        ConversationsManager.setListener(this);
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        mediaPreviewAdapter = new MMSmediaAdapter(this,(toggleInputMethodListner) this);
        //android.app.AlertDialog.Builder mediaPreview = null;
        mediaPreview = findViewById(R.id.media_preview);
        mediaPreview.setAdapter(mediaPreviewAdapter);
        if(getIntent().hasExtra("conv_sid")) {
            convSid = getIntent().getExtras().getString("conv_sid");
        }
        if(getIntent().hasExtra("identity")){
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        ConversationsClient conversationsClient = model.getConversationsClient();
        if(getIntent().hasExtra("title")){
            String Contact_name = (cList.get(getIntent().getStringExtra("title")) != null) ? cList.get(getIntent().getStringExtra("title")) : getIntent().getStringExtra("title");
            setTitle(Contact_name);
        }
        if(getIntent().hasExtra("phoneNumber")){
            convSid = getIntent().getStringExtra("phoneNumber");
        }
        if(getIntent().hasExtra("messageBody")){
            newMessageBody = getIntent().getStringExtra("messageBody");
            Log.d("Glacier "," To phoneNumber "+convSid+" sendedMessage "+newMessageBody);
        }

        if(conversationsClient != null){
            convContList = model.getContConv();
            if(convContList != null && convContList.size() > 0){
                String sid = convContList.get(convSid);
                if(sid != null && !(sid.trim().equals("No sid"))){
                    convSid = sid;
                }
            }
            if(convSid != null && convSid.equals("Group")){
                convSid = getIntent().getStringExtra("groupName");
                groupParticipants = getIntent().getStringArrayExtra("GroupContact");
                Log.d("Glacier","groupParticipants "+String.join(",",groupParticipants));
                groupConv = true;

            }
            ConversationsManager.getConversation(convSid, false, conversationsClient);
        }else {
            ConversationsManager.initializeWithAccessToken(this, Convtoken,convSid);
        }

        recyclerView = findViewById(R.id.recycler_gchat);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        // for a chat app, show latest messages at the bottom
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        //messagesAdapter = new MessageListAdapter(this);
        messagesAdapter = new MessagesAdapter();
        recyclerView.setAdapter(messagesAdapter);

        writeMessageEditText = findViewById(R.id.edit_gchat_message);
        attach = findViewById(R.id.attachbtn);

        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePicDialog();
            }
        });
        ImageButton sendChatMessageButton = findViewById(R.id.button_gchat_send);
        sendChatMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Button clicked "+writeMessageEditText.getText().toString());
                String messageBody = writeMessageEditText.getText().toString().trim();
                if(mediaPreviewAdapter.hasAttachments()){
                    ConversationsManager.sendMMSMessage(mediaPreviewAdapter.getAttachments());
                }
                else if (messageBody.length() > 0) {
                    ConversationsManager.sendMessage(messageBody);
                }
            }
        });

    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_sms, menu);
        MenuItem menuItem = menu.findItem(R.id.group_edit_sms);
        if(getIntent().hasExtra("is_group") && getIntent().getExtras().getBoolean("is_group")) {
            Log.d("Glacier","ConvSid group SMS");
            menuItem.setVisible(true);
        }else
            menuItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.group_edit_sms :
                //add_participant();
                //ConversationsManager.conversation.getParticipantsList();
                Intent intent = new Intent(mContext, EditGroup.class);
                startActivity(intent.putExtra("account",identity));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }
    public void showImagePicDialog(){
        String options[] = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(smsConvActivity.this);
        builder.setTitle("Pick Image From");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (which == 0) {
                    if (!checkCameraPermission()) { // if permission is not given
                        requestCameraPermission(); // request for permission
                    } else {
                        pickFromCamera(); // if already access granted then click
                    }
                } else if (which == 1) {
                    if (!checkStoragePermission()) { // if permission is not given
                        requestStoragePermission(); // request for permission
                    } else {
                        pickFromGallery(); // if already access granted then pick
                    }
                }
            }
        });
        builder.create().show();
    }
    public boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    public boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }
    private void requestStoragePermission() {
        requestPermissions(storagePermission, STORAGE_REQUEST);
    }
    private void requestCameraPermission() {
        requestPermissions(cameraPermission, CAMERA_REQUEST);
    }
    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_pic");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        imageuri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent camerIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camerIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageuri);
        startActivityForResult(camerIntent, IMAGE_PICKCAMERA_REQUEST);
    }
    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGEPICK_GALLERY_REQUEST);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void showList() {
        /*Intent SMSActivity = new Intent(getApplicationContext(), SMSActivity.class);
        startActivity(SMSActivity);*/
        //ProgressBar progressBar = findViewById(R.id.progressBar2);
        //progressBar.setVisibility(View.GONE);
        Log.d("Glacier","smsConvActivity ConversationsManager "+ConversationsManager.getConversation());
        if(groupConv) {
            add_participant(groupParticipants);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void add_participant(String[] participant_list){
        String addParticipantUrl = mContext.getString(R.string.add_participant_url);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("ConvSid", ConversationsManager.conversation.getSid())
                .add("participants", String.join(",",participant_list))
                .add("identity",identity)
                .build();
        Request request = new Request.Builder()
                .url(addParticipantUrl)
                .post(requestBody)
                .build();
        Log.d("Glacier","request "+request);
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Log.d("Glacier", "Response from server: " + responseBody);
        }catch (Exception e){
            Log.d("Glacier", "Response from server: " + e.getMessage());
        }
    }
    private void retrieveTokenFromServer() {
        ConversationsManager.retrieveAccessTokenFromServer(this, identity, new TokenResponseListener() {
            @Override
            public void receivedTokenResponse(boolean success, @org.jetbrains.annotations.Nullable Exception exception,String token) {
                if (success) {
                    //ConversationsManager.initializeWithAccessToken(this,)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // need to modify user interface elements on the UI thread
//                            setTitle(identity);
                        }
                    });
                }
                else {
                    String errorMessage = getString(R.string.error_retrieving_access_token);
                    if (exception != null) {
                        errorMessage = errorMessage + " " + exception.getLocalizedMessage();
                    }
                    Log.d(TAG,"errorMessage "+errorMessage);
                    /*Toast.makeText(MainActivity.this,
                            errorMessage,
                            Toast.LENGTH_LONG)
                            .show();*/
                }
            }
        });
    }
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGEPICK_GALLERY_REQUEST) {
                imageuri = data.getData(); // get image data to upload
                try {
                    sendImageMessage(imageuri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (requestCode == IMAGE_PICKCAMERA_REQUEST) {
                try {
                    sendImageMessage(imageuri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    public void toggleInputMethod() {
        boolean hasAttachments = mediaPreviewAdapter.hasAttachments();
        writeMessageEditText.setVisibility(hasAttachments ? View.GONE : View.VISIBLE);
        mediaPreview.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);
    }
    private void sendImageMessage(Uri imageuri) throws IOException {
        mediaPreviewAdapter.addMediaPreviews(com.glaciersecurity.glaciermessenger.ui.util.Attachment.of(this, imageuri, com.glaciersecurity.glaciermessenger.ui.util.Attachment.Type.FILE));
        toggleInputMethod();
    }
    @Override
    public void receivedNewMessage(String newMessage,String messageConversationSid,String messageAuthor) {
        Log.d("Glacier","getFriendlyName"+ConversationsManager.conversation.getFriendlyName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                Conversation current_conv = model.getConversation();
                if(current_conv != null && current_conv.getSid().equals(messageConversationSid)) {
                    messagesAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(ConversationsManager.getMessages().size() - 1);
                    ConversationsManager.conversation.setAllMessagesRead(new CallbackListener<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            if(model.getNotificationManager() != null && ConversationsManager.conversation.getFriendlyName().length() > 5) {
                                Log.d("Glacier","getFriendlyName "+ Integer.parseInt(ConversationsManager.conversation.getFriendlyName().substring(2, 5))+" "+model.getNotificationManager());
                                model.clearNotification(Integer.parseInt(ConversationsManager.conversation.getFriendlyName().substring(2, 5)));
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void reloadMessages() {
        model.setConversation(ConversationsManager.conversation);
        Log.d("Glacier","getFriendlyName"+ConversationsManager.conversation.getFriendlyName());
        if(model.getNotificationManager() != null) {
            if(ConversationsManager.conversation.getFriendlyName().length() > 5) {
                Log.d("Glacier", "getFriendlyName " + Integer.parseInt(ConversationsManager.conversation.getFriendlyName().substring(2, 5)) + " " + model.getNotificationManager());
                model.clearNotification(Integer.parseInt(ConversationsManager.conversation.getFriendlyName().substring(2, 5)));
            }else{
                model.clearNotification(123);
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                messagesAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(ConversationsManager.getMessages().size()-1);
                ConversationsManager.conversation.setAllMessagesRead(new CallbackListener<Long>() {
                    @Override
                    public void onSuccess(Long result) {

                    }
                });
            }
        });
    }

    @Override
    public void messageSentCallback() {
        smsConvActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                writeMessageEditText.setText("");
            }
        });
    }

    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity));
    }

    class MessagesAdapter extends RecyclerView.Adapter{

        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

        MessagesAdapter() {

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
            View view;
            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.sms_chat_me, parent, false);
                return new SentMessageHolder(view);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.sms_chat_other, parent, false);
                return new MessagesAdapter.ReceivedMessageHolder(view);
            }

            return null;
        }
        public int getItemViewType(int position) {
            Message message = ConversationsManager.getMessages().get(position);

            if (message.getAuthor().equals(identity)) {
                // If the current user is the sender of the message
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Message message = ConversationsManager.getMessages().get(position);
            Message old_message = position == 0 ? ConversationsManager.getMessages().get(position) : ConversationsManager.getMessages().get(position - 1);
            boolean new_one = false;
            Log.d(TAG,"onBindViewHolder "+message.getAuthor()+"---"+message.getMessageBody()+"---"+message);
            String new_data = df.format("dd/MM/yyyy", message.getDateCreatedAsDate()).toString();
            String old_data = df.format("dd/MM/yyyy", old_message.getDateCreatedAsDate()).toString();

            if(!new_data.equals(old_data) || 0 == position){
                new_one = true;
            }
            Log.d(TAG,"onBindViewHolder "+new_data+"---"+old_data+"--"+new_data.equals(old_data)+"--"+((getItemCount()-1) != position)+"---"+getItemCount()+"==="+position+"-------"+new_one);
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message,new_one);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message,new_one);
                    break;
                default:
                    ((SentMessageHolder) holder).bind(message,new_one);
            }
        }

        @Override
        public int getItemCount() {
            return ConversationsManager.getMessages().size();
        }

        class SentMessageHolder extends RecyclerView.ViewHolder implements Runnable{
            TextView messageText, timeText,dateText;
            ImageView sentImg;

            SentMessageHolder(View itemView) {
                super(itemView);
                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message_me);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
                sentImg = itemView.findViewById(R.id.text_gchat_media_me);
            }

            void bind(Message message,boolean new_date) {

                Log.d(TAG,"onBindViewHolder "+message.getAuthor()+"-----------"+message.getMessageBody()+messageText+"------"+itemView);
                if(message.hasMedia()){
                    message.getMediaContentTemporaryUrl(new CallbackListener<String>() {
                        @Override
                        public void onSuccess(String result) {
                            if (result != null) {
                                Log.d("Glacier", "result "+result);
                                downloadMedia(result,new ImgResponseListener(){
                                    public void receivedImgResponse(Bitmap bmpImg){
                                        messageText.setText("");
                                        sentImg.setVisibility(View.VISIBLE);
                                        sentImg.setImageBitmap(bmpImg);
                                    }
                                });
                            }
                        }
                    });
                }else
                    messageText.setText(message.getMessageBody());

                // Format the stored timestamp into a readable String using method.

                timeText.setText(df.format("hh:mm", message.getDateCreatedAsDate()).toString());
                if(new_date)
                    dateText.setText(df.format("MMM d", message.getDateCreatedAsDate()).toString());
                else
                    dateText.setVisibility(View.GONE);
                //timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            }


            @Override
            public void run() {

            }
        }

        class ReceivedMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText, nameText,dateText;
            ImageView profileImage;
            ImageView sentImg;

            ReceivedMessageHolder(View itemView) {
                super(itemView);

                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message_other);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_other);
                nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
                profileImage = (ImageView) itemView.findViewById(R.id.image_gchat_profile_other);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
                sentImg = itemView.findViewById(R.id.text_gchat_media_other);
            }

            void bind(Message message,boolean new_date) {
                Log.d("Glacier","Message has media"+message.hasMedia());
                if(message.hasMedia()){
                    message.getMediaContentTemporaryUrl(new CallbackListener<String>() {
                        @Override
                        public void onSuccess(String result) {
                            Log.d("Glacier", result);
                            downloadMedia(result, new ImgResponseListener() {
                                public void receivedImgResponse(Bitmap bmpImg) {
                                    messageText.setText("");
                                    sentImg.setVisibility(View.VISIBLE);
                                    sentImg.setImageBitmap(bmpImg);
                                }
                            });
                        }
                    });
                }else
                    messageText.setText(message.getMessageBody());

                // Format the stored timestamp into a readable String using method.
                timeText.setText(df.format("hh:mm", message.getDateCreatedAsDate()).toString());
                if(new_date)
                    dateText.setText(df.format("MMM d", message.getDateCreatedAsDate()).toString());
                else
                    dateText.setVisibility(View.GONE);
                nameText.setText(message.getAuthor());

                // Insert the profile image from the URL into the ImageView.
                //Utils.displayRoundImageFromUrl(mContext, message.getSender().getProfileUrl(), profileImage);
            }
        }
        private void downloadMedia(String mediaURL, ImgResponseListener imgResponseListener) {
            smsConvActivity.this.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    try {
                        URL url = new URL(mediaURL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.setConnectTimeout(30000);
                        connection.setReadTimeout(30000);
                        connection.setInstanceFollowRedirects(true);
                        InputStream is = connection.getInputStream();
                        BufferedInputStream buf = new BufferedInputStream(is);
                        Bitmap bMap = BitmapFactory.decodeStream(buf);
                        Log.d("Glacier", "bMap "+bMap.toString());
                        imgResponseListener.receivedImgResponse(bMap);
                        Log.d("Glacier", "bMap imgResponseListener"+bMap);
                    } catch (NetworkOnMainThreadException e) {
                        Log.d("Glacier", "NetworkOnMainThreadException "+e);
                    } catch (Exception e) {
                        Log.e("Glacier", "Exception "+e.getMessage());
                    }
                }
            });
        }
    }
}

interface ImgResponseListener {
    void receivedImgResponse(Bitmap bmpImg);
}