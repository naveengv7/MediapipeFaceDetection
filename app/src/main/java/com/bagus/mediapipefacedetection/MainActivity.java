package com.bagus.mediapipefacedetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketCallback;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.glutil.EglManager;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Code Sources:
//https://github.com/google/mediapipe/blob/master/mediapipe/examples/android/src/java/com/google/mediapipe/apps/iristrackinggpu/MainActivity.java
//https://github.com/google/mediapipe/blob/master/mediapipe/examples/android/src/java/com/google/mediapipe/apps/basic/MainActivity.java
//https://github.com/TheAdmiral95/IrisTracking

public class MainActivity extends AppCompatActivity {

    private static final String FOCAL_LENGTH_STREAM_NAME = "focal_length_pixel";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "face_landmarks_with_iris";
    private static final String BINARY_GRAPH_NAME = "iris_tracking_gpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.FRONT;
    private static final boolean FLIP_FRAMES_VERTICALLY = true;
    private SurfaceTexture surfaceTexture;
    private SurfaceView surfaceView;
    private Button captureImageButton;
    private EglManager eglManager;
    private FrameProcessor frameProcessor;
    private ExternalTextureConverter externalTextureConverter;
    private CameraXPreviewHelper cameraXPreviewHelper;
    private LandmarkProto.NormalizedLandmarkList currentLandmarks;
    private List<LandmarkProto.NormalizedLandmark> captureLandmarks;
    private boolean landmarksExist;
    private boolean haveSidePackets = false;
    private ImageCapture.OnImageSavedCallback imageSavedCallback;
    private ImageCapture.Builder imageCaptureBuilder;
    private final Size cameraResolution = new Size(2448,3264);
    private final String SAVE_FILE_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/";
    private File fullImageFile;
    private IrisData irisData;

    static {
        System.loadLibrary("mediapipe_jni");
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Initializing Super Class
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing Full Image File
        fullImageFile = new File(SAVE_FILE_DIR + "temp.jpg");

        //Initializing landmarksExist
        landmarksExist = false;
        captureLandmarks = null;

        //Getting Button From Main Activity and Setting Handler
        captureImageButton = findViewById(R.id.capImage);
        captureImageButton.setOnClickListener(new ImageCaptureBtnHandler());

        //Initializing the imageCaptureBuilder
        imageCaptureBuilder = new ImageCapture.Builder();

        //Creating Image Saved Callback
        generateImageSavedCallback();

        //Setting Up Preview
        surfaceView = new SurfaceView(this);
        setupPreviewDisplayView();

        //Initializing Asset Manager
        AndroidAssetUtil.initializeNativeAssetManager(this);

        //Setting up Frame Processor
        eglManager = new EglManager(null);
        frameProcessor = new FrameProcessor(this, eglManager.getNativeContext(), BINARY_GRAPH_NAME, INPUT_VIDEO_STREAM_NAME, OUTPUT_VIDEO_STREAM_NAME);
        frameProcessor.getVideoSurfaceOutput().setFlipY(FLIP_FRAMES_VERTICALLY);

        //Adding the Landmark Packet Callback
        addingLandmarkPacketCallback();

        //Getting Camera Permissions
        PermissionHelper.checkAndRequestCameraPermissions(this);
    }



    //From: https://github.com/google/mediapipe/blob/master/mediapipe/examples/android/src/java/com/google/mediapipe/apps/iristrackinggpu/MainActivity.java
    private void addingLandmarkPacketCallback() {
        frameProcessor.addPacketCallback(OUTPUT_LANDMARKS_STREAM_NAME,
                new PacketCallback() {
                    @Override
                    public void process(Packet packet) {
                        byte[] rawLandmarks = PacketGetter.getProtoBytes(packet);
                        try {
                            //Converting the Landmarks from their Raw Form
                            currentLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(rawLandmarks);


                            //Updating the state of the landmarksExist Variable
                            if (currentLandmarks == null) landmarksExist = false;
                            else landmarksExist = true;

                        } catch (InvalidProtocolBufferException e) {
                        }
                    }
                }
        );
    }

