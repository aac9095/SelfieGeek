package android.socialcops.selfiegeek;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by Ayush on 15-10-2016.
 */

public class ImageSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    public static final String TAG = ImageSurfaceView.class.getSimpleName();

    private Camera camera;
    private MediaRecorder mediaRecorder;

    public ImageSurfaceView(Context context) {
        super(context);
    }

    public ImageSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ImageSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void enableCallBack(Camera camera, MediaRecorder mediaRecorder) {
        this.camera = camera;
        this.mediaRecorder = mediaRecorder;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: ");
            mediaRecorder.setPreviewDisplay(holder.getSurface());

            try {
                camera.lock();
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: ");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
            this.mediaRecorder.release();
            this.camera.stopPreview();
            this.camera.release();
        Log.d(TAG, "surfaceDestroyed: ");
    }
}
