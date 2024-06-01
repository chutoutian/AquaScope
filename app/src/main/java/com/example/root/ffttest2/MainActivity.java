package com.example.root.ffttest2;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;


import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.jjoe64.graphview.GraphView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.Set;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // ********************************** Start App Variable Definition **********************************
    String[] perms = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    private static SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    static Activity av;
    static boolean started=false;

    // these can be put into constants
    private static ImageView mImageView;
    private static ImageView mImageView2;

    private Button runModelButton;
    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;
    private Module mModule = null;

    private int currentIndex = 0;
    private String defaultModelName = "lite_optimized_seg_240p.ptl";
    private String defaultServiceName = "Seg Fish Low Res";

    private int defaultPosition = 0;

    private String[] allFiles;
    private String[] imageFiles;
    private String[] ptlFiles;

    private String currentModelName = "";

    private Button buttonRestart;
    private Button resizeButton;
    private EditText widthEditText;
    private EditText heightEditText;
    private Integer currentWidth;
    private Integer currentHeight;
    // see http://host.robots.ox.ac.uk:8080/pascal/VOC/voc2007/segexamples/index.html for the list of classes with indexes
    private Spinner modelSpinner;

    private TextView fishCountTextView;
    private TextView inferenceTimeTextView;
    private TextView fishExistTextView;
    private static final int CLASSNUM = 2;
    private int compressImageSize = 128;
    private Random random = new Random();
    private boolean isShouldRandom = false;
    private int recover_round = 1;

    // ********************************** End App Variable Definition **********************************


    // ********************************** Start App Methods (Later can be put into separate classes) **********************************

    public static int findIndex(String[] array, String target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                return i;
            }
        }
        return 0; // Element not found
    }

    private void resizeImage() {
        // Get user input for new width and height
        String widthStr = widthEditText.getText().toString();
        String heightStr = heightEditText.getText().toString();

        if (!widthStr.isEmpty() && !heightStr.isEmpty()) {
            int newWidth = Integer.parseInt(widthStr);
            int newHeight = Integer.parseInt(heightStr);
            currentWidth = newWidth;
            currentHeight = newHeight;
            displayImage(currentIndex);
        }
    }

    private void displayImageCenterCropWithSize(int index, int targetSize) {

        try {
            String imagePath = imageFiles[index];
            mBitmap = BitmapFactory.decodeStream(getAssets().open(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int sourceWidth = mBitmap.getWidth();
        int sourceHeight = mBitmap.getHeight();
        int s = Math.min(sourceWidth, sourceHeight);
        float r = (float)targetSize / s;
        int widthAfterScaled = Math.round(r * sourceWidth);
        Log.d("tbt", ""+widthAfterScaled);
        int heightAfterScaled = Math.round(r * sourceHeight);
        Log.d("tbt", ""+heightAfterScaled);
        mBitmap = Bitmap.createScaledBitmap(mBitmap, widthAfterScaled, heightAfterScaled, true);

        // Calculate the coordinates to center crop the scaled bitmap
        int x = (int)Math.round((widthAfterScaled - targetSize) / 2);
        int y = (int)Math.round((heightAfterScaled - targetSize) / 2);
        Log.d("tbt", x+" "+y);


        // Create a new bitmap and draw the center-cropped region
        Bitmap resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(mBitmap, -x, -y, null);
        mBitmap = resultBitmap;
        mImageView.setImageBitmap(mBitmap);
    }
    private void displayImage(int index) {
        try {
            // Load the image from the assets folder
            String imagePath = imageFiles[index];
            mBitmap = BitmapFactory.decodeStream(getAssets().open(imagePath));
            mBitmap = Bitmap.createScaledBitmap(mBitmap, currentWidth, currentHeight, true);
            // Display the image in the ImageView
            mImageView.setImageBitmap(mBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] filterJpgFiles(String[] files) {
        List<String> jpgFiles = new ArrayList<>();
        for (String file : files) {
            if (file.toLowerCase().endsWith(".jpg")) {
                jpgFiles.add(file);
            }
        }
        return jpgFiles.toArray(new String[0]);
    }

    private String[] filterPtlFiles(String[] files) {
        List<String> ptlFiles = new ArrayList<>();
        for (String file : files) {
            if (file.toLowerCase().endsWith(".ptl")) {
                ptlFiles.add(file);
            }
        }
        ptlFiles.add("VQGANEncode");
        ptlFiles.add("VQGANDecode");

        return ptlFiles.toArray(new String[0]);
    }

    private static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private static Boolean clf_res_postprocess(float x) {
        if (sigmoid(x) > 0.5)
            return true;
        return false;
    }

    private void load_bitmaps_for_end2endTest() {
        try {
            String[] testEnd2EndImageFiles = getAssets().list("sendTestImages");
            for (String file : testEnd2EndImageFiles) {
                int targetSize = compressImageSize;
                Bitmap tempMBitmap = BitmapFactory.decodeStream(getAssets().open(file));
                int sourceWidth = tempMBitmap.getWidth();
                int sourceHeight = tempMBitmap.getHeight();
                int s = Math.min(sourceWidth, sourceHeight);
                float r = (float) targetSize / s;
                int widthAfterScaled = Math.round(r * sourceWidth);
                int heightAfterScaled = Math.round(r * sourceHeight);
                tempMBitmap = Bitmap.createScaledBitmap(tempMBitmap, widthAfterScaled, heightAfterScaled, true);

                // Calculate the coordinates to center crop the scaled bitmap
                int x = (int) Math.round((widthAfterScaled - targetSize) / 2);
                int y = (int) Math.round((heightAfterScaled - targetSize) / 2);
                // Create a new bitmap and draw the center-cropped region
                Bitmap resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(resultBitmap);
                canvas.drawBitmap(tempMBitmap, -x, -y, null);
                tempMBitmap = resultBitmap;
                Constants.testEnd2EndImageBitmaps.add(tempMBitmap);
            }
        }  catch(Exception e){
            e.printStackTrace();
        }
        Utils.logd("Length of bitmapList: " + Constants.testEnd2EndImageBitmaps.size());
        Constants.didLoadTestImages = true;
    }

    private void load_all_image_codec_models() {
        try {
            if (Constants.mEncoder1 == null) {
                Constants.mEncoder1 = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), "encoder_optimized.ptl"));
            }
            if (Constants.mEncoder2 == null) {
                Constants.mEncoder2 = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), "quant_conv_optimized.ptl"));
            }
            if (Constants.mEncoder3 == null) {
                Constants.mEncoder3 = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), "quantize_optimized.ptl"));
            }

            // later we can separate encoder and decoder users
            if (Constants.mDecoder1 == null) {
                Constants.mDecoder1 = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), "embedding_optimized.ptl"));
            }
            if (Constants.mDecoder2 == null) {
                Constants.mDecoder2 = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), "post_quant_conv_optimized.ptl"));
            }
            if (Constants.mDecoder3 == null) {
                Constants.mDecoder3 = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), "decoder.ptl"));
            }

            if (Constants.mTransformer == null) {
                Constants.mTransformer = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), "transformer_optimized.ptl"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void load_all_file_paths(AssetManager assetManager) {
        try {
            allFiles = assetManager.list("");
            imageFiles = filterJpgFiles(allFiles);
            ptlFiles = filterPtlFiles(allFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.logd("Image Files: " + Arrays.toString(imageFiles));
        Utils.logd("Ptl Files: " + Arrays.toString(ptlFiles));
    }

    // ********************************** End App Methods (Later can be put into separate classes) **********************************

    // Called when the app is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.logd("oncreate start");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // request permission
        ActivityCompat.requestPermissions(this,
                perms,
                1234);

        // get all image and model filepaths in the assets folder
        AssetManager assetManager = getAssets();
        load_all_file_paths(assetManager);

        // preload bitmaps for end2endTest, only load once
        if (Constants.didLoadTestImages == false) {
            load_bitmaps_for_end2endTest();
        }

        // preload all image code related models
        load_all_image_codec_models();

        // setup ui components
        uiSetup();

        // setup constants
        Constants.setup(this);

        // Load the initial image
        if (imageFiles != null && imageFiles.length > 0) {
            // Display the first image
            resizeImage();
        } else {
            // Handle the case when no images are found
            buttonRestart.setEnabled(false);
            resizeButton.setEnabled(false);
        }

        // set window properties
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // set up some services
        // set up sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // setup notification maneger
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }

        // is tts used ?
//        Constants.tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
//            @Override
//            public void onInit(int status) {
//                if(status == TextToSpeech.SUCCESS){
//                    int result=Constants.tts.setLanguage(Locale.UK);
//                    if(result==TextToSpeech.LANG_MISSING_DATA ||
//                        result==TextToSpeech.LANG_NOT_SUPPORTED){
//                        Log.e("error", "This Language is not supported");
//                    }
//                }
//                else
//                    Log.e("error", "Initilization Failed!");
//            }
//        });
        Utils.logd("oncreate end");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1234) {
            boolean audioGranted=false;
            boolean writeGranted=false;
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        audioGranted=true;
                    } else {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1234);
                    }
                }
                else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        writeGranted=true;
                    } else {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1234);
                    }
                }
            }
            // remove to solve double sendchirpasynctask
