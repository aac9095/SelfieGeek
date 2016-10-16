package android.socialcops.selfiegeek;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity {

    private String DEBUG_TAG = MainActivity.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private GalleryAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private ArrayList<String> imageList ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mLayoutManager = new GridLayoutManager(MainActivity.this, 2);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new GalleryAdapter(imageList,MainActivity.this);
        mRecyclerView.setAdapter(mAdapter);
        imageList = getImages();
        mAdapter.swapImages(imageList);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

    }

    private ArrayList<String> getImages(){
        ArrayList<String> results = new ArrayList<String>();

        File cameraAPiDir = CameraActivity.getDir();
        String path = cameraAPiDir.getPath();
        File[] files = cameraAPiDir.listFiles();

        if(files==null || files.length==0)
            return null;

        for (File file : files) {
            if (file.isFile()) {
                String address = path + File.separator + file.getName();
                results.add(address);
            }
        }
        return results;
    }

    @Override
    protected void onResume() {
        super.onResume();
        imageList = getImages();
        mAdapter.swapImages(imageList);
    }
}
