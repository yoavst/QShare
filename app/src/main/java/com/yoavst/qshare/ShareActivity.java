package com.yoavst.qshare;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.lge.qpair.api.r1.IPeerContext;
import com.lge.qpair.api.r1.IPeerIntent;
import com.lge.qpair.api.r1.QPairConstants;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Yoav.
 */
public class ShareActivity extends Activity {
	public static final String EXTRA_WHAT_TO_SHARE = "what_to_share";
	public static final String EXTRA_FILENAME = "filename";
	BroadcastReceiver callbackReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// retrieve the error cause
			String errorMessage =
					intent.getStringExtra(QPairConstants.EXTRA_CAUSE);
			Toast.makeText(ShareActivity.this, errorMessage, Toast.LENGTH_LONG).show();
			Log.e("TAG", errorMessage);
			ShareActivity.this.finish();
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity);
		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		if (Intent.ACTION_SEND.equals(action) && type != null) {
			if ("text/plain".equals(type)) {
				handleSendText(intent); // Handle text being sent
			} else if (type.startsWith("image/")) {
				handleSendImage(intent); // Handle single image being sent
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
			if (type.startsWith("image/")) {
				handleSendMultipleImages(intent); // Handle multiple images being sent
			}
		}
	}

	void handleSendText(Intent intent) {
		final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (sharedText != null) {
			connectQPair(new OnQPairConnectedListener() {
				@Override
				public void onConnect(IPeerContext context) {
					try {
						IPeerIntent intent = context.newPeerIntent();
						intent.setClassName("com.yoavst.qshare","com.yoavst.qshare.ShareService");
						intent.putStringExtra(Intent.EXTRA_TEXT, sharedText);
						intent.putStringExtra(EXTRA_WHAT_TO_SHARE, "text");
						IPeerIntent callback = context.newPeerIntent();
						callback.setAction("custom.callback");
						context.startServiceOnPeer(intent, callback);
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(ShareActivity.this, "Error! " + e.getMessage(), Toast.LENGTH_LONG).show();
					} finally {
						finish();
					}
				}
			});
		}
	}

	void handleSendImage(Intent intent) {
		final Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (imageUri != null) {
			connectQPair(new OnQPairConnectedListener() {
				@Override
				public void onConnect(IPeerContext context) {
					try {
						String path = getRealPathFromURI(imageUri);
						IPeerIntent intent = context.newPeerIntent();
						intent.setClassName("com.yoavst.qshare","com.yoavst.qshare.ShareService");
						intent.setDataAndType(path, "image/*");
						intent.putStringExtra(EXTRA_WHAT_TO_SHARE, "image");
						intent.putStringExtra(EXTRA_FILENAME, new File(path).getName());
						IPeerIntent callback = context.newPeerIntent();
						callback.setAction("custom.callback");
						context.startServiceOnPeerWithFile(intent, "sharedFiles" +  File.separator + new File(path).getName(), callback);
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(ShareActivity.this, "Error! " + e.getMessage(), Toast.LENGTH_LONG).show();
					} finally {
						finish();
					}
				}
			});
		}
	}

	void handleSendMultipleImages(Intent intent) {
		ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
		if (imageUris != null) {
			// Update UI to reflect multiple images being shared
		}
	}

	void connectQPair(OnQPairConnectedListener listener) {
		Intent intent = new Intent(QPairConstants.ACTION_QPAIR_SERVICE);
		if (!bindService(intent, new QPairServiceConnection(listener), 0)) {
			Toast.makeText(this, "Error with QPair!", Toast.LENGTH_SHORT).show();
			finish();
		}

	}

	public class QPairServiceConnection implements ServiceConnection {
		OnQPairConnectedListener listener;

		public QPairServiceConnection(OnQPairConnectedListener listener) {
			this.listener = listener;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			listener.onConnect(IPeerContext.Stub.asInterface(service));
			unbindService(this);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	}

	interface OnQPairConnectedListener {
		void onConnect(IPeerContext context);

	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(callbackReceiver,
				new IntentFilter("custom.callback"));
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(callbackReceiver);
	}

	private String getRealPathFromURI(Uri contentURI) {
		String result;
		Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
		if (cursor == null) { // Source is Dropbox or other similar local file path
			result = contentURI.getPath();
		} else {
			cursor.moveToFirst();
			int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
			result = cursor.getString(idx);
			cursor.close();
		}
		return result;
	}
}
