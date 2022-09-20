package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.ui.util.ViewUtil;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.google.gson.Gson;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;
import com.twilio.conversations.Message;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
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
    protected class addParticipant {
        public String message;
    }
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
        toolbar.setBackgroundColor(getColorForNumber(model.getProxyNumber()));
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
        //Log.d("Glacier","convSid "+convSid);
        if(getIntent().hasExtra("identity")){
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        ConversationsClient conversationsClient = model.getConversationsClient();
        if(getIntent().hasExtra("title")){
            String Contact_name = (cList != null && cList.get(getIntent().getStringExtra("title")) != null) ? cList.get(getIntent().getStringExtra("title")) : getIntent().getStringExtra("title");
            setTitle(Contact_name);
        }
        if(getIntent().hasExtra("phoneNumber")){
            convSid = getIntent().getStringExtra("phoneNumber");
        }
        //Log.d("Glacier","convSid "+convSid);
        if(getIntent().hasExtra("messageBody")){
            newMessageBody = getIntent().getStringExtra("messageBody");
        }
        if(conversationsClient != null){
            if(convSid != null && convSid.equals("Group")){
                convSid = getIntent().getStringExtra("groupName");
                groupParticipants = getIntent().getStringArrayExtra("GroupContact");
                //Log.d("Glacier","groupParticipants "+String.join(",",groupParticipants));
                groupConv = true;
            }else {
                convContList = model.getContConv();
                if (convContList != null && convContList.size() > 0) {
                    String sid = convContList.get(convSid);
                    if (sid != null && !(sid.trim().equals("No sid"))) {
                        convSid = sid;
                    }
                }
            }
            String proxynumber = model.getProxyNumber();
            if(getIntent().hasExtra("phoneNumber")){
                String phoneNumber = getIntent().getStringExtra("phoneNumber");
                ConversationsManager.getConversation(convSid, false, conversationsClient,proxynumber,phoneNumber);
            }else {
                ConversationsManager.getConversation(convSid, false, conversationsClient, proxynumber, "");
            }
        }else {
            if(Convtoken != null) {
                ConversationsManager.initializeWithAccessToken(this, Convtoken, convSid);
            }else{
                retrieveTokenFromServer();
            }
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
        ImageView sendChatMessageButton = findViewById(R.id.button_gchat_send);
        sendChatMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG,"Button clicked "+writeMessageEditText.getText().toString());
                String messageBody = writeMessageEditText.getText().toString().trim();
                if(mediaPreviewAdapter.hasAttachments()){
                    ConversationsManager.sendMMSMessage(mediaPreviewAdapter.getAttachments());
                    Toast.makeText(smsConvActivity.this, "Please wait. Sending an image", Toast.LENGTH_LONG).show();
                }
                else if (messageBody.length() > 0) {
                    ConversationsManager.sendMessage(messageBody);
                }
            }
        });

    }

    public int getColorForNumber(String number){
        String formattedNumber = Tools.reformatNumber(number);
        return UIHelper.getColorForSMS(formattedNumber);
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
                Conversation conversation = model.getConversation();
                if(conversation != null) {
                    Intent intent = new Intent(mContext, EditGroup.class);
                    startActivity(intent.putExtra("account", identity));
                }
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
        String proxyNumber = model.getProxyNumber();
        //Log.d("Glacier","smsConvActivity ConversationsManager proxyNumber "+proxyNumber);
        if(groupConv) {
            add_participant(groupParticipants);
        }
    }

    @Override
    public void notifyMessages(String newMessage,String messageAuthor,String messageTo) {

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
        //Log.d("Glacier","request "+request);
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
                Gson gson = new Gson();
                addParticipant responsBod = gson.fromJson(responseBody,  addParticipant.class);
                if(!(responsBod.message.equals("success"))){
                    Toast.makeText(this,responsBod.message, Toast.LENGTH_LONG).show();
                }
            }
            //Log.d("Glacier", "Response from server: " + responseBody);
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
    public void receivedNewMessage(String newMessage,String messageConversationSid,String messageAuthor,String messageTo) {
        //Log.d("Glacier","getFriendlyName"+ConversationsManager.conversation.getFriendlyName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                Conversation current_conv = model.getConversation();
                //Toast.makeText(mContext, "current_conv "+current_conv, Toast.LENGTH_SHORT).show();
                //Log.d("Glacier","current_conv"+current_conv.getFriendlyName() + current_conv.getSid() + messageConversationSid);
                if(current_conv != null && current_conv.getSid().equals(messageConversationSid)) {
                    messagesAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(ConversationsManager.getMessages().size() - 1);
                    ConversationsManager.conversation.setAllMessagesRead(new CallbackListener<Long>() {
                        @Override
                        public void onSuccess(Long result) {

                            if(model.getNotificationManager() != null && ConversationsManager.conversation.getFriendlyName().length() > 5 && isNumeric(ConversationsManager.conversation.getFriendlyName().substring(2, 5))){
                                //Log.d("Glacier","getFriendlyName "+ Integer.parseInt(ConversationsManager.conversation.getFriendlyName().substring(2, 5))+" "+model.getNotificationManager());
                                model.clearNotification(Integer.parseInt(ConversationsManager.conversation.getFriendlyName().substring(2, 5)));
                            }
                        }
                    });
                }
            }
        });
    }
    public static boolean isNumeric(String str){
        try{
            Double.parseDouble(str);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }
    @Override
    public void reloadMessages() {
        model.setConversation(ConversationsManager.conversation);
        //Log.d("Glacier","getFriendlyName"+ConversationsManager.conversation.getFriendlyName());
        if(model.getNotificationManager() != null) {
            if(ConversationsManager.conversation.getFriendlyName().length() > 5 && isNumeric(ConversationsManager.conversation.getFriendlyName().substring(2, 5)) ) {
                //Log.d("Glacier", "getFriendlyName " + Integer.parseInt(ConversationsManager.conversation.getFriendlyName().substring(2, 5)) + " " + model.getNotificationManager());
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
                mediaPreviewAdapter.notifyDataSetChanged();
                toggleInputMethod();
            }
        });
    }

    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity));
        model.setConversation(null);
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
            String new_data = df.format("dd/MM/yyyy", message.getDateCreatedAsDate()).toString();
            String old_data = df.format("dd/MM/yyyy", old_message.getDateCreatedAsDate()).toString();

            if(!new_data.equals(old_data) || 0 == position){
                new_one = true;
            }
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message,new_one,(SentMessageHolder) holder,position);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message,new_one,(ReceivedMessageHolder) holder,position);
                    break;
                default:
                    ((SentMessageHolder) holder).bind(message,new_one,(SentMessageHolder) holder,position);
            }
        }

        @Override
        public int getItemCount() {
            return ConversationsManager.getMessages().size();
        }

        class SentMessageHolder extends RecyclerView.ViewHolder implements Runnable{
            TextView messageText, timeText,dateText;
            ImageView sentImg;
            LinearLayout sentBubble;

            SentMessageHolder(View itemView) {
                super(itemView);
                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message_me);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
                sentImg = itemView.findViewById(R.id.text_gchat_media_me);
                sentBubble = itemView.findViewById(R.id.layout_gchat_container_me);
                //sentBubble.setBackgroundColor(getColorForNumber(model.getProxyNumber()));
            }

            void bind(Message message, boolean new_date, SentMessageHolder holder, int position) {

                if(message.hasMedia()){
                    messageText.setVisibility(View.GONE);
                    sentImg.setScaleType(ImageView.ScaleType.FIT_XY);
                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                    File directory = cw.getDir("GlacierSMS", Context.BIND_ADJUST_WITH_ACTIVITY);
                    File file = new File(directory,  message.getSid()+".PNG");
                    if(!file.exists()) {
                        message.getMediaContentTemporaryUrl(new CallbackListener<String>() {
                            @Override
                            public void onSuccess(String result) {
                                // Log.d(TAG, "onBindViewHolder bind " + message.getAuthor() + "-----------" + message.getMessageBody() + "------" + message.hasMedia() + "-------" + holder.getAdapterPosition() + "---------" + message.getMessageIndex() + "-------" + ConversationsManager.getMessages().get(holder.getAdapterPosition()).getMessageBody());
                                if (message.hasMedia()) {
                                    if (result != null) {
                                        Log.d("Glacier", "result " + result);
                                        downloadMedia(result, new ImgResponseListener() {
                                            Uri resultUri = Uri.parse(result);

                                            public void receivedImgResponse(Bitmap bmpImg) {
                                                FileOutputStream fos = null;
                                                try {
                                                    fos = new FileOutputStream(file);
                                                    bmpImg.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                                    fos.flush();
                                                    fos.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                int nh = (int) (bmpImg.getHeight() * (512.0 / bmpImg.getWidth()));
                                                Bitmap bitmap = bmpImg.createScaledBitmap(bmpImg, 512, nh, true);
                                                    messageText.setText("");
                                                    sentImg.setVisibility(View.VISIBLE);
                                                    sentImg.setImageBitmap(bitmap);
                                                    // sentImg.setOnClickListener(v -> ViewUtil.view(smsConvActivity.this, resultUri));
                                            }
                                        });
                                    }
                                } else {

                                }
                            }
                        });
                    }else{
                        Log.d("Glacier","BitmapFactory file "+file);
                        Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        sentImg.setVisibility(View.VISIBLE);
                        sentImg.setImageBitmap(myBitmap);
                        // sentImg.setOnClickListener(v -> ViewUtil.view(smsConvActivity.this, file,"PNG"));
                    }
                }else {
                    sentImg.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message.getMessageBody());
                }
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
//                profileImage = (ImageView) itemView.findViewById(R.id.image_gchat_profile_other);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
                sentImg = itemView.findViewById(R.id.text_gchat_media_other);
            }

            void bind(Message message,boolean new_date,ReceivedMessageHolder holder,int position) {
                Log.d("Glacier","Message has media"+message.hasMedia());
                if(message.hasMedia()){
                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                    //String path = Environment.getExternalStorageDirectory().toString()+"/Pictures/Glacier/";
                    File directory = cw.getDir("GlacierSMS", Context.BIND_ADJUST_WITH_ACTIVITY);
                    File file = new File(directory,  message.getSid()+".PNG");
                    if(!file.exists()) {
                        message.getMediaContentTemporaryUrl(new CallbackListener<String>() {
                            @Override
                            public void onSuccess(String result) {
                                Log.d("Glacier", result);
                                downloadMedia(result, new ImgResponseListener() {
                                    public void receivedImgResponse(Bitmap bmpImg) {
                                        FileOutputStream fos = null;
                                        try {
                                            fos = new FileOutputStream(file);
                                            bmpImg.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                            fos.flush();
                                            fos.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        int nh = (int) (bmpImg.getHeight() * (512.0 / bmpImg.getWidth()));
                                        Bitmap bitmap = bmpImg.createScaledBitmap(bmpImg, 512, nh, true);
                                        messageText.setText("");
                                        sentImg.setVisibility(View.VISIBLE);
                                        sentImg.setImageBitmap(bitmap);
                                    }
                                });
                            }
                        });
                    }else{
                        Log.d("Glacier","BitmapFactory file "+file);
                        Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        sentImg.setVisibility(View.VISIBLE);
                        sentImg.setImageBitmap(myBitmap);
                    }
                }else {
                    messageText.setText(message.getMessageBody());
                    sentImg.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message.getMessageBody());
                }
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