//            if (audioGranted && writeGranted) {
//                Constants.user  = Constants.User.Bob;
//                if (started == false) {
//                    Utils.log("start startmethod from on permission granted");
//                    startMethod(this);
//                }
//            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreencall();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.logd("destroy!!!");
    }

    protected void onPause() {
        super.onPause();
        Utils.logd("onpause");
//        sensorManager.unregisterListener(this);
        stopMethod();
    }

    protected void onResume() {
        super.onResume();
        Utils.logd("onresume");
//        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        FullScreencall();

        Constants.user  = Constants.User.Bob;
        if (started == false) {
            Utils.log("start startmethod from onResume");
            startMethod(this);
        }
//        if (Constants.sw12.isChecked() &&
//            ActivityCompat.checkSelfPermission(av, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(av, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
//            !started) {
//            Constants.user  = Constants.User.Bob;
//            Utils.log("start startmethod from Constants.sw12.isChecked() ");
//
//            startMethod(this);
//        }
    }

    public static void startWrapper() {
        Constants.user = Constants.User.Alice;
        stopMethod();
        Utils.log("start startmethod from startWrapper");
        startMethod(av);
    }

    public void onstart(View v) {
        // Beitong: Change the button color to RED after it is pressed. Save the default background for recover the button color after sending chirp.
        Constants.defaultBackground = v.getBackground();
        v.setBackgroundColor(Color.RED);
        // Beitong: Recommended by ChatGPT, in this way it will not block the UI Thread
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startWrapper();
            }
        }, Constants.Send_Delay);

