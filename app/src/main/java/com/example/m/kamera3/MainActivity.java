package com.example.m.kamera3;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//solution based on https://stackoverflow.com/questions/25721410/how-to-detect-color-of-the-center-of-camera-streaming-in-android-without-opencv
class MainActivity extends Activity implements SurfaceHolder.Callback {
    // a variable to store a reference to the Image View at the main.xml file
    private ImageView iv_image;
    // a variable to store a reference to the Surface View at the main.xml file
    private SurfaceView sv;

    // a bitmap to display the captured image
    private Bitmap bmp;

    // Camera variables
// a surface holder
    private SurfaceHolder sHolder;
    // a variable to control the camera
    Camera mCamera;
    // the camera parameters
    private Camera.Parameters parameters;
    Camera.PictureCallback mCall;
    Button takePicture;
    public Handler handler = new Handler();
    TextView colorName, Hex;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    int width = 0, height = 0;
    private Camera.Size pictureSize;

    boolean mStopHandler = false;
    private static final HashMap<String, String> sColorNameMap;
    private static final HashMap<String, float[]> sColorRGBNameMap;

    private boolean safeToTakePicture = true;

    public Switch swithMode;

    static {
        // zielony, czerwony, czarny, biały, żółty, różowy, niebieski, pomarańczowy
        sColorNameMap = new HashMap();
        sColorNameMap.put("#000000", "black");
        sColorNameMap.put("#0000FF", "blue");
        sColorNameMap.put("#00FF00", "green");
        sColorNameMap.put("#FF0000", "red");
        sColorNameMap.put("#FFFFFF", "white");
        sColorNameMap.put("#FFFF00", "yellow");
        sColorNameMap.put("#FFA500", "orange");
        sColorNameMap.put("#FF00FF", "pink");
        // .....

    }

    static {
        // zielony, czerwony, czarny, biały, żółty, różowy, niebieski, pomarańczowy
        sColorRGBNameMap = new HashMap();
        sColorRGBNameMap.put("black", new float[]{0, 0, 0});
        sColorRGBNameMap.put("blue", new float[]{0, 0, 255});
        sColorRGBNameMap.put("green", new float[]{0, 255, 0});
        sColorRGBNameMap.put("red", new float[]{255, 0, 0});
        sColorRGBNameMap.put("white", new float[]{255, 255, 255});
        sColorRGBNameMap.put("yellow", new float[]{255, 255, 0});
        sColorRGBNameMap.put("orange", new float[]{255, 165, 0});
        sColorRGBNameMap.put("pink", new float[]{255, 0, 255});
    }


    Runnable runnable = new Runnable() {
        @Override
        public void run() {

            if (mCamera != null && safeToTakePicture /*&& swithMode.isChecked()*/) {
                mCamera.takePicture(null, null, mCall);
                safeToTakePicture = false;
                Log.i("Runnable", "Taken pic in runnable");
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
                run();
                //if (!mStopHandler) {
                //Thread thread = new Thread() {void post(){
                //handler.postDelayed(this,100);
                //}
                //};
                //thread.start();
                //}
            }
            handler.postDelayed(this, 400);
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();

        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        editor = pref.edit();
        colorName = findViewById(R.id.colorName);
        Hex = findViewById(R.id.colorHex);
        // get the Image View at the main.xml file
        iv_image = findViewById(R.id.imageView);
        takePicture = findViewById(R.id.takePicture);

        swithMode = findViewById(R.id.switchMode);

        // get the Surface View at the main.xml file
        sv = findViewById(R.id.surfaceView);

        // Get a surface
        sHolder = sv.getHolder();

        // add the callback interface methods defined below as the Surface View
        // callbacks
        sHolder.addCallback(this);

        // tells Android that this surface will have its data constantly
        // replaced
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        takePicture.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                if (safeToTakePicture) {
//                    mCamera.takePicture(null, null, mCall);
//                }
//            }
//        });
        sv.setOnTouchListener(new SurfaceView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (/*!swithMode.isChecked() &&*/ event.getAction() == MotionEvent.ACTION_UP) {
                    x = (int) event.getX();
                    y = (int) event.getY();
                    Toast.makeText(getApplicationContext(), "x: " + x + "; y: " + y,
                            Toast.LENGTH_SHORT).show();
                    //ServicePhoto();
                }
                return true;
            }
        });
    }

