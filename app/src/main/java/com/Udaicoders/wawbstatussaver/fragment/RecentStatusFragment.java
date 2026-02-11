package com.Udaicoders.wawbstatussaver.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.Udaicoders.wawbstatussaver.R;
import com.Udaicoders.wawbstatussaver.adapter.RecentAdapter;
import com.Udaicoders.wawbstatussaver.model.StatusModel;
import com.Udaicoders.wawbstatussaver.util.AdController;
import com.Udaicoders.wawbstatussaver.util.SharedPrefs;
import com.Udaicoders.wawbstatussaver.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentStatusFragment extends Fragment implements RecentAdapter.OnCheckboxListener {

    private static final String ARG_FILTER_TYPE = "filter_type";
    public static final String FILTER_IMAGES = "images";
    public static final String FILTER_VIDEOS = "videos";
    static String TAG = "resultCheck";

    private String filterType;

    GridView imageGrid;
    ArrayList<StatusModel> f = new ArrayList<>();
    RecentAdapter myAdapter;
    ArrayList<StatusModel> filesToDelete = new ArrayList<>();
    LinearLayout actionLay, downloadIV, deleteIV;
    CheckBox selectAll;
    RelativeLayout loaderLay, emptyLay;
    SwipeRefreshLayout swipeToRefresh;
    LinearLayout sAccessBtn;

    int REQUEST_WA_TREE = 101;
    int REQUEST_WB_TREE = 1001;
    boolean dataLoaded = false;

    public boolean isDataLoaded() {
        return dataLoaded;
    }

    public static RecentStatusFragment newInstance(String filterType) {
        RecentStatusFragment fragment = new RecentStatusFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILTER_TYPE, filterType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filterType = getArguments().getString(ARG_FILTER_TYPE, FILTER_IMAGES);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recent_fragment, container, false);

        loaderLay = rootView.findViewById(R.id.loaderLay);
        emptyLay = rootView.findViewById(R.id.emptyLay);

        imageGrid = rootView.findViewById(R.id.WorkImageGrid);

        swipeToRefresh = rootView.findViewById(R.id.swipeToRefresh);
        swipeToRefresh.setOnRefreshListener(() -> {
            if (hasAnyTreeAccess()) {
                for (StatusModel deletedFile : filesToDelete) {
                    f.contains(deletedFile.selected = false);
                }
                if (myAdapter != null) {
                    myAdapter.notifyDataSetChanged();
                }
                filesToDelete.clear();
                selectAll.setChecked(false);
                actionLay.setVisibility(View.GONE);
                populateGrid();
            }
            swipeToRefresh.setRefreshing(false);
        });

        actionLay = rootView.findViewById(R.id.actionLay);
        deleteIV = rootView.findViewById(R.id.deleteIV);
        deleteIV.setOnClickListener(view -> {
            if (!filesToDelete.isEmpty()) {
                new AlertDialog.Builder(getContext())
                        .setMessage(getResources().getString(R.string.delete_alert))
                        .setCancelable(true)
                        .setNegativeButton(getResources().getString(R.string.yes), (dialogInterface, i) -> {
                            new deleteAll().execute();
                        })
                        .setPositiveButton(getResources().getString(R.string.no), (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        }).create().show();
            }
        });

        downloadIV = rootView.findViewById(R.id.downloadIV);
        downloadIV.setOnClickListener(view -> {
            new downloadAll().execute();
        });

        selectAll = rootView.findViewById(R.id.selectAll);
        selectAll.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!compoundButton.isPressed()) {
                return;
            }

            filesToDelete.clear();

            for (int i = 0; i < f.size(); i++) {
                if (!f.get(i).selected) {
                    b = true;
                    break;
                }
            }

            if (b) {
                for (int i = 0; i < f.size(); i++) {
                    f.get(i).selected = true;
                    filesToDelete.add(f.get(i));
                }
                selectAll.setChecked(true);
            } else {
                for (int i = 0; i < f.size(); i++) {
                    f.get(i).selected = false;
                }
                actionLay.setVisibility(View.GONE);
            }
            myAdapter.notifyDataSetChanged();
        });

        sAccessBtn = rootView.findViewById(R.id.sAccessBtn);
        sAccessBtn.setOnClickListener(v -> {
            boolean waInstalled = Utils.appInstalledOrNot(getActivity(), "com.whatsapp");
            boolean wbInstalled = Utils.appInstalledOrNot(getActivity(), "com.whatsapp.w4b");
            boolean waGranted = !SharedPrefs.getWATree(getActivity()).equals("");
            boolean wbGranted = !SharedPrefs.getWBTree(getActivity()).equals("");

            if (waInstalled && !waGranted) {
                launchSAFIntent(getWhatsAppFolder(), REQUEST_WA_TREE);
            } else if (wbInstalled && !wbGranted) {
                launchSAFIntent(getWABusinessFolder(), REQUEST_WB_TREE);
            } else if (!waInstalled && !wbInstalled) {
                Toast.makeText(getActivity(), "Please install WhatsApp to download statuses!", Toast.LENGTH_SHORT).show();
            }
        });

        if (hasAnyTreeAccess()) {
            populateGrid();
        }

        return rootView;
    }

    private boolean hasAnyTreeAccess() {
        return !SharedPrefs.getWATree(getActivity()).equals("")
                || !SharedPrefs.getWBTree(getActivity()).equals("");
    }

    private void launchSAFIntent(String statusDir, int requestCode) {
        StorageManager sm = (StorageManager) getActivity().getSystemService(Context.STORAGE_SERVICE);
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
            Uri uri = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");
            String scheme = uri.toString();
            scheme = scheme.replace("/root/", "/document/");
            scheme += "%3A" + statusDir;
            uri = Uri.parse(scheme);
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.putExtra("android.provider.extra.INITIAL_URI",
                    Uri.parse("content://com.android.externalstorage.documents/document/primary%3A" + statusDir));
        }

        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        startActivityForResult(intent, requestCode);
    }

    private String getWhatsAppFolder() {
        if (new File(Environment.getExternalStorageDirectory() + File.separator
                + "Android/media/com.whatsapp/WhatsApp" + File.separator + "Media" + File.separator + ".Statuses").isDirectory()) {
            return "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses";
        } else {
            return "WhatsApp%2FMedia%2F.Statuses";
        }
    }

    private String getWABusinessFolder() {
        if (new File(Environment.getExternalStorageDirectory() + File.separator
                + "Android/media/com.whatsapp.w4b/WhatsApp Business" + File.separator + "Media" + File.separator + ".Statuses").isDirectory()) {
            return "Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp Business%2FMedia%2F.Statuses";
        } else {
            return "WhatsApp Business%2FMedia%2F.Statuses";
        }
    }

    // --- Delete selected statuses ---
    class deleteAll extends AsyncTask<Void, Void, Void> {
        int success = -1;
        AlertDialog alertDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            alertDialog = Utils.loadingPopup(getActivity());
            alertDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<StatusModel> deletedFiles = new ArrayList<>();
            for (int i = 0; i < filesToDelete.size(); i++) {
                StatusModel details = filesToDelete.get(i);
                DocumentFile fromTreeUri = DocumentFile.fromSingleUri(getActivity(), Uri.parse(details.getFilePath()));
                if (fromTreeUri.exists()) {
                    if (fromTreeUri.delete()) {
                        deletedFiles.add(details);
                        if (success == 0) {
                            break;
                        }
                        success = 1;
                    } else {
                        success = 0;
                    }
                } else {
                    success = 0;
                }
            }
            filesToDelete.clear();
            for (StatusModel deletedFile : deletedFiles) {
                f.remove(deletedFile);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            myAdapter.notifyDataSetChanged();
            if (success == 0) {
                Toast.makeText(getContext(), getResources().getString(R.string.delete_error), Toast.LENGTH_SHORT).show();
            } else if (success == 1) {
                Toast.makeText(getActivity(), getResources().getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
            }
            actionLay.setVisibility(View.GONE);
            selectAll.setChecked(false);
            alertDialog.dismiss();
        }
    }

    // --- Download selected statuses ---
    class downloadAll extends AsyncTask<Void, Void, Void> {
        AlertDialog alertDialog;
        int success = -1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            alertDialog = Utils.loadingPopup(getActivity());
            alertDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!filesToDelete.isEmpty()) {
                ArrayList<StatusModel> downloadedFiles = new ArrayList<>();
                for (int i = 0; i < filesToDelete.size(); i++) {
                    StatusModel details = filesToDelete.get(i);
                    DocumentFile fromTreeUri = DocumentFile.fromSingleUri(getActivity(), Uri.parse(details.getFilePath()));
                    if (fromTreeUri.exists()) {
                        if (Utils.download(getActivity(), details.getFilePath())) {
                            downloadedFiles.add(details);
                            if (success == 0) {
                                break;
                            }
                            success = 1;
                        } else {
                            success = 0;
                        }
                    } else {
                        success = 0;
                    }
                }
                filesToDelete.clear();
                for (StatusModel downloadedFile : downloadedFiles) {
                    f.contains(downloadedFile.selected = false);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            myAdapter.notifyDataSetChanged();
            if (success == 0) {
                Toast.makeText(getContext(), getResources().getString(R.string.save_error), Toast.LENGTH_SHORT).show();
            } else if (success == 1) {
                Toast.makeText(getActivity(), getResources().getString(R.string.save_success), Toast.LENGTH_SHORT).show();
            }
            actionLay.setVisibility(View.GONE);
            selectAll.setChecked(false);
            alertDialog.dismiss();

            if (AdController.isLoadIronSourceAd) {
                AdController.ironShowInterstitial(getActivity(), null, 0);
            } else {
                AdController.showInterAd(getActivity(), null, 0);
            }
        }
    }

    // --- Load data ---
    loadDataAsync async;

    public void populateGrid() {
        async = new loadDataAsync();
        async.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (async != null) {
            async.cancel(true);
        }
    }

    class loadDataAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loaderLay.setVisibility(View.VISIBLE);
            imageGrid.setVisibility(View.GONE);
            sAccessBtn.setVisibility(View.GONE);
            emptyLay.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            f = new ArrayList<>();
            ArrayList<DocumentFile> allFilesList = new ArrayList<>();

            // Load from WhatsApp directory
            String waTreeUri = SharedPrefs.getWATree(getActivity());
            if (!waTreeUri.equals("")) {
                try {
                    DocumentFile waDir = DocumentFile.fromTreeUri(
                            requireContext().getApplicationContext(), Uri.parse(waTreeUri));
                    if (waDir != null && waDir.exists() && waDir.isDirectory()
                            && waDir.canRead() && waDir.canWrite()) {
                        DocumentFile[] waFiles = waDir.listFiles();
                        if (waFiles != null) {
                            Collections.addAll(allFilesList, waFiles);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Load from WA Business directory
            String wbTreeUri = SharedPrefs.getWBTree(getActivity());
            if (!wbTreeUri.equals("")) {
                try {
                    DocumentFile wbDir = DocumentFile.fromTreeUri(
                            requireContext().getApplicationContext(), Uri.parse(wbTreeUri));
                    if (wbDir != null && wbDir.exists() && wbDir.isDirectory()
                            && wbDir.canRead() && wbDir.canWrite()) {
                        DocumentFile[] wbFiles = wbDir.listFiles();
                        if (wbFiles != null) {
                            Collections.addAll(allFilesList, wbFiles);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Sort combined list by last modified (newest first)
            Collections.sort(allFilesList,
                    (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));

            // Filter by type and exclude .nomedia
            for (DocumentFile file : allFilesList) {
                String uri = file.getUri().toString();
                if (uri.contains(".nomedia")) continue;

                String mimeType = file.getType();
                if (FILTER_IMAGES.equals(filterType)) {
                    if (mimeType != null && mimeType.startsWith("image")) {
                        f.add(new StatusModel(uri));
                    }
                } else if (FILTER_VIDEOS.equals(filterType)) {
                    if (mimeType != null && mimeType.startsWith("video")) {
                        f.add(new StatusModel(uri));
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (getActivity() != null) {
                if (f != null && f.size() != 0) {
                    myAdapter = new RecentAdapter(RecentStatusFragment.this, f, RecentStatusFragment.this);
                    imageGrid.setAdapter(myAdapter);
                    imageGrid.setVisibility(View.VISIBLE);
                }
                dataLoaded = true;
                loaderLay.setVisibility(View.GONE);
            }

            if (f == null || f.size() == 0) {
                emptyLay.setVisibility(View.VISIBLE);
            } else {
                emptyLay.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (myAdapter != null) {
            myAdapter.onActivityResult(requestCode, resultCode, data);
        }

        // Refresh after returning from PreviewActivity
        if (requestCode == 10 && resultCode == 10) {
            populateGrid();
            actionLay.setVisibility(View.GONE);
            selectAll.setChecked(false);
        }

        // Handle SAF permission grants
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            // URI logging removed for security
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (requestCode == REQUEST_WA_TREE) {
                SharedPrefs.setWATree(getActivity(), uri.toString());
                // Chain to WB if installed and not yet granted
                if (Utils.appInstalledOrNot(getActivity(), "com.whatsapp.w4b")
                        && SharedPrefs.getWBTree(getActivity()).equals("")) {
                    launchSAFIntent(getWABusinessFolder(), REQUEST_WB_TREE);
                } else {
                    populateGrid();
                }
            } else if (requestCode == REQUEST_WB_TREE) {
                SharedPrefs.setWBTree(getActivity(), uri.toString());
                populateGrid();
            }
        }
    }

    @Override
    public void onCheckboxListener(View view, List<StatusModel> list) {
        filesToDelete.clear();
        for (StatusModel details : list) {
            if (details.isSelected()) {
                filesToDelete.add(details);
            }
        }
        if (filesToDelete.size() == f.size()) {
            selectAll.setChecked(true);
        }
        if (!filesToDelete.isEmpty()) {
            actionLay.setVisibility(View.VISIBLE);
            return;
        }
        selectAll.setChecked(false);
        actionLay.setVisibility(View.GONE);
    }
}