    private void onCameraStarted(SurfaceTexture surfaceTexture)
    {
        this.surfaceTexture = surfaceTexture;
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        this.surfaceView.setVisibility(View.VISIBLE);

        //This method is called on activity resume, however the following code should only be executed once
        if (!haveSidePackets) {
            float focalLength = cameraXPreviewHelper.getFocalLengthPixels();
            if (focalLength != Float.MIN_VALUE) {
                Packet focalLengthSidePacket = frameProcessor.getPacketCreator().createFloat32(focalLength);
                Map<String, Packet> inputSidePackets = new HashMap<>();
                inputSidePackets.put(FOCAL_LENGTH_STREAM_NAME, focalLengthSidePacket);
                frameProcessor.setInputSidePackets(inputSidePackets);
            }
            haveSidePackets = true;
        }
    }

    private void setupPreviewDisplayView() {
        surfaceView.setVisibility(View.GONE);
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        viewGroup.addView(surfaceView);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                frameProcessor.getVideoSurfaceOutput().setSurface(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                onPreviewDisplaySurfaceChanged(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                frameProcessor.getVideoSurfaceOutput().setSurface(null);
            }
        });
    }

    protected void onPreviewDisplaySurfaceChanged(int width, int height) {
        Size viewSize = new Size(width, height);
        Size displaySize = cameraXPreviewHelper.computeDisplaySizeFromViewSize(viewSize);
        boolean isCameraRotated = cameraXPreviewHelper.isCameraRotated();
        externalTextureConverter.setSurfaceTextureAndAttachToGLContext(
                surfaceTexture,
                isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    @Override
    protected void onResume() {
        super.onResume();
        externalTextureConverter = new ExternalTextureConverter(eglManager.getContext());
        externalTextureConverter.setFlipY(FLIP_FRAMES_VERTICALLY);
        externalTextureConverter.setConsumer(frameProcessor);
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera();
        }
    }

    private void startCamera() {
        cameraXPreviewHelper = new CameraXPreviewHelper();
        cameraXPreviewHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    onCameraStarted(surfaceTexture);
                }
        );
        cameraXPreviewHelper.startCamera(this, imageCaptureBuilder, CAMERA_FACING, null, null);
    }

    private class ImageCaptureBtnHandler implements View.OnClickListener{
        @Override
        public void onClick(View view) {

            String outputFilename = "output";

            //Checking if any landmarks are found
            if(landmarksExist)
            {
                //Displaying Result to the User
                Toast.makeText(view.getContext(), "Iris Landmarks Found: Captured Image", Toast.LENGTH_SHORT ).show();

                //Saving the Current Landmarks
                captureLandmarks = currentLandmarks.getLandmarkList();

                //Getting the Current Image
                cameraXPreviewHelper.takePicture(fullImageFile, imageSavedCallback);
            }
            else //Notifying the user
            {
                //Displaying Result to the User
                Toast.makeText(view.getContext(), "No Landmarks Found", Toast.LENGTH_SHORT ).show();
            }
        }
    }

    private void generateImageSavedCallback() {
        imageSavedCallback = new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                //Generating Bitmap
                Bitmap bmp = BitmapFactory.decodeFile(fullImageFile.getPath());

                //Creating IrisData
                irisData = new IrisData(captureLandmarks,bmp);
                irisData.generateIrisImages(SAVE_FILE_DIR + "LeftIris.jpg",SAVE_FILE_DIR + "RightIris.jpg");

                //Deleting the Temporary File
                fullImageFile.delete();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {

            }
        };
    }

    private List<LandmarkProto.NormalizedLandmark> copyLandmarks(LandmarkProto.NormalizedLandmarkList source)
    {
        return source.getLandmarkList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        externalTextureConverter.close();
        surfaceView.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}