package com.Udaicoders.wawbstatussaver.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.Udaicoders.wawbstatussaver.PreviewActivity;
import com.Udaicoders.wawbstatussaver.R;
import com.Udaicoders.wawbstatussaver.VideoPlayerActivity;
import com.Udaicoders.wawbstatussaver.model.StatusModel;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class MyStatusAdapter extends BaseAdapter {

    Fragment context;
    List<StatusModel> arrayList;
    int tileSize;
    LayoutInflater inflater;
    public OnCheckboxListener onCheckboxListener;
    RequestOptions glideOptions;

    public MyStatusAdapter(Fragment context, List<StatusModel> arrayList, OnCheckboxListener onCheckboxListener) {
        this.context = context;
        this.arrayList = arrayList;

        inflater = (LayoutInflater) context.getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        DisplayMetrics displayMetrics = context.getResources()
                .getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        tileSize = (width - (int)(16 * displayMetrics.density)) / 2;

        glideOptions = new RequestOptions()
                .override(tileSize, tileSize)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        this.onCheckboxListener = onCheckboxListener;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int arg0) {
        return arg0;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    static class ViewHolder {
        ImageView gridImageVideo;
        ImageView play;
        CheckBox checkbox;
    }

    @Override
    public View getView(final int arg0, View convertView, ViewGroup arg2) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_my_status, null);
            convertView.setLayoutParams(new GridView.LayoutParams(tileSize, tileSize));
            holder = new ViewHolder();
            holder.gridImageVideo = convertView.findViewById(R.id.gridImageVideo);
            holder.play = convertView.findViewById(R.id.play);
            holder.checkbox = convertView.findViewById(R.id.checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final StatusModel item = arrayList.get(arg0);
        boolean isVideo = isVideoFile(item.getFilePath());

        holder.play.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        Glide.with(context.getActivity())
                .load(item.getFilePath())
                .apply(glideOptions)
                .thumbnail(0.1f)
                .into(holder.gridImageVideo);

        // Prevent recycled checkbox from triggering listener
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(item.isSelected());
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setSelected(isChecked);
            if (onCheckboxListener != null) {
                onCheckboxListener.onCheckboxListener(buttonView, arrayList);
            }
        });

        convertView.setOnClickListener(view -> {
            if (isVideo) {
                Intent videoIntent = new Intent(context.getActivity(), VideoPlayerActivity.class);
                videoIntent.putExtra("videoUri", item.getFilePath());
                videoIntent.putExtra("isDownloaded", true);
                context.startActivityForResult(videoIntent, 10);
            } else {
                Intent intent = new Intent(context.getActivity(), PreviewActivity.class);
                intent.putParcelableArrayListExtra("images", (ArrayList<? extends Parcelable>) arrayList);
                intent.putExtra("position", arg0);
                intent.putExtra("statusdownload", "download");
                context.startActivityForResult(intent, 10);
            }
        });

        return convertView;
    }


    public interface OnCheckboxListener {
        void onCheckboxListener(View view, List<StatusModel> list);
    }

    public boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MyAdapter", "onActivityResult");
    }
}
