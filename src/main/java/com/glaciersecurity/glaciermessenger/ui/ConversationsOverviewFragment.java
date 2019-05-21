/*
 * Copyright (c) 2018, Daniel Gultsch All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.util.IOUtils;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.cognito.BackupAccountManager;
import com.glaciersecurity.glaciermessenger.cognito.Constants;
import com.glaciersecurity.glaciermessenger.cognito.Util;
import com.glaciersecurity.glaciermessenger.databinding.FragmentConversationsOverviewBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Conversational;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.ui.adapter.ConversationAdapter;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationArchived;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnConversationSelected;
import com.glaciersecurity.glaciermessenger.ui.util.ActivityResult;
import com.glaciersecurity.glaciermessenger.ui.util.Attachment;
import com.glaciersecurity.glaciermessenger.ui.util.MenuDoubleTabUtil;
import com.glaciersecurity.glaciermessenger.ui.util.PendingActionHelper;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.PresenceSelector;
import com.glaciersecurity.glaciermessenger.ui.util.ScrollState;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.glaciersecurity.glaciermessenger.utils.FileUtils;
import com.glaciersecurity.glaciermessenger.utils.MimeUtils;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;

import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;

public class ConversationsOverviewFragment extends XmppFragment {

	private static final String STATE_SCROLL_POSITION = ConversationsOverviewFragment.class.getName()+".scroll_state";

	private final List<Conversation> conversations = new ArrayList<>();
	private final PendingItem<Conversation> swipedConversation = new PendingItem<>();
	private final PendingItem<ScrollState> pendingScrollState = new PendingItem<>();
	private FragmentConversationsOverviewBinding binding;
	private ConversationAdapter conversationsAdapter;
	private XmppActivity activity;
	private float mSwipeEscapeVelocity = 0f;
	private PendingActionHelper pendingActionHelper = new PendingActionHelper();

	private ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,LEFT|RIGHT) {
		@Override
		public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
			//todo maybe we can manually changing the position of the conversation
			return false;
		}

		@Override
		public float getSwipeEscapeVelocity (float defaultValue) {
			return mSwipeEscapeVelocity;
		}

		@Override
		public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
									float dX, float dY, int actionState, boolean isCurrentlyActive) {
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			if(actionState != ItemTouchHelper.ACTION_STATE_IDLE){
				Paint paint = new Paint();
				paint.setColor(StyledAttributes.getColor(activity,R.attr.conversations_overview_background));
				paint.setStyle(Paint.Style.FILL);
				c.drawRect(viewHolder.itemView.getLeft(),viewHolder.itemView.getTop()
						,viewHolder.itemView.getRight(),viewHolder.itemView.getBottom(), paint);
			}
		}

		@Override
		public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
			super.clearView(recyclerView, viewHolder);
			viewHolder.itemView.setAlpha(1f);
		}

		@Override
		public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
			pendingActionHelper.execute();
			int position = viewHolder.getLayoutPosition();
			try {
				swipedConversation.push(conversations.get(position));
			} catch (IndexOutOfBoundsException e) {
				return;
			}
			conversationsAdapter.remove(swipedConversation.peek(), position);
			activity.xmppConnectionService.markRead(swipedConversation.peek());

			if (position == 0 && conversationsAdapter.getItemCount() == 0) {
				final Conversation c = swipedConversation.pop();
				//ALF AM-51, AM-64
				if (c.getMode() == Conversation.MODE_MULTI) {
					sendLeavingGroupMessage(c);
					// sleep required so message goes out before conversation thread stopped
					try { Thread.sleep(3000); } catch (InterruptedException ie) {}
				}
				activity.xmppConnectionService.archiveConversation(c);
				return;
			}
			final boolean formerlySelected = ConversationFragment.getConversation(getActivity()) == swipedConversation.peek();
			if (activity instanceof OnConversationArchived) {
				((OnConversationArchived) activity).onConversationArchived(swipedConversation.peek());
			}
			final Conversation c = swipedConversation.peek();
			final int title;
			if (c.getMode() == Conversational.MODE_MULTI) {
				if (c.getMucOptions().isPrivateAndNonAnonymous()) {
					title = R.string.title_undo_swipe_out_group_chat;
				} else {
					title = R.string.title_undo_swipe_out_channel;
				}
			} else {
				title = R.string.title_undo_swipe_out_conversation;
			}

			final Snackbar snackbar = Snackbar.make(binding.list, title, 5000)
					.setAction(R.string.undo, v -> {
						pendingActionHelper.undo();
						Conversation conversation = swipedConversation.pop();
						conversationsAdapter.insert(conversation, position);
						if (formerlySelected) {
							if (activity instanceof OnConversationSelected) {
								((OnConversationSelected) activity).onConversationSelected(c);
							}
						}
						LinearLayoutManager layoutManager = (LinearLayoutManager) binding.list.getLayoutManager();
						if (position > layoutManager.findLastVisibleItemPosition()) {
							binding.list.smoothScrollToPosition(position);
						}
					})
					.addCallback(new Snackbar.Callback() {
						@Override
						public void onDismissed(Snackbar transientBottomBar, int event) {
							switch (event) {
								case DISMISS_EVENT_SWIPE:
								case DISMISS_EVENT_TIMEOUT:
									pendingActionHelper.execute();
									break;
							}
						}
					});

			pendingActionHelper.push(() -> {
				if (snackbar.isShownOrQueued()) {
					snackbar.dismiss();
				}
				final Conversation conversation = swipedConversation.pop();
				if(conversation != null){
					if (!conversation.isRead() && conversation.getMode() == Conversation.MODE_SINGLE) {
						return;
					}
					//ALF AM-51, AM-64
					if (c.getMode() == Conversation.MODE_MULTI) {
						sendLeavingGroupMessage(c);
						// sleep required so message goes out before conversation thread stopped
						try { Thread.sleep(3000); } catch (InterruptedException ie) {}
					}
					activity.xmppConnectionService.archiveConversation(c);
				}
			});

			ThemeHelper.fix(snackbar);
			snackbar.show();
		}
	};

	/**
	 * //ALF AM-51
	 */
	public void sendLeavingGroupMessage(final Conversation conversation) {
		final Account account = conversation.getAccount();
		String dname = account.getDisplayName();
		if (dname == null) { dname = account.getUsername(); }
		String bod = dname + " " + getString(R.string.left_group);
		Message message = new Message(conversation, bod, conversation.getNextEncryption());
		activity.xmppConnectionService.sendMessage(message);
		// sleep required so message goes out before conversation thread stopped
		// maybe show a spinner?
		//try { Thread.sleep(2000); } catch (InterruptedException ie) {} //moved to each place
	}

	private ItemTouchHelper touchHelper = new ItemTouchHelper(callback);

	public static Conversation getSuggestion(Activity activity) {
		final Conversation exception;
		Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
		if (fragment instanceof ConversationsOverviewFragment) {
			exception = ((ConversationsOverviewFragment) fragment).swipedConversation.peek();
		} else {
			exception = null;
		}
		return getSuggestion(activity, exception);
	}

	public static Conversation getSuggestion(Activity activity, Conversation exception) {
		Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
		if (fragment instanceof ConversationsOverviewFragment) {
			List<Conversation> conversations = ((ConversationsOverviewFragment) fragment).conversations;
			if (conversations.size() > 0) {
				Conversation suggestion = conversations.get(0);
				if (suggestion == exception) {
					if (conversations.size() > 1) {
						return conversations.get(1);
					}
				} else {
					return suggestion;
				}
			}
		}
		return null;

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		pendingScrollState.push(savedInstanceState.getParcelable(STATE_SCROLL_POSITION));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof XmppActivity) {
			this.activity = (XmppActivity) activity;
		} else {
			throw new IllegalStateException("Trying to attach fragment to activity that is not an XmppActivity");
		}
	}

	@Override
	public void onPause() {
		Log.d(Config.LOGTAG,"ConversationsOverviewFragment.onPause()");
		pendingActionHelper.execute();
		super.onPause();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.activity = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.mSwipeEscapeVelocity = getResources().getDimension(R.dimen.swipe_escape_velocity);
		this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_conversations_overview, container, false);
		this.binding.fab.setOnClickListener((view) -> StartConversationActivity.launch(getActivity()));

		this.conversationsAdapter = new ConversationAdapter(this.activity, this.conversations);
		this.conversationsAdapter.setConversationClickListener((view, conversation) -> {
			if (activity instanceof OnConversationSelected) {
				((OnConversationSelected) activity).onConversationSelected(conversation);
			} else {
				Log.w(ConversationsOverviewFragment.class.getCanonicalName(), "Activity does not implement OnConversationSelected");
			}
		});
		this.binding.list.setAdapter(this.conversationsAdapter);
		this.binding.list.setLayoutManager(new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false));
		this.touchHelper.attachToRecyclerView(this.binding.list);
		return binding.getRoot();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.fragment_conversations_overview, menu);
	}

	@Override
	public void onBackendConnected() {
		refresh();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		ScrollState scrollState = getScrollState();
		if (scrollState != null) {
			bundle.putParcelable(STATE_SCROLL_POSITION, scrollState);
		}
	}

	private ScrollState getScrollState() {
		if (this.binding == null) {
			return null;
		}
		LinearLayoutManager layoutManager = (LinearLayoutManager) this.binding.list.getLayoutManager();
		int position = layoutManager.findFirstVisibleItemPosition();
		final View view = this.binding.list.getChildAt(0);
		if (view != null) {
			return new ScrollState(position,view.getTop());
		} else {
			return new ScrollState(position, 0);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(Config.LOGTAG, "ConversationsOverviewFragment.onStart()");
		if (activity.xmppConnectionService != null) {
			refresh();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(Config.LOGTAG, "ConversationsOverviewFragment.onResume()");
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (MenuDoubleTabUtil.shouldIgnoreTap()) {
			return false;
		}
		switch (item.getItemId()) {
			case R.id.action_search:
				startActivity(new Intent(getActivity(), SearchActivity.class));
				return true;
			case R.id.action_wipe_all_history:
				wipeAllHistoryDialog();
				break;
			case R.id.action_filesafe: //ALF AM-277
				fileSafeChooserDialog();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	void refresh() {
		if (this.binding == null || this.activity == null) {
			Log.d(Config.LOGTAG,"ConversationsOverviewFragment.refresh() skipped updated because view binding or activity was null");
			return;
		}
		this.activity.xmppConnectionService.populateWithOrderedConversations(this.conversations);
		Conversation removed = this.swipedConversation.peek();
		if (removed != null) {
			if (removed.isRead()) {
				this.conversations.remove(removed);
			} else {
				pendingActionHelper.execute();
			}
		}
		this.conversationsAdapter.notifyDataSetChanged();
		ScrollState scrollState = pendingScrollState.pop();
		if (scrollState != null) {
			setScrollPosition(scrollState);
		}
	}

	private void setScrollPosition(ScrollState scrollPosition) {
		if (scrollPosition != null) {
			LinearLayoutManager layoutManager = (LinearLayoutManager) binding.list.getLayoutManager();
			layoutManager.scrollToPositionWithOffset(scrollPosition.position, scrollPosition.offset);
		}
	}

	//ALF AM-277 down to GOOBER
	@Override
	public void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		ActivityResult activityResult = ActivityResult.of(requestCode, resultCode, data);
		if (activity != null && activity.xmppConnectionService != null) {
			if (activityResult.resultCode == Activity.RESULT_OK && requestCode == ConversationFragment.ATTACHMENT_CHOICE_CHOOSE_FILE) {
				final List<Attachment> fileUris = Attachment.extractFileSafeAttachments(getActivity(), activityResult.data, Attachment.Type.FILE);
				tryFileSafeUpload(fileUris);
			}
		}
	}

	//ALF AM-277
	private void tryFileSafeUpload(List<Attachment> fileUris) {
		if (fileUris == null || fileUris.size() == 0) {
			return;
		}

        fileSafeUris = fileUris;

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// retrieve Cognito credentials
		getCognitoInfo();

		// sign into Cognito
		signInUser();
		// check Cognito status
		// if good, start upload (have popup with progress bar)

	}

	private final String REPLACEMENT_ORG_ID = "<org_id>";
	String username = null;
	String password = null;
	String organization = null;
	Activity context = null;
    List<Attachment> fileSafeUris = null;
	/**
	 * Retrieve Cognito account information from file
	 */
	private void getCognitoInfo() {
		if (context == null) {
			context = getActivity();
		}
		BackupAccountManager backupAccountManager = new BackupAccountManager(context);
		BackupAccountManager.AccountInfo accountInfo = backupAccountManager.getAccountInfo(BackupAccountManager.LOCATION_PRIVATE, BackupAccountManager.APPTYPE_MESSENGER);
		if (accountInfo != null) {
			BackupAccountManager.Account cognitoAccount = accountInfo.getCognitoAccount();

			username = cognitoAccount.getAttribute(BackupAccountManager.COGNITO_USERNAME_KEY);
			password = cognitoAccount.getAttribute((BackupAccountManager.COGNITO_PASSWORD_KEY));
			organization = cognitoAccount.getAttribute((BackupAccountManager.COGNITO_ORGANIZATION_KEY));
		}
	}

	/**
	 * Sign into Cognito
	 */
	private void signInUser() {
        AppHelper.init(getActivity().getApplicationContext());
		AppHelper.setUser(username);
		AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
	}

	/**
	 * Check if S3 bucket exists
	 *
	 * @return
	 */
	private boolean doesBucketExist() {
		try {
			String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
			AmazonS3 sS3Client = Util.getS3Client(context);

			return sS3Client.doesBucketExist(bucketName);
		} catch (Exception e) {
			String temp = e.getMessage();
			e.printStackTrace();
		}

		// bucket doesn't exist if there's a problem
		return false;
	}

	private void uploadFileSafe() {

		String bucketName = Constants.BUCKET_NAME.replace(REPLACEMENT_ORG_ID,organization);
		AmazonS3 sS3Client = Util.getS3Client(context);
		TransferUtility transferUtility = Util.getTransferUtility(context, bucketName);
		int totalForCompletion = fileSafeUris.size() * 100;
		int[] transferIds = new int[fileSafeUris.size()];
		int[] completion = new int[fileSafeUris.size()];
		File[] tempFiles = new File[fileSafeUris.size()];

		for (int i=0; i<transferIds.length; i++) {
			transferIds[i] = -1;
			completion[i] = 0;
		}

        showUploadDialog(context.getString(R.string.upload_filesafe_dialog_message));

		int ctr = 0;
        for (Attachment attachment : fileSafeUris) {
            try {
				final String filepath = FileUtils.getPath(activity.xmppConnectionService, attachment.getUri());
				File uploadfile = null;

				if (filepath != null && !FileBackend.isPathBlacklisted(filepath)) {
					uploadfile = activity.xmppConnectionService.getFileBackend().getFileForPath(filepath);
				} else {
					try {
						Uri uri = attachment.getUri();
						String filename = uri.getLastPathSegment();
						if (filename.startsWith("enc")) {
							filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
						}

						String mime = MimeUtils.guessMimeTypeFromUriAndMime(activity.xmppConnectionService, attachment.getUri(), null);
						String extension = MimeUtils.guessExtensionFromMimeType(mime);
						if (extension == null) {
							extension = activity.xmppConnectionService.getFileBackend().getExtensionFromUri(attachment.getUri());
						}
						if (extension != null) {
							filename = filename + "." + extension;
						}

						uploadfile = new File(context.getFilesDir().getAbsolutePath(), filename);

						try(OutputStream outputStream = new FileOutputStream(uploadfile)){
							IOUtils.copy(context.getContentResolver().openInputStream(attachment.getUri()), outputStream);
						} catch (FileNotFoundException e) {
							Log.e("FileSafe", "FileNotFound Error in local file save");
						} catch (IOException e) {
							Log.e("FileSafe", "IO Error in local file save");
						}

						tempFiles[ctr] = uploadfile;
					} catch (Exception ex) {
						Log.e("FileSafe", "Error in FileSafe without path");
					}
				}

				if (uploadfile == null) {
					showFailedDialog("Couldn't create file to upload: " + filepath);
					continue;
				}

                // FILESAFE_PREFIX / cognito user
                TransferObserver uploadObserver = transferUtility.upload(Constants.FILESAFE_PREFIX + "/" + username + "/" + uploadfile.getName(), uploadfile);

                transferIds[ctr] = uploadObserver.getId();
                completion[ctr] = 0;
                ctr++;

                // Attach a listener to the observer to get state update and progress notifications
                uploadObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (TransferState.COMPLETED == state) {


							for (int i=0; i<transferIds.length; i++) {
								if (id == transferIds[i] && tempFiles[i] != null) {

									//String loadedName = Constants.FILESAFE_PREFIX + "/" + username + "/" + tempFiles[i].getName();
									/*if (sS3Client.doesObjectExist(bucketName, loadedName)) {
										Log.i("FileSafe", "Object successfully uploaded.");
									} else {
										Log.i("FileSafe", "Can't find object");
									}*/

									if (tempFiles[i] != null) {
										try {
											tempFiles[i].delete();
										} catch (Exception e) {
											Log.e("FileSafe", "Error Deleting file: " + tempFiles[i].getAbsolutePath());
										}
									}
									tempFiles[i] = null;
									break;
								}
							}
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                        int percentDone = (int)percentDonef;

                        Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                                + " bytesTotal: " + bytesTotal + " " + percentDone + "%");

                        int totalDone = 0;
                        for (int i=0; i<completion.length; i++) {
                            if (id == transferIds[i]) {
                                completion[i] = percentDone;
                            }
                            totalDone = totalDone + completion[i];
                        }

                        int percentTotal = (totalDone*100) / totalForCompletion;
                        if (uploadDialog != null) {
                            uploadDialog.setProgress(percentTotal);
                        }
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e("YourActivity", "Error in upload of id" + id);
						for (int i=0; i<transferIds.length; i++) {
							if (id == transferIds[i] && tempFiles[i] != null) {
								try {
									tempFiles[i].delete();
								} catch (Exception e) {
									Log.e("FileSafe", "Error uploading file: " + tempFiles[i].getAbsolutePath());
								}
								tempFiles[i] = null;
								break;
							}
						}
                    }

                });

                //uploadObserver.getAbsoluteFilePath();

            } catch (AmazonS3Exception ase) {
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","Caught an AmazonS3Exception, " +
                        "which means your request made it " +
                        "to Amazon S3, but was rejected with an error response " +
                        "for some reason.");
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "Error Message:    " + ase.getMessage());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","HTTP Status Code: " + ase.getStatusCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","AWS Error Code:   " + ase.getErrorCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","Error Type:       " + ase.getErrorType());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","Request ID:       " + ase.getRequestId());
                showFailedDialog("Failed to retrieve profile list(2)!");
            } catch (AmazonServiceException ase) {
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","Caught an AmazonServiceException, " +
                        "which means your request made it " +
                        "to Amazon S3, but was rejected with an error response " +
                        "for some reason.");
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "Error Message:    " + ase.getMessage());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","HTTP Status Code: " + ase.getStatusCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","AWS Error Code:   " + ase.getErrorCode());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","Error Type:       " + ase.getErrorType());
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","Request ID:       " + ase.getRequestId());
            } catch (AmazonClientException ace) {
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "Caught an AmazonClientException, " +
                        "which means the client encountered " +
                        "an internal error while trying to communicate" +
                        " with S3, " +
                        "such as not being able to access the network.");
                com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER","Error Message: " + ace.getMessage());
                showFailedDialog("Failed to retrieve profile list(3)!");
            } catch (Exception e) {
                e.printStackTrace();
                showFailedDialog("Failed to retrieve profile list(9)!");
            }
        }

        //observers = transferUtility.getTransfersWithType(TransferType.UPLOAD);
	}

	/**
	 * GOOBER - strip off directory and extension return filename
	 *
	 * @param value
	 * @return
	 */
	private String stripProfileName(String value) {
		String tmpStringArray[] = value.split("/");

		if (tmpStringArray.length > 1) {
			String tmpString = tmpStringArray[tmpStringArray.length -1];
			return tmpString.substring(0, (tmpString.length() - ".ovpn".length()));
		} else {
			return tmpStringArray[0];
		}
	}

	/**
	 *
	 */
	AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
		@Override
		public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
			com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", " -- Auth Success");
			AppHelper.setCurrSession(cognitoUserSession);
			AppHelper.newDevice(device);
			// closeWaitDialog();

			// username/password is correct.  Now check if bucket exists
			if (organization != null) {
				if (doesBucketExist()) {
                    uploadFileSafe();
				} else {
					showFailedDialog("Failed to retrieve profile list(4)!");
					// log out of cognito
					logOut();
				}
			} else {
				// log out of cognito
				logOut();
			}
		}



		@Override
		public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
			// closeWaitDialog();
			Locale.setDefault(Locale.US);
			getUserAuthentication(authenticationContinuation, username);
		}

		private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
			//closeWaitDialog();
			if(username != null) {
				username = username;
				AppHelper.setUser(username);
			}
			AuthenticationDetails authenticationDetails = new AuthenticationDetails(username, password, null);
			continuation.setAuthenticationDetails(authenticationDetails);
			continuation.continueTask();
		}

		@Override
		public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
			// GOOBER
		}

		@Override
		public void onFailure(Exception e) {
			showFailedDialog("Failed to retrieve profile list(5)!");
		}

		@Override
		public void authenticationChallenge(ChallengeContinuation continuation) {
			/**
			 * For Custom authentication challenge, implement your logic to present challenge to the
			 * user and pass the user's responses to the continuation.
			 */
		}
	};

	// App methods
	// Logout of Cognito and display logout screen
	// This is actually cuplicate of logOut(View) but call
	// comes from function call in program.
	public void logOut() {
		// logout of Cognito
		cognitoCurrentUserSignout();

		// clear s3bucket client
		Util.clearS3Client(context);
	}

	private void cognitoCurrentUserSignout() {
		// logout of Cognito
		// sometimes if it's been too long, I believe pool doesn't
		// exists and user is no longer logged in
		CognitoUserPool userPool = AppHelper.getPool();
		if (userPool != null) {
			CognitoUser user = userPool.getCurrentUser();
			if (user != null) {
				user.signOut();
			}
		}
	}

	private void showFailedDialog(String body) {
		closeUploadDialog();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(body)
				.setTitle("FileSafe Error")
				.setCancelable(false)
				.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// dismiss this dialog
						dialog.dismiss();

						// show wait dialog
						showUploadDialog(context.getString(R.string.upload_filesafe_dialog_message));

						// since we don't know what went wrong, logout, get credentials and log back in
						logOut();
						getCognitoInfo();
						signInUser();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						uploadDialog.dismiss();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Close progress dialog
	 */
	private void closeUploadDialog() {
		if (uploadDialog != null){
			uploadDialog.dismiss();
		}
	}

	static private ProgressDialog uploadDialog;
	/**
	 * Display progress dialog
	 *
	 * @param message
	 */
	public void showUploadDialog(String message) {
		if (uploadDialog != null) {
			uploadDialog.dismiss();
		}
		uploadDialog = new ProgressDialog(context);
		uploadDialog.setMessage(message); // Setting Message
		uploadDialog.setTitle("Glacier FileSafe"); // Setting Title
        uploadDialog.setMax(100);
        uploadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		//uploadDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
		uploadDialog.show(); // Display Progress Dialog
		//uploadDialog.setIndeterminate(true);
		//uploadDialog.setCancelable(false);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (uploadDialog.getProgress() <= uploadDialog
							.getMax()) {
						Thread.sleep(200);
						//handle.sendMessage(handle.obtainMessage());
						if (uploadDialog.getProgress() == uploadDialog
								.getMax()) {
							uploadDialog.dismiss();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/*static Handler handle = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			uploadDialog.incrementProgressBy(1);
		}
	};*/

	/**
	 * ALF AM-277 File upload
	 */
	@SuppressLint("InflateParams")
	protected void fileSafeChooserDialog() {
		final Account account = activity.xmppConnectionService.getAccounts().get(0);
		if (account == null) {return;}

		final PresenceSelector.OnPresenceSelected callback = () -> {

			//Intent intent = new Intent();
			Intent intent = new Intent(Intent.ACTION_PICK);

			intent.setType("*/*");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			}
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setAction(Intent.ACTION_GET_CONTENT);

			if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
				Intent chooserIntent = Intent.createChooser(intent, getString(R.string.perform_action_with));
				startActivityForResult(chooserIntent, ConversationFragment.ATTACHMENT_CHOICE_CHOOSE_FILE);
			}
		};

		//if (cognitoAccount)
		callback.onPresenceSelected();
	}



	/**  GOOBER WIPE ALL  HISTORY  **/
	/**
	 * GOOBER - end ALL existing conversations
	 */
	@SuppressLint("InflateParams")
	protected void wipeAllHistoryDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.action_wipe_all_history));
		View dialogView = getActivity().getLayoutInflater().inflate(
				R.layout.dialog_wipe_all_history, null);
		final CheckBox deleteAllMessagesEachChatCheckBox = (CheckBox) dialogView
				.findViewById(R.id.delete_all_messages_each_chat_checkbox);
		final CheckBox endAllChatCheckBox = (CheckBox) dialogView
				.findViewById(R.id.end_all_chat_checkbox);
		final CheckBox deleteAllCachedFilesCheckBox = (CheckBox) dialogView
				.findViewById(R.id.delete_all_cached_files_checkbox);
		// default to true
		deleteAllMessagesEachChatCheckBox.setChecked(true);
		endAllChatCheckBox.setChecked(true);
		deleteAllCachedFilesCheckBox.setChecked(true);

		builder.setView(dialogView);
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.wipe_all_history), (dialog, which) -> {
			// go through each conversation and either delete or end each chat depending
			// on what was checked.
			for (int i = (conversations.size() - 1); i >= 0; i--) {

				// delete messages
				if (deleteAllMessagesEachChatCheckBox.isChecked()) {
					activity.xmppConnectionService.clearConversationHistory(conversations.get(i));
					((ConversationsActivity) getActivity()).onConversationsListItemUpdated();
					refresh();
				}

				// end chat
				if (endAllChatCheckBox.isChecked()) {
					//ALF AM-51, AM-64
					if (conversations.get(i).getMode() == Conversation.MODE_MULTI) {
						sendLeavingGroupMessage(conversations.get(i));
						// sleep required so message goes out before conversation thread stopped
						try { Thread.sleep(3000); } catch (InterruptedException ie) {}
					}
					activity.xmppConnectionService.archiveConversation(conversations.get(i));
				}
			}

			// delete everything in cache
			if (deleteAllCachedFilesCheckBox.isChecked()) {
				clearCachedFiles();
			}
		});
		builder.create().show();
	}

	/**
	 * Clear images, files from directory
	 */
	private void clearCachedFiles() {
		// clear images, etc
		clearLocalFiles();

		// clear images, etc
		clearPictures();

		// clear voice recordings from plugin
		clearVoiceRecordings();

		// clear shared location
		clearSharedLocations();

		// clear internal storage
		clearExternalStorage();
	}

	/**
	 * GOOBER - clear storage area
	 */
	private void clearExternalStorage() {
		FileBackend.removeStorageDirectory();
	}

	/**
	 * GOOBER - Clear pictures in Pictures/Messenger directory
	 */
	private void clearPictures() {
		// GOOBER - Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Pictures/Messenger";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// GOOBER - delete file
			for (int i = 0; i < fileDir.length; i++) {
				if (fileDir[i].delete()) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// GOOBER - Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
		}

		// GOOBER - Remove higher level files
		extStore = System.getenv("EXTERNAL_STORAGE") + "/Pictures";
		f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// GOOBER - delete file
			for (int i = 0; i < fileDir.length; i++) {
				// GOOBER - do not remove directory
				if ((!fileDir[i].isDirectory()) && (fileDir[i].delete())) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// GOOBER - Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
		}
	}

	/**
	 * GOOBER - Clear local files for Messenger
	 */
	private void clearLocalFiles() {
		// GOOBER - Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Messenger";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// GOOBER - delete file
			for (int i = 0; i < fileDir.length; i++) {
				// do not delete lollipin db
				if (!(fileDir[i].getName().startsWith("LollipinDB") || (fileDir[i].getName().startsWith("AppLockImpl"))) && (fileDir[i].delete())) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// GOOBER - Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
		}
	}

	/**
	 * GOOBER - Clear voice recordings
	 */
	private void clearVoiceRecordings() {
		// GOOBER - Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Voice Recorder";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// GOOBER - delete file
			for (int i = 0; i < fileDir.length; i++) {
				if (fileDir[i].delete()) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
				} else {
					com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
				}
			}

			// GOOBER - Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			callBroadcast(deletedFiles);
		}
	}

	/**
	 * GOOBER - Clear shared locations
	 */
	private void clearSharedLocations() {
		// GOOBER - Retrieve directory
		//String extStore = System.getenv("EXTERNAL_STORAGE") + "/Android/data/com.glaciersecurity.glaciermessenger.sharelocation/cache";
		ArrayList<String> deletedFiles = new ArrayList<String>();
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Android/data/com.glaciersecurity.glaciermessenger.sharelocation";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {

			String extStore2 = extStore + "/cache";
			File f_exts2 = new File(extStore2);

			if (f_exts2.exists()) {
				File[] fileDir = f_exts2.listFiles();

				// GOOBER - delete file
				for (int i = 0; i < fileDir.length; i++) {
					if (fileDir[i].delete()) {
						deletedFiles.add(fileDir[i].toString());
						com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Successfully deleted " + fileDir[i]);
					} else {
						com.glaciersecurity.glaciermessenger.utils.Log.d("GOOBER", "File list: Did not delete " + fileDir[i]);
					}
				}
				if (f_exts2.delete()) {
					deletedFiles.add(f_exts2.toString());
				}
			}

			if (f_exts.delete()) {
				deletedFiles.add(f_exts.toString());
			}

			// GOOBER - Need to do something to update after deleting
			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			String[] stringArray = deletedFiles.toArray(new String[0]);
			callBroadcast(deletedFiles.toArray(new String[0]));
		}
	}

	/**
	 * GOOBER - Notify everyone of file status change.  Must send specific files (not necessarily directories.
	 */
	private void callBroadcast(String[] files) {
		if (Build.VERSION.SDK_INT >= 14) {
			com.glaciersecurity.glaciermessenger.utils.Log.d("-->", " >= 14");
			MediaScannerConnection.scanFile(getActivity(), files, null, new MediaScannerConnection.OnScanCompletedListener() {
				/*
				 *   (non-Javadoc)
				 * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
				 */
				public void onScanCompleted(String path, Uri uri) {
					com.glaciersecurity.glaciermessenger.utils.Log.d("ExternalStorage", "Scanned " + path + ":");
					com.glaciersecurity.glaciermessenger.utils.Log.d("ExternalStorage", "-> uri=" + uri);
				}
			});
		} else {
			com.glaciersecurity.glaciermessenger.utils.Log.d("-->", " < 14");
			getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://" + Environment.getExternalStorageDirectory())));
		}
	}
}