//        try {
//            Thread.sleep(Constants.Send_Delay);
//        }
//        catch (Exception e) {
//            Utils.log(e.getMessage());
//        }
//        startWrapper();
    }

    public static void startMethod(Activity av) {
        started=true;
//        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        Constants.ts = System.currentTimeMillis();

        Constants.work=true;
        FileOperations.mkdir(av,Utils.getDirName());
//        FileOperations.writetofile(av, Constants.ts+"", Utils.genName(Constants.SignalType.Timestamp,0)+".txt");

        Constants.tv6.setText(Utils.trimmed_ts());
        String formattedNow = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            formattedNow = now.format(formatter);
        } else {
            formattedNow = "not_available";
        }

        Constants.task = new SendChirpAsyncTask(av, Constants.mattempts, Constants.sendButton, Constants.defaultBackground, Constants.testEnd2EndImageBitmaps, mImageView, mImageView2, formattedNow);
        Constants.task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void onstop(View v) {
        stopMethod();
        Constants.user  = Constants.User.Bob;
        Utils.log("start startmethod from onstop");
        startMethod(av);
    }

    public static void stopMethod() {
        if (Constants.task != null) {
            Constants.task.cancel(true);
            Constants.task = null;
        }
        if (Constants.timer!=null) {
            Constants.timer.cancel();
        }

//        sensorManager.unregisterListener(this);
        Constants.work=false;
        Utils.logd("onstop");
        Constants.sensorFlag=false;
//        if (Constants.acc != null && Constants.acc.size() > 0) {
//            FileOperations.writeSensors(this, Constants.ts+".txt");
//        }
        if (Constants._OfflineRecorder!=null) {
            Constants._OfflineRecorder.halt2();
        }
        if (Constants.sp1!=null && Constants.sp1.track1!=null&&
                Constants.sp1.track1.getState()== AudioTrack.STATE_INITIALIZED) {
            Constants.sp1.pause();
        }
        Constants.toggleUI(true);
        started=false;
    }

    public void FullScreencall() {
        if(Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if(Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public void clear(View v) {
        Constants.debugPane.setText("");
//        Constants.imgview.setImageBitmap(null);
        inferenceTimeTextView.setText("");
        fishCountTextView.setText("");
        fishExistTextView.setText("");
        Constants.msgview.setText("");
        FullScreencall();
    }

    public static void unreg(Activity av) {
        sensorManager.unregisterListener((MainActivity)av);
    }


    // ********************************** Start App Long Methods **********************************

    // Called when run model button is pressed
    public void run() {
        if (currentModelName.equals("VQGANEncode")) {
            float[] mu = {0.0f, 0.0f, 0.0f};
            float[] std = {1.0f, 1.0f, 1.0f};
            final Tensor tempInputTensor = TensorImageUtils.bitmapToFloat32Tensor(mBitmap,
                    mu, std);


            // Convert the PyTorch tensor to a float array
            float[] tempArray = tempInputTensor.getDataAsFloatArray();

            // Apply the operation x = 2 * x - 1 to the float array
            for (int i = 0; i < tempArray.length; i++) {
                tempArray[i] = 2.0f * tempArray[i] - 1.0f;
            }

            // Convert the float array back to a PyTorch tensor
            Tensor inputTensor = Tensor.fromBlob(tempArray, tempInputTensor.shape()); // Adjust the shape as needed
            final long startTime = SystemClock.elapsedRealtime();
            // run model
            Tensor outTensors = Constants.mEncoder1.forward(IValue.from(inputTensor)).toTensor();
//            final float[] intResult = outTensors.getDataAsFloatArray();
//            Log.d("tbt", Arrays.toString(intResult));
            outTensors = Constants.mEncoder2.forward(IValue.from(outTensors)).toTensor();
            outTensors = Constants.mEncoder3.forward(IValue.from(outTensors)).toTensor();
            final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
            Log.d("tbt",  "inference time (ms): " + inferenceTime);
            final long[] results = outTensors.getDataAsLongArray();
//            Constants.SegFish = results;

            Log.d("tbt", "result: " + Arrays.toString(results));
            Log.d("tbt", "result length: " + results.length );
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    fishCountTextView.setText(String.valueOf(Math.round(results[0])));
                    runModelButton.setEnabled(true);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    inferenceTimeTextView.setText(inferenceTime+" ms");
                }
            });
        }
        else if (currentModelName.equals("VQGANDecode")) {

            // default workable indices
            long[] indices = new long[] {425,256,854,389,329,972,901,184,969,1019,515,906,172,253,589,928,937,810,966,215,610,720,365,628,856,854,642,92,247,642,558,835,788,601,400,911,417,813,903,610,937,970,462,810,539,417,308,546,761,432,323,172,469,865,1012,663,725,548,873,40,868,548,737,393};
            // randomly generate indices every two clicks
            if (isShouldRandom == true) {
                for (int i = 0; i < indices.length; i++) {
                    indices[i] = random.nextInt(1024);
                }
                isShouldRandom = false;
            } else {
                isShouldRandom = true;
            }

            final long startTime = SystemClock.elapsedRealtime();
            Tensor inputTensor = Tensor.fromBlob(indices, new long[]{64});
            Tensor outTensors = Constants.mDecoder1.forward(IValue.from(inputTensor)).toTensor();
//            Log.d("tbt", "shape" + Arrays.toString(outTensors.shape()));
            final float[] embedding_res = outTensors.getDataAsFloatArray();

            outTensors = Constants.mDecoder2.forward(IValue.from(outTensors)).toTensor();
//            Log.d("tbt", "shape" + Arrays.toString(outTensors.shape()));
            outTensors = Constants.mDecoder3.forward(IValue.from(outTensors)).toTensor();
//            Log.d("tbt", "shape" + Arrays.toString(outTensors.shape()));
            final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
            Log.d("tbt",  "inference time (ms): " + inferenceTime);

            final byte[] rgbData = outTensors.getDataAsUnsignedByteArray();
            int[] argbPixels = new int[compressImageSize * compressImageSize]; // Array to hold ARGB pixel data.
            int pixelIndex = 0;
            int argbIndex = 0;
            for (int y = 0; y < compressImageSize; y++) {
                for (int x = 0; x < compressImageSize; x++) {
                    int r = rgbData[pixelIndex++] & 0xFF; // Red component
                    int g = rgbData[pixelIndex++] & 0xFF; // Green component
                    int b = rgbData[pixelIndex++] & 0xFF; // Blue component
//                    Log.d("tbt", "r " + rgbData[pixelIndex-3] + " g " + rgbData[pixelIndex-2] + " b " + rgbData[pixelIndex-1]);
                    // Combine these into an ARGB color with full opacity.
                    int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                    argbPixels[argbIndex++] = argb; // Store the ARGB value in the array.
                }
            }
            mBitmap = Bitmap.createBitmap(argbPixels, compressImageSize, compressImageSize, Bitmap.Config.ARGB_8888);

            Log.d("tbt", "result length: " + rgbData.length );


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    fishCountTextView.setText(String.valueOf(Math.round(results[0])));
                    mImageView.setImageBitmap(mBitmap);
                    runModelButton.setEnabled(true);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    inferenceTimeTextView.setText(inferenceTime+" ms");
                }
            });
        }
        else if (currentModelName.equals("transformer_optimized.ptl")) {
            long[] gt = new long[] {425,256,854,389,329,972,901,184,969,1019,515,906,172,253,589,928,937,810,966,215,610,720,365,628,856,854,642,92,247,642,558,835,788,601,400,911,417,813,903,610,937,970,462,810,539,417,308,546,761,432,323,172,469,865,1012,663,725,548,873,40,868,548,737,393};
            long[] data = new long[] {425,320,854,388,264,972,965,188,713,1019,515,936,184,253,589,928,1000,810,710,213,610,720,381,628,856,854,646,92,247,642,558,835,784,601,400,907,297,813,902,611,937,970,462,810,571,417,308,546,633,432,323,168,469,865,1012,658,725,544,873,41,868,548,737,393};
            Tensor inputTensor = Tensor.fromBlob(data, new long[]{1, 64});
            Tensor inputTensor2 = Tensor.fromBlob(data, new long[]{1, 64});
            Log.d("tbt", "shape: " + Arrays.toString(inputTensor.shape()));
            final long startTime = SystemClock.elapsedRealtime();
            for (int p = 0; p < Constants.recover_round; p++) {
                IValue result = Constants.mTransformer.forward(IValue.from(inputTensor), IValue.from(inputTensor2));
                if (result.isTuple()) {
                    // Get the tuple and extract the tensors
                    IValue[] outputs = result.toTuple();
                    Tensor prediction_tensor = outputs[0].toTensor();
                    Tensor target = outputs[1].toTensor();
                    long[] prediction = prediction_tensor.getDataAsLongArray();
                    int differenceCount = 0;
                    int differenceCount_gt = 0;
                    for (int i = 0; i < gt.length; i++) {
                        if (data[i] != gt[i]) {
                            differenceCount++;
                        }
                    }
                    for (int i = 0; i < gt.length; i++) {
                        if (prediction[i] != gt[i]) {
                            differenceCount_gt++;
                        }
                    }
//                    Log.d("tbt", "input: " + Arrays.toString(data));
                    Log.d("tbt", "gt: " + Arrays.toString(gt));
//
                    Log.d("tbt", "result: " + Arrays.toString(prediction));
                    Log.d("tbt", "difference count before recovery: " + differenceCount);
                    Log.d("tbt", "difference count after recovery: " + differenceCount_gt);

                    Log.d("tbt", "result length: " + prediction.length);
                    inputTensor = Tensor.fromBlob(prediction, new long[]{1, 64});
                    inputTensor2 = Tensor.fromBlob(prediction, new long[]{1, 64});;
                }
            }
            final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
            Log.d("tbt", "inference time (ms): " + inferenceTime);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //                    fishCountTextView.setText(String.valueOf(Math.round(results[0])));
                    runModelButton.setEnabled(true);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    inferenceTimeTextView.setText(inferenceTime + " ms");
                }
            });
        }
        else if (currentModelName.equals("lite_optimized_count_fish_224_224.ptl")) {

            final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(mBitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
            final float[] inputs = inputTensor.getDataAsFloatArray();

            final long startTime = SystemClock.elapsedRealtime();
//        Map<String, IValue> outTensors = mModule.forward(IValue.from(inputTensor)).toDictStringKey();
            final Tensor outTensors = mModule.forward(IValue.from(inputTensor)).toTensor();
            final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
            Log.d("ImageSegmentation",  "inference time (ms): " + inferenceTime);


//        final Tensor outputTensor = outTensors.get("out").toTensor();
            final Tensor outputTensor = outTensors;

            final float[] results = outputTensor.getDataAsFloatArray();
            Constants.NumFish = Math.round(results[0]);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fishCountTextView.setText(String.valueOf(Constants.NumFish));
                    runModelButton.setEnabled(true);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    inferenceTimeTextView.setText(inferenceTime+" ms");
                }
            });
        }
        else if (currentModelName.equals("lite_optimized_clf.ptl")) {
            final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(mBitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
            final float[] inputs = inputTensor.getDataAsFloatArray();

            final long startTime = SystemClock.elapsedRealtime();
//        Map<String, IValue> outTensors = mModule.forward(IValue.from(inputTensor)).toDictStringKey();
            final Tensor outTensors = mModule.forward(IValue.from(inputTensor)).toTensor();
            final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
            Log.d("ImageSegmentation",  "inference time (ms): " + inferenceTime);


//        final Tensor outputTensor = outTensors.get("out").toTensor();
            final Tensor outputTensor = outTensors;

            final float[] results = outputTensor.getDataAsFloatArray();

            Constants.IsFish = clf_res_postprocess(results[0]);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fishExistTextView.setText(clf_res_postprocess(results[0]).toString());
                    runModelButton.setEnabled(true);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    inferenceTimeTextView.setText(inferenceTime+" ms");
                }
            });
        }
        else if (currentModelName.equals("deepfish_scripted_optimized.ptl") || currentModelName.equals("lite_optimized_seg_240p.ptl")) {
            final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(mBitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
            final float[] inputs = inputTensor.getDataAsFloatArray();

            final long startTime = SystemClock.elapsedRealtime();
//        Map<String, IValue> outTensors = mModule.forward(IValue.from(inputTensor)).toDictStringKey();
            final Tensor outTensors = mModule.forward(IValue.from(inputTensor)).toTensor();
            final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
            Log.d("ImageSegmentation",  "inference time (ms): " + inferenceTime);
            final Tensor outputTensor = outTensors;

            final float[] scores = outputTensor.getDataAsFloatArray();

            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();
//        for (int i = 0; i < width * height; i++) {
//            Log.d("ImageSegmentation",  "class 0 " + scores[i] + " class 1" + scores[i+width*height]);
//        }
            Log.d("ImageSegmentation",  "outputTensor len: " + scores.length);
            Log.d("ImageSegmentation",  "bit map width: " + width);
            Log.d("ImageSegmentation",  "bit map height: " + height);
            int[] intValues = new int[width * height];
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < width; k++) {
                    int maxi = 0, maxj = 0, maxk = 0;
                    double maxnum = -Double.MAX_VALUE;
                    for (int i = 0; i < CLASSNUM; i++) {
                        float score = scores[i * (width * height) + j * width + k];
                        if (score > maxnum) {
                            maxnum = score;
                            maxi = i; maxj = j; maxk = k;
                        }
                    }
                    if (maxi == 1) {
//                    mBitmap.setPixel(10, 10, Color.GREEN);
                        intValues[maxj * width + maxk] = 0xFF00FF00;
                    }

                    else
//                    continue;
                        intValues[maxj * width + maxk] = 0xFF000000;
//                    intValues[maxj * width + maxk] = int(mBitmap.getColor(maxj, maxk));

                }
            }

            Bitmap bmpSegmentation = Bitmap.createScaledBitmap(mBitmap, width, height, true);
            Bitmap outputBitmap = bmpSegmentation.copy(bmpSegmentation.getConfig(), true);
            outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
            final Bitmap transferredBitmap = Bitmap.createScaledBitmap(outputBitmap, mBitmap.getWidth(), mBitmap.getHeight(), true); //

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageView.setImageBitmap(transferredBitmap);
                    runModelButton.setEnabled(true);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    inferenceTimeTextView.setText(inferenceTime+" ms");

                }
            });
        }
    }

    public void uiSetup() {

        // *********************** start setup ui components new in image transmission ***********************

        // setup keyboard
        KeyboardVisibilityEvent.setEventListener(
                this,
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean isOpen) {
                        FullScreencall();
                    }
                });

        mImageView = findViewById(R.id.imageView_fish);
        mImageView2 = findViewById(R.id.imageView_fish2);
        Constants.logswitch = findViewById(R.id.logcontroller);
        Constants.logswitch.setChecked(true); // set default to true which means we want to log
        Constants.allowLog = true;

        Constants.logswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Handle switch state change
                if (isChecked) {
                    Utils.log("Log Switch is ON");
                    Constants.allowLog = true;
                } else {
                    Utils.log("Log Switch is OFF");
                    Constants.allowLog = false;
                }
            }
        });

