package com.wangdaye.mysplash._common.ui.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash._common.data.entity.ChangeCollectionPhotoResult;
import com.wangdaye.mysplash._common.data.entity.Collection;
import com.wangdaye.mysplash._common.data.entity.LikePhotoResult;
import com.wangdaye.mysplash._common.data.entity.Photo;
import com.wangdaye.mysplash._common.data.entity.User;
import com.wangdaye.mysplash._common.data.service.PhotoService;
import com.wangdaye.mysplash._common.utils.DisplayUtils;
import com.wangdaye.mysplash._common.utils.helper.IntentHelper;
import com.wangdaye.mysplash._common.utils.manager.AuthManager;
import com.wangdaye.mysplash._common.ui.dialog.DeleteCollectionPhotoDialog;
import com.wangdaye.mysplash._common.ui.dialog.RateLimitDialog;
import com.wangdaye.mysplash._common.ui.dialog.SelectCollectionPhotoDialog;
import com.wangdaye.mysplash._common.ui.widget.freedomSizeView.FreedomImageView;
import com.wangdaye.mysplash._common.ui.widget.LikeImageButton;
import com.wangdaye.mysplash._common.utils.AnimUtils;
import com.wangdaye.mysplash._common.utils.ColorUtils;
import com.wangdaye.mysplash._common.ui.activity.LoginActivity;
import com.wangdaye.mysplash.collection.view.activity.CollectionActivity;
import com.wangdaye.mysplash.me.view.activity.MeActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Photos adapter. (Recycler view)
 * */

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder>
        implements SelectCollectionPhotoDialog.OnCollectionsChangedListener,
        DeleteCollectionPhotoDialog.OnDeleteCollectionListener {
    // widget
    private Context a;
    private List<Photo> itemList;
    private PhotoService service;

    // data
    private boolean own = false;
    private boolean inMyCollection = false;

    /** <br> life cycle. */

    public PhotoAdapter(Context a, List<Photo> list) {
        this.a = a;
        this.itemList = list;
    }

    /** <br> UI. */

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(v, viewType);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.title.setText("");
        holder.image.setShowShadow(false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Glide.with(a)
                    .load(itemList.get(position).urls.regular)
                    .override(
                            itemList.get(position).getRegularWidth(),
                            itemList.get(position).getRegularHeight())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model,
                                                       Target<GlideDrawable> target,
                                                       boolean isFromMemoryCache, boolean isFirstResource) {
                            itemList.get(position).loadPhotoSuccess = true;
                            String titleTxt = a.getString(R.string.by) + " " + itemList.get(position).user.name + ", "
                                    + a.getString(R.string.on) + " " + itemList.get(position).created_at.split("T")[0];
                            holder.title.setText(titleTxt);
                            holder.image.setShowShadow(true);
                            return false;
                        }

                        @Override
                        public boolean onException(Exception e, String model,
                                                   Target<GlideDrawable> target,
                                                   boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(holder.image);
        } else {
            Glide.with(a)
                    .load(itemList.get(position).urls.regular)
                    .override(
                            itemList.get(position).getRegularWidth(),
                            itemList.get(position).getRegularHeight())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model,
                                                       Target<GlideDrawable> target,
                                                       boolean isFromMemoryCache, boolean isFirstResource) {
                            itemList.get(position).loadPhotoSuccess = true;
                            if (!itemList.get(position).hasFadedIn) {
                                holder.image.setHasTransientState(true);
                                final AnimUtils.ObservableColorMatrix matrix = new AnimUtils.ObservableColorMatrix();
                                final ObjectAnimator saturation = ObjectAnimator.ofFloat(
                                        matrix, AnimUtils.ObservableColorMatrix.SATURATION, 0f, 1f);
                                saturation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener
                                        () {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        // just animating the color matrix does not invalidate the
                                        // drawable so need this update listener.  Also have to create a
                                        // new CMCF as the matrix is immutable :(
                                        holder.image.setColorFilter(new ColorMatrixColorFilter(matrix));
                                    }
                                });
                                saturation.setDuration(2000L);
                                saturation.setInterpolator(AnimUtils.getFastOutSlowInInterpolator(a));
                                saturation.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        holder.image.clearColorFilter();
                                        holder.image.setHasTransientState(false);
                                    }
                                });
                                saturation.start();
                                itemList.get(position).hasFadedIn = true;
                            }
                            String titleTxt = a.getString(R.string.by) + " " + itemList.get(position).user.name + ", "
                                    + a.getString(R.string.on) + " " + itemList.get(position).created_at.split("T")[0];
                            holder.title.setText(titleTxt);
                            holder.image.setShowShadow(true);
                            return false;
                        }

                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(holder.image);

            holder.image.setTransitionName(itemList.get(position).id + "-image");
            holder.background.setTransitionName(itemList.get(position).id + "-background");
        }

        if (inMyCollection) {
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.likeButton.initLikeState(itemList.get(position).liked_by_user);

        holder.background.setBackgroundColor(
                ColorUtils.calcCardBackgroundColor(
                        itemList.get(position).color));
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        Glide.clear(holder.image);
    }

    public void setActivity(Activity a) {
        this.a = a;
    }

    /** <br> data. */

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void insertItem(Photo item) {
        itemList.add(item);
        notifyItemInserted(itemList.size() - 1);
    }

    public void clearItem() {
        itemList.clear();
        notifyDataSetChanged();
    }

    private void setLikeForAPhoto(boolean like, int position) {
        if (service == null) {
            service = PhotoService.getService();
        }
        service.setLikeForAPhoto(
                itemList.get(position).id,
                like,
                new OnSetLikeListener(itemList.get(position).id, like, position));
    }

    public void cancelService() {
        if (service != null) {
            service.cancel();
        }
    }

    public int getRealItemCount() {
        return itemList.size();
    }

    public void setOwn(boolean own) {
        this.own = own;
    }

    public void setInMyCollection(boolean in) {
        this.inMyCollection = in;
    }

    /** <br> interface. */

    // on set like listener.

    private class OnSetLikeListener implements PhotoService.OnSetLikeListener {
        // data
        private String id;
        private boolean like;
        private int position;

        OnSetLikeListener(String id, boolean like, int position) {
            this.id = id;
            this.like = like;
            this.position = position;
        }

        @Override
        public void onSetLikeSuccess(Call<LikePhotoResult> call, Response<LikePhotoResult> response) {
            if (response.isSuccessful() && response.body() != null) {
                if (itemList.size() >= position
                        && itemList.get(position).id.equals(response.body().photo.id)) {
                    itemList.get(position).liked_by_user = response.body().photo.liked_by_user;
                    itemList.get(position).likes = response.body().photo.likes;
                }
            } else if (Integer.parseInt(response.headers().get("X-Ratelimit-Remaining")) < 0) {
                RateLimitDialog dialog = new RateLimitDialog();
                dialog.show(((Activity) a).getFragmentManager(), null);
            } else {
                service.setLikeForAPhoto(
                        id,
                        like,
                        this);
            }
        }

        @Override
        public void onSetLikeFailed(Call<LikePhotoResult> call, Throwable t) {
            service.setLikeForAPhoto(
                    id,
                    like,
                    this);
        }
    }

    // on collections changed listener.

    @Override
    public void onAddCollection(Collection c) {
        ((MeActivity) a).addCollection(c);
    }

    @Override
    public void onAddPhotoToCollection(Collection c, User u) {
        ((MeActivity) a).changeCollection(c);
    }

    // on delete collection photo listener.

    @Override
    public void onDeletePhotoSuccess(ChangeCollectionPhotoResult result, int position) {
        if (itemList.size() > position && itemList.get(position).id.equals(result.photo.id)) {
            itemList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /** <br> inner class. */

    // view holder.

    class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, LikeImageButton.OnLikeListener {
        // widget
        public RelativeLayout background;
        public FreedomImageView image;
        public TextView title;
        ImageButton deleteButton;
        LikeImageButton likeButton;

        ViewHolder(View itemView, int position) {
            super(itemView);

            this.background = (RelativeLayout) itemView.findViewById(R.id.item_photo_background);
            background.setOnClickListener(this);

            this.image = (FreedomImageView) itemView.findViewById(R.id.item_photo_image);
            image.setSize(itemList.get(position).width, itemList.get(position).height);

            this.title = (TextView) itemView.findViewById(R.id.item_photo_title);
            DisplayUtils.setTypeface(itemView.getContext(), title);

            this.deleteButton = (ImageButton) itemView.findViewById(R.id.item_photo_deleteButton);
            deleteButton.setOnClickListener(this);

            this.likeButton = (LikeImageButton) itemView.findViewById(R.id.item_photo_likeButton);
            likeButton.setOnLikeListener(this);

            itemView.findViewById(R.id.item_photo_collectionButton).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.item_photo_background:
                    if (a instanceof Activity) {
                        View imageView = ((RelativeLayout) view).getChildAt(0);
                        IntentHelper.startPhotoActivity(
                                (Activity) a,
                                imageView,
                                view,
                                itemList.get(getAdapterPosition()));
                    }
                    break;

                case R.id.item_photo_deleteButton:
                    if (a instanceof CollectionActivity) {
                        DeleteCollectionPhotoDialog dialog = new DeleteCollectionPhotoDialog();
                        dialog.setDeleteInfo(
                                ((CollectionActivity) a).getCollection(),
                                itemList.get(getAdapterPosition()),
                                getAdapterPosition());
                        dialog.setOnDeleteCollectionListener(PhotoAdapter.this);
                        dialog.show(((CollectionActivity) a).getFragmentManager(), null);
                    }
                    break;

                case R.id.item_photo_collectionButton:
                    if (a instanceof Activity) {
                        if (!AuthManager.getInstance().isAuthorized()) {
                            Intent i = new Intent(a, LoginActivity.class);
                            a.startActivity(i);
                        } else {
                            SelectCollectionPhotoDialog dialog = new SelectCollectionPhotoDialog();
                            dialog.setPhoto(itemList.get(getAdapterPosition()));
                            if (own) {
                                dialog.setOnCollectionsChangedListener(PhotoAdapter.this);
                            }
                            dialog.show(((Activity) a).getFragmentManager(), null);
                        }
                    }
                    break;
            }
        }

        @Override
        public void onClickLikeButton(boolean newLikeState) {
            setLikeForAPhoto(newLikeState, getAdapterPosition());
        }
    }
}
