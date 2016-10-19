package android.socialcops.selfiegeek;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Ayush on 15-10-2016.
 */

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private final Context context;
    private ArrayList<String> imageList;

    public GalleryAdapter(ArrayList<String> imageList, Context context) {
        this.imageList = imageList == null ? new ArrayList<String>() : imageList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.imageview, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String address = imageList.get(position);
        final File imageFile = new File(address);
        final boolean isVideo = address.contains("Video");
        if(imageFile.exists()){

            Bitmap myBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if(isVideo)
                holder.cameraImage.setImageResource(R.drawable.ic_video);
            else
                holder.cameraImage.setImageBitmap(myBitmap);

            holder.cameraImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.fromFile(imageFile);
                    if(isVideo)
                        intent.setDataAndType(uri,"video/*");
                    else
                        intent.setDataAndType(uri,"image/*");
                    context.startActivity(intent);
                }
            });
        }
    }

    public void add(int position, String item) {
        imageList.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(String item) {
        int position = imageList.indexOf(item);
        imageList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public void swapImages(ArrayList<String> data){
        imageList = data == null ? new ArrayList<String>() : data;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView cameraImage;

        public ViewHolder(View itemView) {
            super(itemView);
            cameraImage = (ImageView) itemView.findViewById(R.id.image);
        }
    }
}
