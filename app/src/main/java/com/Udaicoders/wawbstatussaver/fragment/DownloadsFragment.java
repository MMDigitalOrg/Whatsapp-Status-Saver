package com.Udaicoders.wawbstatussaver.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import androidx.fragment.app.Fragment;

import com.Udaicoders.wawbstatussaver.R;
import com.Udaicoders.wawbstatussaver.adapter.MyStatusAdapter;
import com.Udaicoders.wawbstatussaver.model.StatusModel;
import com.Udaicoders.wawbstatussaver.util.AdController;
import com.Udaicoders.wawbstatussaver.util.Utils;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloadsFragment extends Fragment implements MyStatusAdapter.OnCheckboxListener {

    private static final String ARG_FILTER_TYPE = "filter_type";
    public static final String FILTER_IMAGES = "images";
    public static final String FILTER_VIDEOS = "videos";
    public static final String FILTER_ALL = "all";

    private String filterType;

    GridView gridView;
    ArrayList<StatusModel> f = new ArrayList<>();
    MyStatusAdapter myAdapter;
    int save = 10;
    ArrayList<StatusModel> filesToDelete = new ArrayList<>();
    LinearLayout actionLay, deleteIV;
    CheckBox selectAll;
    RelativeLayout loaderLay, emptyLay;

    public static DownloadsFragment newInstance(String filterType) {
        DownloadsFragment fragment = new DownloadsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILTER_TYPE, filterType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filterType = getArguments().getString(ARG_FILTER_TYPE, FILTER_ALL);
        } else {
            filterType = FILTER_ALL;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.my_status_fragment, container, false);

        loaderLay = rootView.findViewById(R.id.loaderLay);
        emptyLay = rootView.findViewById(R.id.emptyLay);

        gridView = rootView.findViewById(R.id.videoGrid);
        populateGrid();

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

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Always refresh when returning to this fragment (e.g. after deleting in PreviewActivity/VideoPlayerActivity)
        if (myAdapter != null) {
            getFromSdcard();
            myAdapter = new MyStatusAdapter(DownloadsFragment.this, f, DownloadsFragment.this);
            gridView.setAdapter(myAdapter);

            if (f == null || f.size() == 0) {
                emptyLay.setVisibility(View.VISIBLE);
            } else {
                emptyLay.setVisibility(View.GONE);
            }
        }
    }

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
                File file = new File(details.getFilePath());
                if (file.exists()) {
                    if (file.delete()) {
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

    public void populateGrid() {
        new loadDataAsync().execute();
    }

    class loadDataAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loaderLay.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            getFromSdcard();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (getActivity() != null) {
                myAdapter = new MyStatusAdapter(DownloadsFragment.this, f, DownloadsFragment.this);
                gridView.setAdapter(myAdapter);
                loaderLay.setVisibility(View.GONE);

                if (f == null || f.size() == 0) {
                    emptyLay.setVisibility(View.VISIBLE);
                } else {
                    emptyLay.setVisibility(View.GONE);
                }
            }
        }
    }

    public void getFromSdcard() {
        String basePath = Environment.getExternalStorageDirectory().toString()
                + File.separator + "Download"
                + File.separator + getResources().getString(R.string.app_name);

        ArrayList<File> allFiles = new ArrayList<>();

        if (FILTER_IMAGES.equals(filterType) || FILTER_ALL.equals(filterType)) {
            File imagesDir = new File(basePath + "/Images");
            if (imagesDir.isDirectory()) {
                File[] imgFiles = imagesDir.listFiles();
                if (imgFiles != null) allFiles.addAll(Arrays.asList(imgFiles));
            }
        }
        if (FILTER_VIDEOS.equals(filterType) || FILTER_ALL.equals(filterType)) {
            File videosDir = new File(basePath + "/Videos");
            if (videosDir.isDirectory()) {
                File[] vidFiles = videosDir.listFiles();
                if (vidFiles != null) allFiles.addAll(Arrays.asList(vidFiles));
            }
        }

        File[] filesArray = allFiles.toArray(new File[0]);
        Arrays.sort(filesArray, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

        f = new ArrayList<>();
        for (File file : filesArray) {
            f.add(new StatusModel(file.getAbsolutePath()));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (myAdapter != null) {
            myAdapter.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == save && resultCode == save) {
            myAdapter.notifyDataSetChanged();

            getFromSdcard();
            myAdapter = new MyStatusAdapter(DownloadsFragment.this, f, DownloadsFragment.this);
            gridView.setAdapter(myAdapter);

            actionLay.setVisibility(View.GONE);
            selectAll.setChecked(false);
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
