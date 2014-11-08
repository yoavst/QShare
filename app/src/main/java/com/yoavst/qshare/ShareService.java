package com.yoavst.qshare;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Yoav.
 */
public class ShareService extends IntentService {
	private static final File LOG_FILE = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "logging.txt");
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm");

	public ShareService() {
		super("shareService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		log(intent.getStringExtra(ShareActivity.EXTRA_WHAT_TO_SHARE));
		switch (intent.getStringExtra(ShareActivity.EXTRA_WHAT_TO_SHARE)) {
			case "text":
				shareText(intent);
				break;
			case "image":
				shareImage(intent);
				break;
		}
	}

	void shareText(Intent original) {
		log(original.getStringExtra(Intent.EXTRA_TEXT));
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, original.getStringExtra(Intent.EXTRA_TEXT));
		startActivity(Intent.createChooser(intent, "Share").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	void shareImage(Intent original) {
		Uri imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
				+ "sharedFiles" + File.separator + original.getStringExtra(ShareActivity.EXTRA_FILENAME)));
		log(imageUri.toString());
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("image/*");
		intent.putExtra(Intent.EXTRA_STREAM, imageUri);
		startActivity(Intent.createChooser(intent, "Share").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	void log(String text) {
		try {
			FileUtils.writeStringToFile(LOG_FILE, FORMATTER.format(new Date()) + ": " + text + "\n", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
