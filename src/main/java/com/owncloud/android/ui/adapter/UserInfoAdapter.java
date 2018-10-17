package com.owncloud.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.owncloud.android.databinding.UserInfoDetailsTableItemBinding;
import com.owncloud.android.ui.components.UserInfoDetailsItem;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder> {
    private List<UserInfoDetailsItem> displayList;

    public UserInfoAdapter(List<UserInfoDetailsItem> displayList) {
        this.displayList = displayList == null ? new LinkedList<>() : displayList;
    }

    public void setData(List<UserInfoDetailsItem> displayList) {
        this.displayList = displayList == null ? new LinkedList<>() : displayList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserInfoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        UserInfoDetailsTableItemBinding binding = UserInfoDetailsTableItemBinding.inflate(layoutInflater, parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserInfoDetailsItem item = displayList.get(position);

        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final UserInfoDetailsTableItemBinding binding;

        public ViewHolder(UserInfoDetailsTableItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(UserInfoDetailsItem item) {
            binding.setUserInfoDetailsItem(item);
            binding.executePendingBindings();
        }
    }

    @BindingAdapter({"android:src"})
    public static void setImageViewResource(ImageView imageView, int resource) {
        imageView.setImageResource(resource);
    }
}