//        Constants.frameLayout = findViewById(R.id.frameLayout);
        Constants.preview = findViewById(R.id.previewView);
        Constants.cameraCaptureBtn = findViewById(R.id.cameraCapture);
        Constants.cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Constants.cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = Constants.cameraProviderFuture.get();
                CameraHelper.bindCamera(cameraProvider, this, mImageView2);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        //        CameraHelper.startCamera(this, Constants.cameraTextureView, mImageView2);
        Constants.cameraCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraHelper.takePicture2();
            }
        });

        // Resize:
        // Create widgets for image resize feature (width edit text box, height edit text box and resize button)
        widthEditText = findViewById(R.id.editWidth);
        heightEditText = findViewById(R.id.editHeight);
        resizeButton = findViewById(R.id.buttonResize);
        // Set click listener for the resize button
        resizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Resize the image based on user input
                resizeImage();
            }
        });

        // Run Model:
        // set listener of the segment button (do model inference with the current displayed image).
        runModelButton = findViewById(R.id.runModelButton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        runModelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                runModelButton.setEnabled(false);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                run();
            }
        });

        // Go to Next Image:
        // set listener of the next image button
        buttonRestart = findViewById(R.id.restartButton);
        buttonRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show the next image
                currentIndex = (currentIndex + 1) % imageFiles.length;
                if (currentModelName.equals("VQGANEncode")) {
                    displayImageCenterCropWithSize(currentIndex, compressImageSize);
                } else {
                    displayImage(currentIndex);
                }
            }
        });

        // Count Fish Text View
        fishCountTextView = findViewById(R.id.fishCount);
        inferenceTimeTextView = findViewById(R.id.inferenceTime);
        fishExistTextView = findViewById(R.id.fishExist);

        // model selector
        if (ptlFiles != null && ptlFiles.length > 0) {
            // get all service names
            Set<String> keySet = Constants.serviceNameToModelMap.keySet();
            String[] allServiceNames = keySet.toArray(new String[0]);

            // Model Selector:
            modelSpinner = findViewById(R.id.modelSpinner);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allServiceNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modelSpinner.setAdapter(spinnerAdapter);

            defaultPosition = findIndex(allServiceNames, defaultServiceName);
            Log.d("ImageSegmentation", "default position of" + defaultModelName + " is " + defaultPosition);

            modelSpinner.setSelection(defaultPosition);
            modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    // Load the selected model
                    currentModelName = Constants.serviceNameToModelMap.get(allServiceNames[position]);
                    try {
                        Log.d("ImageSegmentation", currentModelName);
                        if (currentModelName.equals("lite_optimized_count_fish_224_224.ptl")) {
                            Constants.IsCountingFish = true;
                            Constants.IsDectectingFish = false;
                            Constants.ImagingFish = false;
                        }
                        else if (currentModelName.equals("lite_optimized_clf.ptl"))
                        {
                            Constants.IsDectectingFish = true;
                            Constants.IsCountingFish = false;
                            Constants.ImagingFish = false;
                        }
                        else if (currentModelName.equals("lite_optimized_seg_240p.ptl"))
                        {
                            Constants.IsDectectingFish = false;
                            Constants.IsCountingFish = false;
                            Constants.ImagingFish = true;
                        }

                        // special handle counting fish
                        if (currentModelName.equals("lite_optimized_count_fish_224_224.ptl") || currentModelName.equals("lite_optimized_clf.ptl")) {
                            mModule = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), currentModelName));
                            currentHeight = 224;
                            currentWidth = 224;
                            // do we need it?
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    widthEditText.setText("224");
                                    heightEditText.setText("224");
                                }
                            });

                            resizeImage();
                        } else if (currentModelName.equals("lite_optimized_seg_240p.ptl")) {
                            mModule = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), currentModelName));
                            currentHeight = 240;
                            currentWidth = 426;
                            // do we need it?
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    widthEditText.setText("426");
                                    heightEditText.setText("240");
                                }
                            });

                            resizeImage();
                        } else if (currentModelName.equals("VQGANEncode")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    widthEditText.setText(String.valueOf(compressImageSize));
                                    heightEditText.setText(String.valueOf(compressImageSize));
                                }
                            });

                            displayImageCenterCropWithSize(currentIndex, compressImageSize);
                        } else if (currentModelName.equals("VQGANDecode")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    widthEditText.setText(String.valueOf(compressImageSize));
                                    heightEditText.setText(String.valueOf(compressImageSize));
                                }
                            });

                            displayImageCenterCropWithSize(currentIndex, compressImageSize);
                        } else if (currentModelName.equals("transformer_optimized.ptl")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    widthEditText.setText(String.valueOf(compressImageSize));
                                    heightEditText.setText(String.valueOf(compressImageSize));
                                }
                            });
                            displayImageCenterCropWithSize(currentIndex, compressImageSize);

                        } else {
                            mModule = LiteModuleLoader.load(Utils.assetFilePath(getApplicationContext(), currentModelName));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        finish();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // Do nothing
                }
            });

        }

        // *********************** end setup ui components new in image transmission ***********************



        Constants.gview = (GraphView) findViewById(R.id.graphProd);
        Constants.gview2 = (GraphView) findViewById(R.id.graphProd);
        Constants.gview3 = (GraphView) findViewById(R.id.graphProd3);
        Constants.gview4 = (GraphView) findViewById(R.id.graphProd4);

        Constants.sw1 = (Switch) findViewById(R.id.switch1);
        Constants.sw2 = (Switch) findViewById(R.id.switch2);
        Constants.sw3 = (Switch) findViewById(R.id.switch3);
        Constants.sw4 = (Switch) findViewById(R.id.switch4);
        Constants.sw5 = (Switch) findViewById(R.id.switch5);
        Constants.sw6 = (Switch) findViewById(R.id.switch6);
        Constants.sw7 = (Switch) findViewById(R.id.switch7);
        Constants.sw8 = (Switch) findViewById(R.id.switch8);
        Constants.sw9 = (Switch) findViewById(R.id.switch9);
        Constants.sw10 = (Switch) findViewById(R.id.switch10);
        Constants.sw11 = (Switch) findViewById(R.id.switch11);
        Constants.sw12 = (Switch) findViewById(R.id.switch12);
        Constants.spinner = (Spinner) findViewById(R.id.spinner);
        Constants.spinner2 = (Spinner) findViewById(R.id.spinner2);
        Constants.spinner3 = (Spinner) findViewById(R.id.spinner3);
        Constants.spinner4 = (Spinner) findViewById(R.id.spinner4);
        Constants.sendButton = (Button) findViewById(R.id.sendbutton);

        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("None");
        arrayList.add("4/8");
        arrayList.add("6/8");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arrayList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Constants.spinner.setAdapter(arrayAdapter);
        Constants.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                if (position == 0) {
                    editor.putString("code_rate", Constants.CodeRate.None.toString());
                    Constants.CodeRate_LoRA = 0;
                }
                else if (position == 1) {
                    editor.putString("code_rate", Constants.CodeRate.C4_8.toString());
                    Constants.CodeRate_LoRA = 4;
                }
                else if (position == 2) {
                    editor.putString("code_rate", Constants.CodeRate.C4_6.toString());
                    Constants.CodeRate_LoRA = 2;
                }
                stopMethod();
                startMethod(av);
                editor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });

        ArrayList<String> arrayList2 = new ArrayList<>();
        arrayList2.add("Proposed");
        arrayList2.add("OFDMwiadapt");
        arrayList2.add("OFDMwoadapt");
        arrayList2.add("Noise");
        arrayList2.add("Chirp");
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arrayList2);
        arrayAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Constants.spinner2.setAdapter(arrayAdapter2);
        // need to stop the current method
        Constants.spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                //Log.e("snr","pos "+position+"");
                if (position == 0) {
                    editor.putString("Tx protocol", Constants.Modulation.LoRa.toString());
                    Constants.scheme = Constants.Modulation.LoRa;
                    //Constants.et4.setText(Constants.Modulation.LoRa+"");
                }
                else if (position == 1) {
                    editor.putString("Tx protocol", Constants.Modulation.OFDM_freq_adapt.toString());
                    Constants.scheme = Constants.Modulation.OFDM_freq_adapt;
                    //Constants.et4.setText(Constants.Modulation.OFDM_freq_adapt+"");
                }
                else if (position == 2) {
                    editor.putString("Tx protocol", Constants.Modulation.OFDM_freq_all.toString());
                    Constants.scheme = Constants.Modulation.OFDM_freq_all;
                    //Constants.et4.setText(Constants.Modulation.OFDM_freq_all+"");
                }
                else if (position == 3) {
                    editor.putString("Tx protocol", Constants.Modulation.Noise.toString());
                    Constants.scheme = Constants.Modulation.Noise;
                    //Constants.et4.setText(Constants.Modulation.OFDM_freq_all+"");
                }
                else if (position == 4) {
                    editor.putString("Tx protocol", Constants.Modulation.Chirp.toString());
                    Constants.scheme = Constants.Modulation.Chirp;
                    //Constants.et4.setText(Constants.Modulation.OFDM_freq_all+"");
                }
                stopMethod();
                startMethod(av);
                editor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });

        ArrayList<String> arrayList3 = new ArrayList<>();
        arrayList3.add("960");
        arrayList3.add("1920");
        arrayList3.add("4800");
        arrayList3.add("9600");

        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arrayList3);
        arrayAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Constants.spinner3.setAdapter(arrayAdapter3);
        Constants.spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putInt("ns", Integer.parseInt(arrayList3.get(position)));
                Constants.Ns = Integer.parseInt(arrayList3.get(position));
                editor.commit();
                Constants.updateNbins();
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });

        ArrayList<String> arrayList4 = new ArrayList<>();
        arrayList4.add("testExp");
        arrayList4.add("end2endTest");
        arrayList4.add("end2endCam");

        ArrayAdapter<String> arrayAdapter4 = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arrayList4);
        arrayAdapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Constants.spinner4.setAdapter(arrayAdapter4);
        Constants.spinner4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putInt("expMode", Constants.Experiment.valueOf(arrayList4.get(position)).ordinal());
                // trigger setup, need to clean the code
                if (arrayList4.get(position) == "end2endTest") {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                widthEditText.setText(String.valueOf(compressImageSize));
                                heightEditText.setText(String.valueOf(compressImageSize));
                            }
                        });

                        displayImageCenterCropWithSize(currentIndex, compressImageSize);
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
                Constants.expMode = Constants.Experiment.valueOf(arrayList4.get(position));
                stopMethod();
                startMethod(av);
                Utils.logd("Current Expmode: " + arrayList4.get(position));
                editor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });

        Constants.et1 = (EditText) findViewById(R.id.editTextNumber);
        Constants.et2 = (EditText) findViewById(R.id.editTextNumber2);
        Constants.et3 = (EditText) findViewById(R.id.editTextNumber3);
        Constants.et4 = (EditText) findViewById(R.id.editTextNumber4);
        Constants.et5 = (EditText) findViewById(R.id.editTextNumber5);
        Constants.et6 = (EditText) findViewById(R.id.editTextNumber6);
        Constants.et7 = (EditText) findViewById(R.id.editTextNumber7);
        Constants.et8 = (EditText) findViewById(R.id.editTextNumber8);
        Constants.et9 = (EditText) findViewById(R.id.editTextNumber9);
        Constants.et10 = (EditText) findViewById(R.id.editTextNumber10);
        Constants.et11 = (EditText) findViewById(R.id.editTextNumber11);
        Constants.et12 = (EditText) findViewById(R.id.editTextNumber12);
        Constants.et13 = (EditText) findViewById(R.id.editTextNumber13);
        Constants.et14 = (EditText) findViewById(R.id.editTextNumber14);
        Constants.et15 = (EditText) findViewById(R.id.editTextNumber15);
        Constants.et17 = (EditText) findViewById(R.id.editTextNumber17);
        Constants.et18 = (EditText) findViewById(R.id.editTextNumber18);
        Constants.et25 = (EditText) findViewById(R.id.editTextNumber25);
        Constants.et26 = (EditText) findViewById(R.id.editTextNumber26);
        Constants.et27 = (EditText) findViewById(R.id.editTextNumber27);

        Constants.tv1 = (TextView) findViewById(R.id.textView);
        Constants.tv2 = (TextView) findViewById(R.id.textView2);
        Constants.tv3 = (TextView) findViewById(R.id.textView3);
        Constants.tv4 = (TextView) findViewById(R.id.textView4);
        Constants.tv5 = (TextView) findViewById(R.id.textView5);
        Constants.debugPane = (TextView) findViewById(R.id.debugPane);
        Constants.tv7 = (TextView) findViewById(R.id.textView7);
        Constants.tv8 = (TextView) findViewById(R.id.textView8);
        Constants.tv9 = (TextView) findViewById(R.id.textView9);
        Constants.tv10 = (TextView) findViewById(R.id.textView10);
        Constants.tv13 = (TextView) findViewById(R.id.textView13);
        Constants.tv14 = (TextView) findViewById(R.id.textView14);
        Constants.tv15 = (TextView) findViewById(R.id.textView15);
        Constants.tv16 = (TextView) findViewById(R.id.textView16);
        Constants.tv17 = (TextView) findViewById(R.id.textView17);
        Constants.tv18 = (TextView) findViewById(R.id.textView18);
        Constants.tv19 = (TextView) findViewById(R.id.textView19);
        Constants.tv20 = (TextView) findViewById(R.id.textView20);
        Constants.tv21 = (TextView) findViewById(R.id.textView21);
        Constants.msgview = (TextView) findViewById(R.id.msg);
