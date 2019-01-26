package com.wojciechkolendo.applock.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.models.AppInfo;


/**
 * App adapter
 */

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

	public static final int TYPE_PROTECTED = 100;
	public static final int TYPE_UNPROTECTED = 101;

	private Context context;

	private int type;

	private OnClickCallback mCallback;

	private List<AppInfo> appInfoList;

	static class ViewHolder extends RecyclerView.ViewHolder {
		ImageView appIconView;
		TextView appNameView;
		TextView appPackageNameView;
		CheckBox appCheckedView;

		ViewHolder(View view) {
			super(view);
			appIconView = view.findViewById(R.id.app_icon);
			appNameView = view.findViewById(R.id.app_name);
			appPackageNameView = view.findViewById(R.id.app_package_name);
			appCheckedView = view.findViewById(R.id.app_checked);
		}
	}

	public AppAdapter(int type, List<AppInfo> list, OnClickCallback callback) {
		this.type = type;
		this.appInfoList = list;
		this.mCallback = callback;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (context == null) {
			context = parent.getContext();
		}

		View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
		final ViewHolder holder = new ViewHolder(view);

		holder.appCheckedView.setOnClickListener(view1 -> {
			AppInfo appInfo = appInfoList.get(holder.getAdapterPosition());
			mCallback.onClick(appInfo);
		});

		return holder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		AppInfo info = appInfoList.get(position);
		holder.appIconView.setImageDrawable(info.getAppIcon());
		holder.appNameView.setText(info.getAppName());
		holder.appPackageNameView.setText(info.getAppPackageName());
		if (type == TYPE_PROTECTED) {
			holder.appCheckedView.setChecked(true);
		} else if (type == TYPE_UNPROTECTED) {
			holder.appCheckedView.setChecked(false);
		}
	}

	@Override
	public int getItemCount() {
		return appInfoList.size();
	}

	public interface OnClickCallback {
		void onClick(AppInfo info);
	}
}
