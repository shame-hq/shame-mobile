package com.shame.tracker;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class UpdateManager {

    // TODO: REPLACE THIS URL WITH YOUR ACTUAL GITHUB URL
    private static final String VERSION_JSON_URL = "https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/version.json";

    private final Context context;
    private long downloadId = -1;

    public UpdateManager(Context context) {
        this.context = context;
    }

    public void checkForUpdates() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com/") // Base URL is required but ignored if @GET has full URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        UpdateService service = retrofit.create(UpdateService.class);
        Call<VersionInfo> call = service.getVersionInfo(VERSION_JSON_URL);

        call.enqueue(new Callback<VersionInfo>() {
            @Override
            public void onResponse(Call<VersionInfo> call, Response<VersionInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    checkVersion(response.body());
                }
            }

            @Override
            public void onFailure(Call<VersionInfo> call, Throwable t) {
                // Silent fail or log
            }
        });
    }

    private void checkVersion(VersionInfo remoteVersion) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int currentVersionCode = pInfo.versionCode;

            if (remoteVersion.versionCode > currentVersionCode) {
                showUpdateDialog(remoteVersion.downloadUrl);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showUpdateDialog(String downloadUrl) {
        new AlertDialog.Builder(context)
                .setTitle("New Update Available")
                .setMessage("A new version of Shame Tracker is available. Would you like to update?")
                .setPositiveButton("Update", (dialog, which) -> downloadApk(downloadUrl))
                .setNegativeButton("Later", null)
                .show();
    }

    private void downloadApk(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading Update");
        request.setDescription("Downloading updated Shame Tracker APK");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk");
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            downloadId = manager.enqueue(request);
            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show();

            // Register receiver for download complete
            context.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                installApk();
                context.unregisterReceiver(this);
            }
        }
    };

    private void installApk() {
        File apkFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");

        if (apkFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", apkFile);

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Update file not found", Toast.LENGTH_SHORT).show();
        }
    }

    // Interfaces for JSON parsing
    interface UpdateService {
        @GET
        Call<VersionInfo> getVersionInfo(@retrofit2.http.Url String url);
    }

    class VersionInfo {
        int versionCode;
        String downloadUrl;
    }
}