//        Constants.imgview = (ImageView) findViewById(R.id.my_image_view);

        Constants.sview = (NestedScrollView) findViewById(R.id.scrollView);
        Constants.tv5.setMovementMethod(new ScrollingMovementMethod());
        Constants.startButton = (Button) findViewById(R.id.button);
        Constants.clearButton = (Button) findViewById(R.id.button2);
        Constants.stopButton = (Button) findViewById(R.id.button3);
        Constants.tv6 = (TextView) findViewById(R.id.textView6);
        av = this;

        //Constants.sw2.setVisibility(View.GONE);
        // Constants.sw3.setVisibility(View.GONE);
        //Constants.sw4.setVisibility(View.GONE);
        //Constants.sw5.setVisibility(View.GONE);
        Constants.sw6.setVisibility(View.GONE);
        Constants.sw7.setVisibility(View.GONE);
        //Constants.sw8.setVisibility(View.GONE);
        Constants.sw9.setVisibility(View.GONE);

        Constants.et2.setVisibility(View.GONE);
//        Constants.et4.setVisibility(View.GONE);
        Constants.et5.setVisibility(View.GONE);
        //Constants.et6.setVisibility(View.GONE);
        //Constants.et7.setVisibility(View.GONE);
        //Constants.et8.setVisibility(View.GONE);
        Constants.et11.setVisibility(View.GONE);