    int x = -1;
    int y = -1;

    @Override
    public void surfaceChanged(SurfaceHolder sv, int arg1, int arg2, int arg3) {
        // get camera parameters
        parameters = mCamera.getParameters();
        // parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setDisplayOrientation(90);
        setBesttPictureResolution();

        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewDisplay(sv);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mCamera.setParameters(parameters);
        // set camera parameters
        mCamera.startPreview();
        //safeToTakePicture = true;

        // if (swithMode.isChecked()) {
        ServicePhoto();
        //   }
    }

    private void ServicePhoto() {
        // sets what code should be executed after the picture is taken

        mCall = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                String source = "pic";
                // decode the data obtained by the camera into a Bitmap
                if (data != null) {
                    bmp = decodeBitmap(data);

                    // set the iv_image
                    if (bmp != null) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 5, bytes);
                        Bitmap resizebitmap;

                        if (x == -1) {
                            resizebitmap = Bitmap.createScaledBitmap(bmp,
                                    bmp.getWidth() / bmp.getWidth(), bmp.getHeight() / bmp.getHeight(), false);
                        } else {
                            try {
                                source = "tap";
                                int w_x = y * bmp.getWidth() / sv.getHeight() - 19;
                                int h_y = (sv.getWidth() - x) * bmp.getHeight() / sv.getWidth() - 19;
                                resizebitmap = Bitmap.createBitmap(bmp, w_x
                                        , h_y, 20, 20);
//                                resizebitmap = Bitmap.createBitmap(bmp, y * bmp.getWidth() / sv.getHeight() - 19
//                                        , (sv.getHeight() - x) * bmp.getHeight() / sv.getWidth() - 19, 20, 20);
                            } catch (Exception e) {
                                int w_x = y * bmp.getWidth() / sv.getHeight() - 10;
                                int h_y = (sv.getWidth() - x) * bmp.getHeight() / sv.getWidth() - 10;
                                resizebitmap = Bitmap.createBitmap(bmp, w_x
                                        , h_y, 10, 10);
                            }
                        }

                        resizebitmap = Bitmap.createScaledBitmap(resizebitmap,
                                1, 1, false);

                        //iv_image.setImageBitmap(rotateImage(resizebitmap, 90));
                        iv_image.setImageBitmap(resizebitmap);

                        int color = getAverageColor(resizebitmap);
                        float[] colorArray = getAverageColorArray(resizebitmap);
                        Log.i("Color Int", color + "");
                        // int color =
                        // resizebitmap.getPixel(resizebitmap.getWidth()/2,resizebitmap.getHeight()/2);

                        String strColor = String.format("#%06X", 0xFFFFFF & color);
                        Hex.setText("hex color: " + strColor + "(" + source + ")");
                        //String colorname = sColorNameMap.get(strColor);
                        String colorname = ClassifyColor(colorArray);
                        if (colorName != null) {
                            colorName.setText("color name: " + colorname);
                        }

                        Log.i("Pixel Value",
                                "Top Left pixel: " + Integer.toHexString(color));
                    }
                }
                camera.startPreview();
                safeToTakePicture = true;
                x = -1;
                y = -1;
            }
        };
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw the preview.
        //mCamera = Camera.open();
        mCamera = getCameraInstance();
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                handler.post(runnable);

            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
            }
        } else
            Toast.makeText(getApplicationContext(), "Camera is not available",
                    Toast.LENGTH_SHORT).show();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        if (mCamera != null) {
            // stop the preview
            mCamera.stopPreview();
            // release the camera
            mCamera.release();
        }
        // unbind the camera from this object
        if (handler != null)
            handler.removeCallbacks(runnable);
    }

    public static Bitmap decodeBitmap(byte[] data) {

        Bitmap bitmap = null;
        BitmapFactory.Options bfOptions = new BitmapFactory.Options();
        bfOptions.inDither = false; // Disable Dithering mode
        bfOptions.inPurgeable = true; // Tell to gc that whether it needs free
        // memory, the Bitmap can be cleared
        bfOptions.inInputShareable = true; // Which kind of reference will be
        // used to recover the Bitmap data
        // after being clear, when it will
        // be used in the future
        bfOptions.inTempStorage = new byte[32 * 1024];

        if (data != null)
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
                    bfOptions);

        return bitmap;
    }

    public Bitmap rotateImage(Bitmap src, float degree) {
        // create new matrix object
        Matrix matrix = new Matrix();
        // setup rotation degree
        matrix.postRotate(degree);
        // return new bitmap rotated using matrix
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(),
                matrix, true);
    }

    public int getAverageColor(Bitmap bitmap) {
        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int pixelCount = 0;

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int c = bitmap.getPixel(x, y);

                pixelCount++;
                redBucket += Color.red(c);
                greenBucket += Color.green(c);
                blueBucket += Color.blue(c);
                // does alpha matter?
            }
        }

        int averageColor = Color.rgb(redBucket / pixelCount, greenBucket
                / pixelCount, blueBucket / pixelCount);
        return averageColor;
    }

    public float[] getAverageColorArray(Bitmap bitmap) {
        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int pixelCount = 0;

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int c = bitmap.getPixel(x, y);

                pixelCount++;
                redBucket += Color.red(c);
                greenBucket += Color.green(c);
                blueBucket += Color.blue(c);
                // does alpha matter?
            }
        }

