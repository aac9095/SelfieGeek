package android.socialcops.selfiegeek;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.kinvey.android.Client;
import com.kinvey.java.User;
import com.kinvey.java.core.KinveyCancellableCallback;
import com.kinvey.java.core.MediaHttpUploader;
import com.kinvey.java.core.UploaderProgressListener;
import com.kinvey.java.model.FileMetaData;
import com.kinvey.java.model.KinveyMetaData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {

    private ImageSurfaceView mImageSurfaceView;
    private Camera camera;
    private String DEBUG_TAG = CameraActivity.class.getSimpleName();
    private String videoFile;
    private static Client geekyClient;
    private FileMetaData metaData;
    private ImageButton switchCamera;
    private int camId = Camera.CameraInfo.CAMERA_FACING_BACK;
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFileDir = getDir();
            Log.d(DEBUG_TAG, "onPictureTaken:");
            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                Log.d(DEBUG_TAG, "Can't create directory to save image.");
                Toast.makeText(CameraActivity.this, "Can't create directory to save image.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "Picture_" + date + ".jpg";

            String filename = pictureFileDir.getPath() + File.separator + photoFile;

            File pictureFile = new File(filename);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Toast.makeText(CameraActivity.this, "New Image saved:" + photoFile,
                        Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Log.d(DEBUG_TAG, "File" + filename + "not saved: "
                        + error.getMessage());
                Toast.makeText(CameraActivity.this, "Image could not be saved.",
                        Toast.LENGTH_LONG).show();
            }

            //FileMetaData for images
            FileMetaData fileMetaData = new FileMetaData(date);
            fileMetaData.setPublic(true);
            fileMetaData.setAcl(new KinveyMetaData.AccessControlList());
            fileMetaData.setFileName(photoFile);

            //uploading file to kinvey
            kinveyUploadFile(fileMetaData,pictureFile);
        }
    };
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private FrameLayout cameraPreviewLayout;

    public static File getDir() {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "SelfieGeek");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraPreviewLayout = (FrameLayout)findViewById(R.id.camera_preview);
        mImageSurfaceView = (ImageSurfaceView) findViewById(R.id.image_surface);

        initializeCamera(camId);

        switchCamera = (ImageButton) findViewById(R.id.switch_camera);
        if(Camera.getNumberOfCameras()==1)
            switchCamera.setVisibility(View.INVISIBLE);
        else{
            switchCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    releaseMediaRecorder();
                    mImageSurfaceView.surfaceDestroyed(mImageSurfaceView.getHolder());
                    mImageSurfaceView.getHolder().removeCallback(mImageSurfaceView);
                    if(camId == Camera.CameraInfo.CAMERA_FACING_BACK)
                        camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    else
                        camId = Camera.CameraInfo.CAMERA_FACING_BACK;

                    initializeCamera(camId);
                }
            });
        }

        //Implicit Kinvey Login
        kinveyUserLogin();

        final ImageButton captureButton = (ImageButton) findViewById(R.id.button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.startPreview();
                Log.d(DEBUG_TAG, "onClick: Picture");
                camera.takePicture(null, null, pictureCallback);
            }
        });

        final ImageButton videoButton = (ImageButton) findViewById(R.id.video_button);
        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording){
                    Log.d(DEBUG_TAG, "isRecording: " + isRecording);
                    isRecording=false;
                    mediaRecorder.stop();
                    File video = new File(videoFile);
                    kinveyUploadFile(metaData,video);
                    releaseMediaRecorder();
                    prepareVideoRecorder(camera);
                    videoButton.setImageResource(R.mipmap.ic_start);

                }else {
                    try {
                        Log.d(DEBUG_TAG, "isRecording: " + isRecording);
                        mediaRecorder.prepare();
                        isRecording = true;
                        camera.unlock();
                        camera.stopPreview();
                        mediaRecorder.start();
                        videoButton.setImageResource(R.mipmap.ic_stop);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    private void initializeCamera(int camId) {
        camera = checkDeviceCamera(camId);
        prepareVideoRecorder(camera);

        mImageSurfaceView.enableCallBack(camera, mediaRecorder);
        mImageSurfaceView.surfaceCreated(mImageSurfaceView.getHolder());
    }

    private void kinveyUploadFile(FileMetaData metaData, File file) {
        geekyClient.file().upload(metaData, file, new UploaderProgressListener() {
            @Override
            public void progressChanged(MediaHttpUploader mediaHttpUploader) throws IOException {
                Log.e(DEBUG_TAG, "upload progress: " + mediaHttpUploader.getUploadState());
            }

            @Override
            public void onSuccess(FileMetaData fileMetaData) {
                Log.e(DEBUG_TAG,"File uploaded: " + fileMetaData.getFileName());
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(DEBUG_TAG,"File cannot be uploaded to kinvey ",throwable);
            }
        });
    }

    private void kinveyUserLogin() {
        geekyClient = new Client.Builder(getString(R.string.app_id), //app_id
                getString(R.string.app_secret), //app_secret
                getApplicationContext()) //context
                .build();
        if(!geekyClient.user().isUserLoggedIn())
            geekyClient.user().login(new KinveyCancellableCallback<User>() {
            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void onCancelled() {

            }

            @Override
            public void onSuccess(User user) {
                Log.e(DEBUG_TAG,user.getUsername() + "login");
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(DEBUG_TAG,"Implicit login failed",throwable);
            }
        });
    }

    private Camera checkDeviceCamera(int camId){
        Camera mCamera = null;
        try {
            mCamera = Camera.open(camId);
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            params.setJpegQuality(70);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            mCamera.setParameters(params);
            setCameraOrientation(CameraActivity.this,camId,mCamera);
        } catch (Exception e) {
            Log.e("Camera",e.getMessage());
        }
        return mCamera;
    }

    private void prepareVideoRecorder(Camera mCamera) {
        Log.d(DEBUG_TAG, "prepareVideoRecorder: ");
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        mediaRecorder.setOutputFile(getOutputMediaFile());
    }

    private void releaseMediaRecorder(){
        Log.d(DEBUG_TAG, "releaseMediaRecorder: ");
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

    private String getOutputMediaFile(){
        Log.d(DEBUG_TAG, "getOutputMediaFile: ");
        File pictureFileDir = getDir();

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            Log.d(DEBUG_TAG, "Can't create directory to save image.");
            Toast.makeText(CameraActivity.this, "Can't create directory to save image.",
                    Toast.LENGTH_LONG).show();
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String videoFileName = "Video_" + date + ".mp4";
        videoFile = pictureFileDir.getPath() + File.separator + videoFileName;

        //FileMetaData for videos
        metaData = new FileMetaData(date);
        metaData.setPublic(true);
        metaData.setAcl(new KinveyMetaData.AccessControlList());
        metaData.setFileName(videoFileName);

        return videoFile;
    }

    public static void setCameraOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}