//        Constants.et13.setVisibility(View.GONE);
        Constants.et14.setVisibility(View.GONE);
        Constants.et15.setVisibility(View.GONE);
        Constants.et17.setVisibility(View.GONE);
        Constants.et18.setVisibility(View.GONE);

        Constants.tv2.setVisibility(View.GONE);
        //Constants.tv5.setVisibility(View.GONE);
        Constants.tv7.setVisibility(View.GONE);
        //Constants.tv8.setVisibility(View.GONE);
        //Constants.tv9.setVisibility(View.GONE);
        //Constants.tv10.setVisibility(View.GONE);
        //Constants.tv13.setVisibility(View.GONE);
//        Constants.tv14.setVisibility(View.GONE);
        Constants.tv15.setVisibility(View.GONE);
//        Constants.tv17.setVisibility(View.GONE);
        Constants.tv18.setVisibility(View.GONE);
        Constants.tv19.setVisibility(View.GONE);
        Constants.tv20.setVisibility(View.GONE);
        Constants.tv21.setVisibility(View.GONE);

        //Constants.spinner2.setVisibility(View.GONE);

        Constants.sw1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                if (isChecked) {
                    editor.putString("user", Constants.User.Alice.toString());
                    Constants.user  = Constants.User.Alice;
                    Constants.sw1.setText(Constants.User.Alice.toString());
                    Constants.sw2.setEnabled(false);
                }
                else {
                    editor.putString("user", Constants.User.Bob.toString());
                    Constants.user  = Constants.User.Bob;
                    Constants.sw1.setText(Constants.User.Bob.toString());
                    Constants.sw2.setEnabled(true);
                }
                editor.commit();
            }
        });

        Constants.sw2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("decode_data", isChecked);
                Constants.DecodeData  = isChecked;
                editor.commit();
            }
        });

        Constants.sw3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("test", isChecked);
                Constants.TEST  = isChecked;
                editor.commit();
            }
        });

        Constants.sw4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("imu", isChecked);
                Constants.imu  = isChecked;
                editor.commit();
            }
        });

        Constants.sw5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("stereo", isChecked);
                Constants.stereo  = isChecked;
                editor.commit();
            }
        });

        Constants.sw6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                if (isChecked) {
                    editor.putString("est_sig", Constants.EstSignalType.Chirp.toString());
                    Constants.est_sig = Constants.EstSignalType.Chirp;
                }
                else {
                    editor.putString("est_sig", Constants.EstSignalType.Symbol.toString());
                    Constants.est_sig = Constants.EstSignalType.Symbol;
                }
                Constants.sw6.setText(Constants.est_sig.toString());
                editor.commit();
            }
        });

        Constants.sw7.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("feed_pre", isChecked);
                Constants.feedbackPreamble  = isChecked;
                editor.commit();
            }
        });

        Constants.sw8.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("gap", isChecked);
                Constants.ADD_GAP  = isChecked;
                Constants.updateChirp_Parameters();
                editor.commit();
            }
        });

        Constants.sw9.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("flip_symbol", isChecked);
                Constants.FLIP_SYMBOL  = isChecked;
                editor.commit();
            }
        });

        Constants.sw10.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("naiser", isChecked);
                Constants.NAISER  = isChecked;
                Constants.updateNaiser(MainActivity.av);
                editor.commit();
            }
        });

        Constants.sw11.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                editor.putBoolean("check_sym", isChecked);
                Constants.CHECK_SYM  = isChecked;
                editor.commit();
            }
        });

