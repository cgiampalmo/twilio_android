package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.ui.util.AvatarWorkerTask;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.xmpp.pep.Avatar;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnAvatarPublication;
import com.glaciersecurity.glaciermessenger.utils.PhoneHelper;

public class PublishProfilePictureActivity extends XmppActivity implements XmppConnectionService.OnAccountUpdate, OnAvatarPublication {

    private ImageView avatar;
    private TextView hintOrWarning;
    private TextView secondaryHint;
    private Button cancelButton;
    private Button publishButton;
    private Uri avatarUri;
    private Uri defaultUri;
    private Account account;
    private boolean support = false;
    private boolean publishing = false;
    private AtomicBoolean handledExternalUri = new AtomicBoolean(false);
    private OnLongClickListener backToDefaultListener = new OnLongClickListener() {

        //CMG AM-361
        @Override
        public boolean onLongClick(View v) {
            avatarUri = defaultUri;
            loadImageIntoPreview(defaultUri);
            return true;
        }
    };
    private boolean mInitialAccountSetup;

    @Override
    public void onAvatarPublicationSucceeded() {
        runOnUiThread(() -> {
            if (mInitialAccountSetup) {
                Intent intent = new Intent(getApplicationContext(), StartConversationActivity.class);
                StartConversationActivity.addInviteUri(intent, getIntent());
                intent.putExtra("init", true);
                startActivity(intent);
            }
            Toast.makeText(PublishProfilePictureActivity.this,
                    R.string.avatar_has_been_published,
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onAvatarPublicationFailed(int res) {
        runOnUiThread(() -> {
            hintOrWarning.setText(res);
            hintOrWarning.setTextColor(getWarningTextColor());
            hintOrWarning.setVisibility(View.VISIBLE);
            publishing = false;
            togglePublishButton(true, R.string.publish);
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_profile_picture);
        setSupportActionBar(findViewById(R.id.toolbar));

        this.avatar = findViewById(R.id.account_image);
        this.cancelButton = findViewById(R.id.cancel_button);
        this.publishButton = findViewById(R.id.publish_button);
        this.hintOrWarning = findViewById(R.id.hint_or_warning);
        this.secondaryHint = findViewById(R.id.secondary_hint);
        this.publishButton.setOnClickListener(v -> {
            if (avatarUri != null) {
                publishing = true;
                togglePublishButton(false, R.string.publishing);
                xmppConnectionService.publishAvatar(account, avatarUri, this);
            }
        });
        this.cancelButton.setOnClickListener(v -> {
            if (mInitialAccountSetup) {
                Intent intent = new Intent(getApplicationContext(), StartConversationActivity.class);
                if (xmppConnectionService != null && xmppConnectionService.getAccounts().size() == 1) {
                    StartConversationActivity.addInviteUri(intent, getIntent());
                    intent.putExtra("init", true);
                }
                startActivity(intent);
            }
            finish();
        });
        this.avatar.setOnClickListener(v -> chooseAvatar());
        if (savedInstanceState != null) {
            this.avatarUri = savedInstanceState.getParcelable("uri");
            this.defaultUri = savedInstanceState.getParcelable("default");
            this.handledExternalUri.set(savedInstanceState.getBoolean("handle_external_uri",false));
        } else {
            this.avatarUri = PhoneHelper.getProfilePictureUri(getApplicationContext());
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.reset_avatar, menu);
//        //AccountUtils.showHideMenuItems(menu);
//        MenuItem resetAvatar = menu.findItem(R.id.action_reset_avatar);
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (MenuDoubleTabUtil.shouldIgnoreTap()) {
            return false;
        }
        switch (item.getItemId()) {
            //CMG AM-361
//            case R.id.action_reset_avatar:
//                if (account != null) {
//                    avatarUri = defaultUri;
//                    loadImageIntoPreview(defaultUri);
//                    return true;
//                }
//                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (this.avatarUri != null) {
            outState.putParcelable("uri", this.avatarUri);
            outState.putParcelable("default", this.defaultUri);
        }
        outState.putBoolean("handle_external_uri", handledExternalUri.get());
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                this.avatarUri = result.getUri();
                if (xmppConnectionServiceBound) {
                    loadImageIntoPreview(this.avatarUri);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                if (error != null) {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void chooseAvatar() {
        CropImage.activity()
                .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                .setAspectRatio(1, 1)
                .setMinCropResultSize(Config.AVATAR_SIZE, Config.AVATAR_SIZE)
                .start(this);
    }

    @Override
    protected void onBackendConnected() {
        this.account = extractAccount(getIntent());
        if (this.account != null) {
            reloadAvatar();
        }
    }

    private void reloadAvatar() {
        this.support = this.account.getXmppConnection() != null && this.account.getXmppConnection().getFeatures().pep();
        if (this.avatarUri == null) {
            if (this.account.getAvatar() != null || this.defaultUri == null) {
                loadImageIntoPreview(null);
            } else {
                this.avatarUri = this.defaultUri;
                loadImageIntoPreview(this.defaultUri);
            }
        } else {
            loadImageIntoPreview(avatarUri);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Intent intent = getIntent();
        this.mInitialAccountSetup = intent != null && intent.getBooleanExtra("setup", false);

        final Uri uri = intent != null ? intent.getData() : null;

        if (uri != null && handledExternalUri.compareAndSet(false,true)) {
            CropImage.activity(uri).setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                    .setAspectRatio(1, 1)
                    .setMinCropResultSize(Config.AVATAR_SIZE, Config.AVATAR_SIZE)
                    .start(this);
            return;
        }

        if (this.mInitialAccountSetup) {
            this.cancelButton.setText(R.string.skip);
        }
        configureActionBar(getSupportActionBar(), !this.mInitialAccountSetup && !handledExternalUri.get());
    }

    protected void loadImageIntoPreview(Uri uri) {

        Bitmap bm = null;
        if (uri == null) {
            bm = avatarService().get(account, (int) getResources().getDimension(R.dimen.publish_avatar_size));
        } else {
            try {
                bm = xmppConnectionService.getFileBackend().cropCenterSquare(uri, (int) getResources().getDimension(R.dimen.publish_avatar_size));
            } catch (Exception e) {
                Log.d(Config.LOGTAG, "unable to load bitmap into image view", e);
            }
        }

        if (bm == null) {
            togglePublishButton(false, R.string.publish);
            this.hintOrWarning.setVisibility(View.VISIBLE);
            this.hintOrWarning.setTextColor(getWarningTextColor());
            this.hintOrWarning.setText(R.string.error_publish_avatar_converting);
            return;
        }
        this.avatar.setImageBitmap(bm);
        if (support) {
            togglePublishButton(uri != null, R.string.publish);
            this.hintOrWarning.setVisibility(View.INVISIBLE);
        } else {
            togglePublishButton(false, R.string.publish);
            this.hintOrWarning.setVisibility(View.VISIBLE);
            this.hintOrWarning.setTextColor(getWarningTextColor());
            if (account.getStatus() == Account.State.ONLINE) {
                this.hintOrWarning.setText(R.string.error_publish_avatar_no_server_support);
            } else {
                this.hintOrWarning.setText(R.string.error_publish_avatar_offline);
            }
        }
        if (this.defaultUri == null || this.defaultUri.equals(uri)) {
            this.secondaryHint.setVisibility(View.INVISIBLE);
            this.avatar.setOnLongClickListener(null);
        } else if (this.defaultUri != null) {
            this.secondaryHint.setVisibility(View.VISIBLE);
            this.avatar.setOnLongClickListener(this.backToDefaultListener);
        }
    }

    protected void togglePublishButton(boolean enabled, @StringRes int res) {
        final boolean status = enabled && !publishing;
        this.publishButton.setText(publishing ? R.string.publishing : res);
        this.publishButton.setEnabled(status);
    }

    public void refreshUiReal() {
        if (this.account != null) {
            reloadAvatar();
        }
    }

    @Override
    public void onAccountUpdate() {
        refreshUi();
    }

}