//        int averageColor = Color.rgb(redBucket / pixelCount, greenBucket
//                / pixelCount, blueBucket / pixelCount);
        return new float[]{(float) (redBucket / pixelCount)
                , (float) (greenBucket / pixelCount)
                , (float) (blueBucket / pixelCount)};
        //return averageColor;
    }

    public String ClassifyColor(float[] rgb) {
        Iterator it = sColorRGBNameMap.entrySet().iterator();
        String output = "";
        float minValue = -1f;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            float currValue = CalculateRGBDifference(rgb, (float[]) pair.getValue());
            if (minValue == -1 || minValue > currValue) {
                output = (String) pair.getKey();
                minValue = currValue;
            }
        }
        return output;
    }

    float CalculateRGBDifference(float[] caughtRgb, float[] baseRgb) {
        return (float) Math.sqrt(Math.pow(caughtRgb[0] - baseRgb[0], 2)
                + Math.pow(caughtRgb[1] - baseRgb[1], 2)
                + Math.pow(caughtRgb[2] - baseRgb[2], 2));
    }

    int[] averageARGB(Bitmap pic) {
        int A, R, G, B;
        A = R = G = B = 0;
        int pixelColor;
        int width = pic.getWidth();
        int height = pic.getHeight();
        int size = width * height;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                pixelColor = pic.getPixel(x, y);
                A += Color.alpha(pixelColor);
                R += Color.red(pixelColor);
                G += Color.green(pixelColor);
                B += Color.blue(pixelColor);
            }
        }

        A /= size;
        R /= size;
        G /= size;
        B /= size;

        int[] average = {A, R, G, B};
        return average;

    }

    private void setBesttPictureResolution() {
        // get biggest picture size
        width = pref.getInt("Picture_Width", 0);
        height = pref.getInt("Picture_height", 0);

        if (width == 0 | height == 0) {
            pictureSize = getBiggesttPictureSize(parameters);
            if (pictureSize != null)
                parameters
                        .setPictureSize(pictureSize.width, pictureSize.height);
            // save width and height in sharedprefrences
            width = pictureSize.width;
            height = pictureSize.height;
            editor.putInt("Picture_Width", width);
            editor.putInt("Picture_height", height);
            editor.commit();

        } else {
            // if (pictureSize != null)
            parameters.setPictureSize(width, height);
        }
    }

    private Camera.Size getBiggesttPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea > resultArea) {
                    result = size;
                }
            }
        }

        return (result);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    float touchX;
    float touchY;

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        float eventX = event.getX();
//        float eventY = event.getY();
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                touchX = eventX;
//                touchY = eventY;
//                //return true;
////                if (isBetween(originX, (int) (touchX * 0.9), (int) (touchX * 1.1))
////                        && isBetween(originY, (int) (touchY * 0.9), (int) (touchY * 1.1))) {
////                    isTouched = true;
////                }
//                break;
//            case MotionEvent.ACTION_MOVE:
////                touchX = eventX;
////                touchY = eventY;
////                if (isTouched) {
////                    originX = (int) touchX;
////                    originY = (int) touchY;
////                }
//                break;
//            case MotionEvent.ACTION_UP:
////                    touchX = eventX;
////                    touchY = eventY;
////                isTouched = false;
//                break;
//            default:
//                return false;
//        }
//
//        //invalidate();
//        return true;
//    }
}