//        Constants.sw12.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (!isChecked) {
//                    stopMethod();
//                }
//                else {
//                    Constants.user  = Constants.User.Bob;
//
//                    startMethod(av);
//                }
//            }
//        });

        Constants.et1.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et1.getText().toString();
                Utils.checkTextInput(Constants.VolumeCandidates, Constants.et1, ss);

                if (Utils.isFloat(ss)) {
                    try {
                        editor.putFloat("volume", Float.parseFloat(ss));
                        editor.commit();
                        Constants.volume = Float.parseFloat(ss);
                    }
                    catch(Exception e) {
                        Log.e(Constants.LOG,e.getMessage());
                    }
                }
            }
        });

//        Constants.et2.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void afterTextChanged(Editable s) {
//            }
//            @Override
//            public void beforeTextChanged(CharSequence s, int start,
//                                          int count, int after) {
//            }
//            @Override
//            public void onTextChanged(CharSequence s, int start,
//                                      int before, int count) {
//                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
//                String ss = Constants.et2.getText().toString();
//                if (Utils.isInteger(ss)) {
//                    editor.putInt("preamble_len", Integer.parseInt(ss));
//                    editor.commit();
//                    Constants.preambleTime = Integer.parseInt(ss);
//                }
//            }
//        });

        Constants.et3.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et3.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("init_sleep", Integer.parseInt(ss));
                    editor.commit();
                    Constants.initSleep = Integer.parseInt(ss);
                }
            }
        });

        // send delay text input
        Constants.et4.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et4.getText().toString();

                Utils.checkTextInput(Constants.SendDelay_Candidates, Constants.et4, ss);

                if (Utils.isInteger(ss)) {
                    editor.putInt("Send_Delay", Integer.parseInt(ss));
                    Constants.Send_Delay = Integer.parseInt(ss);
//                    }
                    editor.commit();
                }
            }
        });

        Constants.et5.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et5.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("nsyms", Integer.parseInt(ss));
                    editor.commit();
                    Constants.Nsyms = Integer.parseInt(ss);
                }
            }
        });

        // bandwidth text input
        Constants.et6.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et6.getText().toString();

                Utils.checkTextInput(Constants.BW_Candidates, Constants.et6, ss);

                if (Utils.isInteger(ss)) {
                    editor.putInt("BW", Integer.parseInt(ss));
                    editor.commit();
                    Constants.BW = Integer.parseInt(ss);
                    Constants.updateChirp_Parameters();
                    //Constants.updateNbins();
                }
            }
        });

        // FC carrier frequency text input
        Constants.et7.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et7.getText().toString();

                Utils.checkTextInput(Constants.FC_Candidates, Constants.et7, ss);

                if (Utils.isInteger(ss)) {
                    editor.putInt("FC", Integer.parseInt(ss));
                    editor.commit();
                    Constants.FC = Integer.parseInt(ss);
                    Constants.updateChirp_Parameters();
                    //Constants.updateNbins();
                }
            }
        });

        // sf text input
        Constants.et8.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et8.getText().toString();

                Utils.checkTextInput(Constants.SF_Candidates, Constants.et8, ss);

                if (Utils.isInteger(ss)) {
                    editor.putInt("SF", Integer.parseInt(ss));
                    editor.commit();
                    Constants.SF = Integer.parseInt(ss);
                    Constants.updateChirp_Parameters();
                }
            }
        });

        // n measurement text input
        Constants.et9.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et9.getText().toString();

                Utils.checkTextInput(Constants.Mattempts_Candidates, Constants.et9, ss);

                if (Utils.isInteger(ss)) {
                    editor.putInt("mattempts", Integer.parseInt(ss));
                    editor.commit();
                    Constants.mattempts = Integer.parseInt(ss);
                }
            }
        });

        Constants.et10.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et10.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("exp_num", Integer.parseInt(ss));
                    editor.commit();
                    Constants.exp_num = Integer.parseInt(ss);
                }
            }
        });
        Constants.et12.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et12.getText().toString();
                if (Utils.isFloat(ss)) {
                    editor.putFloat("scale_factor", Float.parseFloat(ss));
                    editor.commit();
                    Constants.FreAdaptScaleFactor = Float.parseFloat(ss);
                }
            }
        });
        Constants.et13.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et13.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("snr_thresh2_2", Integer.parseInt(ss));
                    editor.commit();
                    Constants.SNR_THRESH2_2 = Integer.parseInt(ss);
                }
            }
        });

        Constants.et14.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et14.getText().toString();
                if (Utils.isFloat(ss)) {
                    editor.putFloat("xcorr_thresh", (float)Double.parseDouble(ss));
                    editor.commit();
                    Constants.MinXcorrVal = Double.parseDouble(ss);
                }
            }
        });

        Constants.et15.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et15.getText().toString();
                if (Utils.isFloat(ss)) {
                    editor.putFloat("xcorr_thresh2", (float)Double.parseDouble(ss));
                    editor.commit();
                    Constants.XCORR_MAX_VAL_HEIGHT_FAC = Double.parseDouble(ss);
                }
            }
        });

        Constants.et17.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et17.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("var_thresh", Integer.parseInt(ss));
                    editor.commit();
                    Constants.VAR_THRESH = Integer.parseInt(ss);
                }
            }
        });
        Constants.et18.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et18.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("xcorr_above_thresh", Integer.parseInt(ss));
                    editor.commit();
                    Constants.XcorrAboveThresh = Integer.parseInt(ss);
                }
            }
        });

        Constants.et25.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et25.getText().toString();
                if (Utils.isFloat(ss)) {
                    editor.putFloat("naiser_thresh", Float.parseFloat(ss));
                    editor.commit();
                    Constants.NaiserThresh = Float.parseFloat(ss);
                }
            }
        });

        Constants.et26.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et26.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("feedback_thresh", Integer.parseInt(ss));
                    editor.commit();
                    Constants.FEEDBACK_SNR_THRESH = Integer.parseInt(ss);
                }
            }
        });

        Constants.et27.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(av).edit();
                String ss = Constants.et27.getText().toString();
                if (Utils.isInteger(ss)) {
                    editor.putInt("checksym_snrthresh", Integer.parseInt(ss));
                    editor.commit();
                    Constants.CheckSymSNRThresh = Integer.parseInt(ss);
                }
            }
        });

//        ImageView iv1=(ImageView)findViewById(R.id.imageView);
//        ImageView iv2=(ImageView)findViewById(R.id.imageView2);
//        ImageView iv3=(ImageView)findViewById(R.id.imageView3);
//        ImageView iv4=(ImageView)findViewById(R.id.imageView4);
//        ImageView iv5=(ImageView)findViewById(R.id.imageView5);
//        ImageView iv6=(ImageView)findViewById(R.id.imageView6);
//        ImageView iv7=(ImageView)findViewById(R.id.imageView7);
//        ImageView iv8=(ImageView)findViewById(R.id.imageView8);
//        ImageView iv9=(ImageView)findViewById(R.id.imageView9);
//        ImageView iv10=(ImageView)findViewById(R.id.imageView10);
//        ImageView iv11=(ImageView)findViewById(R.id.imageView11);
//        ImageView iv12=(ImageView)findViewById(R.id.imageView12);
//        ImageView iv13=(ImageView)findViewById(R.id.imageView13);
//        ImageView iv14=(ImageView)findViewById(R.id.imageView14);
//        ImageView iv15=(ImageView)findViewById(R.id.imageView15);
//        ImageView iv16=(ImageView)findViewById(R.id.imageView16);
//        ImageView iv17=(ImageView)findViewById(R.id.imageView17);
//        ImageView iv18=(ImageView)findViewById(R.id.imageView18);
//        ImageView iv19=(ImageView)findViewById(R.id.imageView19);
//        ImageView iv20=(ImageView)findViewById(R.id.imageView20);
//        ImageView iv21=(ImageView)findViewById(R.id.imageView21);
//        ImageView iv22=(ImageView)findViewById(R.id.imageView22);
//        ImageView iv23=(ImageView)findViewById(R.id.imageView23);
//        ImageView iv24=(ImageView)findViewById(R.id.imageView24);
//        ImageView iv25=(ImageView)findViewById(R.id.imageView25);
        Constants.vv = (View)findViewById(R.id.myview);
//
//        if (!Constants.SPEECH_IN) {
//            iv25.setVisibility(View.GONE);
//        }
//
//        iv1.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=1;startWrapper(); }});
//        iv2.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=2;startWrapper(); }});
//        iv3.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=3;startWrapper(); }});
//        iv4.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=4;startWrapper(); }});
//        iv5.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=5;startWrapper(); }});
//        iv6.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=6;startWrapper(); }});
//        iv7.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=7;startWrapper(); }});
//        iv8.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=8;startWrapper(); }});
//        iv9.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=9;startWrapper(); }});
//        iv10.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=10;startWrapper(); }});
//        iv11.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=11;startWrapper(); }});
//        iv12.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=12;startWrapper(); }});
//        iv13.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=13;startWrapper(); }});
//        iv14.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=14;startWrapper(); }});
//        iv15.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=15;startWrapper(); }});
//        iv16.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=16;startWrapper(); }});
//        iv17.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=17;startWrapper(); }});
//        iv18.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=18;startWrapper(); }});
//        iv19.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=19;startWrapper(); }});
//        iv20.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=20;startWrapper(); }});
//        iv21.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=21;startWrapper(); }});
//        iv22.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=22;startWrapper(); }});
//        iv23.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=23;startWrapper(); }});
//        iv24.setOnClickListener(new View.OnClickListener() {public void onClick(View view) { Constants.messageID=24;startWrapper(); }});
//
//        iv25.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent
//                        = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
//                        Locale.getDefault());
//                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");
//
//                try {
//                    startActivityForResult(intent, 1);
//                }
//                catch (Exception e) {
//                    Toast.makeText(MainActivity.this, " " + e.getMessage(),
//                            Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }

    // ********************************** End App Long Methods **********************************


    // Sensor related
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                String textresult = Objects.requireNonNull(result).get(0);
                int mindist = Integer.MAX_VALUE;
                int minval = 1;
                for (int i = 1; i<=24; i++) {
                    int dist = Utils.LevenshteinDistance(textresult,Constants.mmap.get(i));
                    if (dist<mindist) {
                        mindist=dist;
                        minval=i;
                    }
                }
                Utils.log("voicematch "+mindist+","+minval+","+Constants.mmap.get(minval));
                Constants.messageID=minval;
                startWrapper();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (Constants.sensorFlag && Constants.imu) {
            if (event.sensor.equals(accelerometer)) {
                Constants.acc.add(event.values[0]+","+event.values[1]+","+event.values[2]+"\n");
            }
            else if (event.sensor.equals(gyroscope)) {
                Constants.gyro.add(event.values[0]+","+event.values[1]+","+event.values[2]+"\n");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

}
