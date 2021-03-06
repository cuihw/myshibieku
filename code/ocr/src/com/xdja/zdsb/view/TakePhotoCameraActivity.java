package com.xdja.zdsb.view;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xdja.zdsb.R;
import com.xdja.zdsb.utils.CutPhoto;
import com.xdja.zdsb.utils.Zzlog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

//示例拍照并调用识别将结果显示在这个程序结果界面
public class TakePhotoCameraActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "CameraActivity";

    public static final String PATH = Environment.getExternalStorageDirectory().toString() + "/wtimage/";

    private String strCaptureFilePath = PATH + "/camera_snap.jpg";

    public static final Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    // 预览尺寸 默认设置
    public int WIDTH = 320;// 640;//1024;

    public int HEIGHT = 240;// 480;//768;
    // 拍摄尺寸 默认设置

    public int srcwidth = 2048;// 1600;//2048;final

    public int srcheight = 1536;// 1200;//1536;final
    // 裁切尺寸

    private int cutwidth = 1300;// 1845;//1100;

    private int cutheight = 200;// 1155;//750;
    // 证件类型

    int nMainID = 0;

    String imagename = "";

    private ImageButton backbtn, confirmbtn, resetbtn, takepicbtn, lighton, lightoff, cuton, cutoff;

    private TextView back_reset_text, take_recog_text, light_text, cut_text;

    private Camera camera;

    private SurfaceView surfaceView;

    private SurfaceHolder surfaceHolder;

    private ToneGenerator tone;

    private ImageView imageView;

    private Bitmap bitmap;

    public Map<Object, Object> imagemap = new HashMap<Object, Object>();

    private byte[] imagedata;

    private RelativeLayout rlyaout;

    private Boolean cut = true;

    private List<String> focusModes;

    private String path = "";

    public long fastClick = 0;

    public int recogType = -1;// 自动识别、划线识别

    public boolean isVinRecog;

    private int width, height;

    private ImageView top_left, top_right, bottom_left, bottom_right, left_cut, right_cut;

    private SensorManager sManager = null;

    private SensorEventListener myListener = null;

    private float x, y, z;

    private final int UPTATE_Difference_TIME = 200;

    private int count = 0;

    private final float MoveDifference = 0.08f;

    private final float MoveDifferencemin = 0.001f;

    private long last_Time;

    private Boolean noShakeBoolean = false;

    private List<float[]> mlist;

    private int layout_width;

    private final int ListMaxLen = 3;

    public static final int KEYCODE_T = 27;

    public static final int KEYCODE_F1 = 131;

    public static final int KEYCODE_F2 = 132;

    public static final int KEYCODE_F3 = 133;

    private boolean isAutoTakePic = false;// 是否设置自动拍照识别功能

    Handler handler = new Handler();

    Runnable runnable = new Runnable() {

        @Override
        public void run() {
            // 三星在对焦设置为两秒的时候会抛出异常
            handler.postDelayed(this, 3000);
            autoFocus();

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        noShakeBoolean = false;
        if (readPreferences("", "isAutoTakePic") == 1)
            isAutoTakePic = true;
        else if (readPreferences("", "isAutoTakePic") == 0)
            isAutoTakePic = false;

        if (!isAutoTakePic)
            handler.postDelayed(runnable, 3000);// 每两秒秒执行一次runnable.启动程序

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;

        System.out.println("CameraActivity....  屏幕的宽和高:" + width + "--" + height);
        setContentView(R.layout.take_photo_camera_activity);
        if (nMainID == 0) {
            String cfg = "";
            try {
                cfg = readtxt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            String cfgs[] = cfg.split("==##");
            if (cfgs != null && cfgs.length >= 2) {
                if (cfgs[0] != null && !cfgs[0].equals("")) {
                    try {
                        nMainID = Integer.parseInt(cfgs[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // 设置拍摄尺寸
        Intent intentget = this.getIntent();

        srcwidth = width;
        srcheight = height;
        System.out.println("srcwidth:" + srcwidth + "--" + ", srcheight:" + srcheight);
        recogType = intentget.getIntExtra("recogType", 1);
        WIDTH = 960;
        HEIGHT = 720;

        nMainID = 2;

        findview();
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mlist = new ArrayList<float[]>();
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // 监听传感器事件
        myListener = new SensorEventListener() {

            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }

            public void onSensorChanged(SensorEvent event) {
                long now_Time = System.currentTimeMillis();// 获取当前时间
                long time_Difference = now_Time - last_Time;

                if (time_Difference >= UPTATE_Difference_TIME) {
                    // System.out.println("time_Difference:" + time_Difference);
                    last_Time = now_Time;
                    x = event.values[SensorManager.DATA_X];
                    y = event.values[SensorManager.DATA_Y];
                    z = event.values[SensorManager.DATA_Z];

                    double move_Difference = getStableFloat(x, y, z);
                    if (count < 5) {
                        count++;
                    }
                    // else if(count==10&&afterCount<=4)
                    // {
                    // afterCount++;
                    // }
                    // System.out.println("次数:" + count );
                    if (isAutoTakePic) {
                        if (move_Difference <= MoveDifference && move_Difference >= MoveDifferencemin && count == 5) {

                            if (!noShakeBoolean) {
                                noShakeBoolean = true;
                                handler.postDelayed(runnable, 3000);// 每两秒秒执行一次runnable.启动程序
                                takePicture();
                                // if(afterCount==4){
                                // camera.takePicture(shutterCallback, null,
                                // PictureCallback);
                                // }
                            }
                        } else if (move_Difference > MoveDifference) {
                            count = 0;
                            // System.out.println("count:" + count);
                            noShakeBoolean = false;
                            handler.removeCallbacks(runnable);// 停止计时器，每当拍照或退出时都要执行这段代码。
                        }
                    }
                }
            }
        };
        sManager.registerListener(myListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void findview() {
        back_reset_text = (TextView) findViewById(R.id.back_and_reset_text);
        back_reset_text.setTextColor(Color.BLACK);
        take_recog_text = (TextView) findViewById(R.id.take_and_confirm_text);
        take_recog_text.setTextColor(Color.BLACK);
        light_text = (TextView) findViewById(R.id.light_on_off_text);
        light_text.setTextColor(Color.BLACK);
        cut_text = (TextView) findViewById(R.id.cut_on_off_text);
        cut_text.setTextColor(Color.BLACK);

        int button_width = (int) (height * 0.125);
        int button_distance = (int) (height * 0.1);

        RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(button_width, button_width);
        lParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        lParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        lParams.topMargin = button_distance;
        backbtn = (ImageButton) findViewById(R.id.backbtn);
        backbtn.setLayoutParams(lParams);
        backbtn.setOnClickListener(new mClickListener());
        resetbtn = (ImageButton) findViewById(R.id.reset_btn);
        resetbtn.setLayoutParams(lParams);
        resetbtn.setOnClickListener(new mClickListener());

        lParams = new RelativeLayout.LayoutParams(button_width, button_width);
        lParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        lParams.addRule(RelativeLayout.BELOW, R.id.backbtn);
        lParams.topMargin = button_distance;
        takepicbtn = (ImageButton) findViewById(R.id.takepic_btn);
        takepicbtn.setLayoutParams(lParams);
        takepicbtn.setOnClickListener(new mClickListener());
        confirmbtn = (ImageButton) findViewById(R.id.confirm_btn);
        confirmbtn.setLayoutParams(lParams);
        confirmbtn.setOnClickListener(new mClickListener());

        lParams = new RelativeLayout.LayoutParams(button_width, button_width);
        lParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        lParams.addRule(RelativeLayout.BELOW, R.id.confirm_btn);
        lParams.topMargin = button_distance;
        lighton = (ImageButton) findViewById(R.id.lighton);
        lighton.setLayoutParams(lParams);
        lighton.setOnClickListener(new mClickListener());
        lightoff = (ImageButton) findViewById(R.id.lightoff);
        lightoff.setLayoutParams(lParams);
        lightoff.setOnClickListener(new mClickListener());

        lParams = new RelativeLayout.LayoutParams(button_width, button_width);
        lParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        lParams.addRule(RelativeLayout.BELOW, R.id.lighton);
        lParams.topMargin = button_distance;
        cuton = (ImageButton) findViewById(R.id.cuton);
        cuton.setLayoutParams(lParams);
        cuton.setOnClickListener(new mClickListener());
        cutoff = (ImageButton) findViewById(R.id.cutoff);
        cutoff.setLayoutParams(lParams);
        cutoff.setOnClickListener(new mClickListener());

        top_left = (ImageView) findViewById(R.id.topleft);
        top_right = (ImageView) findViewById(R.id.topright);
        bottom_left = (ImageView) findViewById(R.id.bottomleft);
        bottom_right = (ImageView) findViewById(R.id.bottomright);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams((int) (height * 0.18),
                (int) (height * 0.18));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        top_left.setLayoutParams(layoutParams);

        layoutParams = new RelativeLayout.LayoutParams((int) (height * 0.18), (int) (height * 0.18));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.idcard_rightlyaout);
        top_right.setLayoutParams(layoutParams);

        layoutParams = new RelativeLayout.LayoutParams((int) (height * 0.18), (int) (height * 0.18));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        bottom_left.setLayoutParams(layoutParams);

        layoutParams = new RelativeLayout.LayoutParams((int) (height * 0.18), (int) (height * 0.18));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.idcard_rightlyaout);
        bottom_right.setLayoutParams(layoutParams);

        int margin = 0;
        int cutImageLayoutHeight = 0;
        if (srcwidth == 1280 || srcwidth == 960) {
            margin = (int) ((height * 1.333) * 0.165);
            cutImageLayoutHeight = (int) (height * 0.135);
        }
        if (srcwidth == 1600 || srcwidth == 1200) {
            margin = (int) ((height * 1.333) * 0.19);
            cutImageLayoutHeight = (int) (height * 0.108);
        }
        if (srcwidth == 2048 || srcwidth == 1536) {
            margin = (int) ((height * 1.333) * 0.22);
            cutImageLayoutHeight = (int) (height * 0.13);
        }
        left_cut = (ImageView) findViewById(R.id.leftcut);
        right_cut = (ImageView) findViewById(R.id.rightcut);
        layoutParams = new RelativeLayout.LayoutParams((int) (cutImageLayoutHeight * 0.6), cutImageLayoutHeight);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        layoutParams.leftMargin = margin;
        left_cut.setLayoutParams(layoutParams);

        layoutParams = new RelativeLayout.LayoutParams((int) (cutImageLayoutHeight * 0.6), cutImageLayoutHeight);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.idcard_rightlyaout);
        layoutParams.rightMargin = margin;
        right_cut.setLayoutParams(layoutParams);

        imageView = (ImageView) findViewById(R.id.backimageView);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceViwe);
        rlyaout = (RelativeLayout) findViewById(R.id.idcard_rightlyaout);
        if (WIDTH == 1920 && HEIGHT == 1080) {
            layout_width = (int) (width - (height * 1.5));
        } else {
            layout_width = (int) (width - ((height * 4) / 3));
        }
        if (((float) width / height == (float) 4 / 3) || ((float) width / height == (float) 3 / 4)) {
            RelativeLayout.LayoutParams lP = new RelativeLayout.LayoutParams(width / 4, height);
            // lP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
            // RelativeLayout.TRUE);
            lP.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            lP.leftMargin = 3 * width / 4;
            rlyaout.setLayoutParams(lP);
            lP = new RelativeLayout.LayoutParams(width, height);
            lP.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            lP.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            surfaceView.setLayoutParams(lP);
        } else {
            RelativeLayout.LayoutParams lP = new RelativeLayout.LayoutParams(layout_width, height);
            lP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            lP.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            rlyaout.setLayoutParams(lP);
        }
        if (nMainID == 1100 || nMainID == 1101) {
            left_cut.setBackgroundResource(R.drawable.leftcut);
            right_cut.setBackgroundResource(R.drawable.rightcut);
            showTwoCutImageView();
        } else {
            top_left.setBackgroundResource(R.drawable.top_left);
            bottom_left.setBackgroundResource(R.drawable.bottom_left);
            top_right.setBackgroundResource(R.drawable.top_right);
            bottom_right.setBackgroundResource(R.drawable.bottom_right);
            if (((float) width / height == (float) 4 / 3) || ((float) width / height == (float) 3 / 4)) {
                RelativeLayout.LayoutParams lP = new RelativeLayout.LayoutParams(width / 20, width / 30);
                lP.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                lP.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                top_left.setLayoutParams(lP);
                lP = new RelativeLayout.LayoutParams(width / 20, width / 30);
                lP.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                lP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                bottom_left.setLayoutParams(lP);
                lP = new RelativeLayout.LayoutParams(width / 20, width / 30);
                lP.leftMargin = (int) (width * 0.7);
                lP.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                top_right.setLayoutParams(lP);
                lP = new RelativeLayout.LayoutParams(width / 20, width / 30);
                lP.leftMargin = (int) (width * 0.7);
                lP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                bottom_right.setLayoutParams(lP);

            }
            showFourImageView();
        }

        lightoff.setVisibility(View.VISIBLE);
        lighton.setVisibility(View.INVISIBLE);
        light_text.setText(getString(R.string.light2_string));

    }

    private void showTwoCutImageView() {
        left_cut.setVisibility(View.VISIBLE);
        right_cut.setVisibility(View.VISIBLE);
        top_left.setVisibility(View.INVISIBLE);
        top_right.setVisibility(View.INVISIBLE);
        bottom_left.setVisibility(View.INVISIBLE);
        bottom_right.setVisibility(View.INVISIBLE);
    }

    private void hideTwoCutImageView() {
        left_cut.setVisibility(View.INVISIBLE);
        right_cut.setVisibility(View.INVISIBLE);
    }

    private void hideFourImageView() {
        top_left.setVisibility(View.INVISIBLE);
        top_right.setVisibility(View.INVISIBLE);
        bottom_left.setVisibility(View.INVISIBLE);
        bottom_right.setVisibility(View.INVISIBLE);
    }

    private void showFourImageView() {
        left_cut.setVisibility(View.INVISIBLE);
        right_cut.setVisibility(View.INVISIBLE);
        top_left.setVisibility(View.VISIBLE);
        top_right.setVisibility(View.VISIBLE);
        bottom_left.setVisibility(View.VISIBLE);
        bottom_right.setVisibility(View.VISIBLE);

    }

    private class mClickListener implements OnClickListener {

        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.backbtn:
                closeCamera();
                handler.removeCallbacks(runnable);// 停止计时器，每当拍照或退出时都要执行这段代码。
                break;
            // 拍照
            case R.id.takepic_btn:
                handler.removeCallbacks(runnable);// 停止计时器，每当拍照或退出时都要执行这段代码。
                takepicbtn.setEnabled(false);
                takePicture();
                break;
            case R.id.lighton:

                lightoff.setVisibility(View.VISIBLE);
                lighton.setVisibility(View.INVISIBLE);
                light_text.setText(getString(R.string.light2_string));

                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                camera.setParameters(parameters);
                break;
            case R.id.lightoff:
                lighton.setVisibility(View.VISIBLE);
                lightoff.setVisibility(View.INVISIBLE);
                light_text.setText(getString(R.string.light1_string));
                // 关闭闪光灯
                Camera.Parameters parameters2 = camera.getParameters();
                parameters2.setFlashMode(Parameters.FLASH_MODE_OFF);
                camera.setParameters(parameters2);
                break;
            case R.id.cuton:
                cuton.setVisibility(View.INVISIBLE);
                cutoff.setVisibility(View.VISIBLE);
                cut_text.setText("关闭剪裁");
                cut = true;
                break;

            case R.id.cutoff:
                cuton.setVisibility(View.VISIBLE);
                cutoff.setVisibility(View.INVISIBLE);
                cut_text.setText("打开剪裁");
                cut = false;
                break;
            }

        }

    }

    protected int readPreferences(String perferencesName, String key) {
        SharedPreferences preferences = getSharedPreferences(perferencesName, MODE_PRIVATE);
        int result = preferences.getInt(key, 0);
        return result;
    }

    // 读取配置文件
    public String readtxt() throws IOException {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
        }
        String paths = sdDir.toString();
        if (paths.equals("") || paths == null) {
            return "";
        }
        String path = paths + "/AndroidWT/idcard.cfg";
        File file = new File(path);
        if (!file.exists())
            return "";
        FileReader fileReader = new FileReader(path);
        BufferedReader br = new BufferedReader(fileReader);
        String str = "";
        String r = br.readLine();
        while (r != null) {
            str += r;
            r = br.readLine();
        }
        br.close();
        fileReader.close();
        return str;
    }

    public boolean isEffectClick() {
        long lastClick = System.currentTimeMillis();
        long diffTime = lastClick - fastClick;
        if (diffTime > 5000) {
            fastClick = lastClick;
            return true;
        }
        return false;
    }

    /* 拍照对焦 */
    /* 拍照 */
    public void takePicture() {
        if (camera != null) {
            try {
                if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    camera.autoFocus(new AutoFocusCallback() {

                        public void onAutoFocus(boolean success, Camera camera) {

                            if (success) {
                                if (count == 5 || !isAutoTakePic) {
                                    camera.takePicture(shutterCallback, null, PictureCallback);
                                }
                            } else {
                                if (count == 5 || !isAutoTakePic) {
                                    camera.takePicture(shutterCallback, null, PictureCallback);
                                }
                            }

                        }
                    });
                } else {
                    camera.takePicture(shutterCallback, null, PictureCallback);
                }

            } catch (Exception e) {
                e.printStackTrace();
                camera.stopPreview();
                camera.startPreview();
                takepicbtn.setEnabled(true);
                Toast.makeText(this, R.string.toast_autofocus_failure, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "exception:" + e.getMessage());
                // System.out.println( "exception:" + e.getMessage());
            }
        }
    }

    // 快门按下的时候onShutter()被回调拍照声音
    private ShutterCallback shutterCallback = new ShutterCallback() {

        public void onShutter() {
            if (tone == null)
                // 发出提示用户的声音
                tone = new ToneGenerator(1, // AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                        ToneGenerator.MIN_VOLUME);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
    };

    /* 拍照后回显 */
    private PictureCallback PictureCallback = new PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            count = 0;
            Log.i(TAG, "onPictureTaken");
            BitmapFactory.Options opts = new BitmapFactory.Options();
            // 设置成了true,不占用内存，只获取bitmap宽高
            opts.inJustDecodeBounds = true;
            // 根据内存大小设置采样率
            // 需要测试！
            int SampleSize = computeSampleSize(opts, -1, 2048 * 1536);
            opts.inSampleSize = SampleSize;
            opts.inJustDecodeBounds = false;
            opts.inPurgeable = true;
            opts.inInputShareable = true;
            // opts.inNativeAlloc = true;
            // //属性设置为true，可以不把使用的内存算到VM里。SDK默认不可设置这个变量，只能用反射设置。
            try {
                Field field = BitmapFactory.Options.class.getDeclaredField("inNativeAlloc");
                field.set(opts, true);
            } catch (Exception e) {
                Log.i(TAG, "Exception inNativeAlloc");
            }
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            if (srcwidth == 2048 && srcheight == 1536) {
                Matrix matrix = new Matrix();
                matrix.postScale(0.625f, 0.625f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            if (srcwidth == 1600 && srcheight == 1200) {
                Matrix matrix = new Matrix();
                matrix.postScale(0.8f, 0.8f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            imagedata = data;
            /* 创建文件 */
            File dir = new File(PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File myCaptureFile = new File(strCaptureFilePath);
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                /* 采用压缩转档方法 */
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                /* 调用flush()方法，更新BufferStream */
                bos.flush();
                /* 结束OutputStream */
                bos.close();
                // 隐藏焦点图片和行驶证外框

                if (nMainID == 1100 || nMainID == 1101) {
                    hideTwoCutImageView();
                } else {
                    hideFourImageView();
                }

                /* 将拍照下来且保存完毕的图文件，显示出来 */
                imageView.setImageBitmap(bitmap);
                // takepicbtn.setVisibility(View.INVISIBLE);
                // backbtn.setVisibility(View.INVISIBLE);
                // resetbtn.setVisibility(View.VISIBLE);
                // back_reset_text.setText(getString(R.string.reset_btn_string));
                // confirmbtn.setVisibility(View.VISIBLE);
                // take_recog_text.setText(getString(R.string.confirm_btn_string));
                // confirmbtn.setEnabled(true);
                savephoto();
                if (isAutoTakePic) {

                    handler.removeCallbacks(runnable);// 停止计时器，每当拍照或退出时都要执行这段代码。
                }
                resetCamera();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };

    /* 保存图片并送识别 */
    @SuppressLint("SimpleDateFormat")
    private void savephoto() {

        // 系统时间

        // 图像名称
        Date date = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddhhmmss");
        String sysDatetime = fmt.format(date.getTime());

        String name = "idcard_" + sysDatetime + ".jpg";
        // 存储图像（PATH目录）
        // 裁切
        if ((nMainID == 1100 || nMainID == 1101) && recogType == 1) {
            if (srcwidth == 1280 || srcwidth == 960) {
                cutwidth = 750;
                cutheight = 130;
            }
            if (srcwidth == 2048 || srcwidth == 1536) {
                cutwidth = 1300;
                cutheight = 200;
            }
            cutwidth = 750;
            cutheight = 130;
            CutPhoto cutPhoto = new CutPhoto(this, cutwidth, cutheight);
            path = cutPhoto.getCutPhotoPath(bitmap, name.replace(".jpg", "_cut"), PATH);
        } else {
            path = PATH + "/" + name;
        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        if (recogType == 3 && (1100 == nMainID || 1101 == nMainID)) {
            // Intent intent = new Intent(this,
            // RecognizeActivity.class);
            // intent.putExtra("selectPath", path);
            // // 设置识别自动裁切
            // intent.putExtra("iscut", true);
            // intent.putExtra("recogType", recogType);
            // intent.putExtra("nMainID", nMainID);
            // CameraActivity.this.finish();
            // startActivity(intent);
        } else {
            // Intent intent = new Intent(CameraActivity.this,
            // IdcardRunner.class);
            // intent.putExtra("path", path);
            // intent.putExtra("cut", cut);
            // // 设置识别自动裁切
            // intent.putExtra("iscut", true);
            // intent.putExtra("nMainID", nMainID);
            // CameraActivity.this.finish();
            // startActivity(intent);
        }
    }

    @SuppressWarnings("deprecation")
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (camera != null) {
            try {

                Camera.Parameters parameters = camera.getParameters();

                List<Camera.Size> list = parameters.getSupportedPreviewSizes();
                Camera.Size size;
                int previewWidth = 640;
                int previewheight = 480;
                int second_previewWidth = 0;
                int second_previewheight = 0;

                int length = list.size();

                if (length == 1) {
                    size = list.get(0);
                    previewWidth = size.width;
                    previewheight = size.height;
                } else {
                    for (int i = 0; i < length; i++) {
                        size = list.get(i);
                        Zzlog.out(TAG, "size: " + size.width + " , " + size.height);
                        if (size.width <= 960 || size.height <= 720) {
                            second_previewWidth = size.width;
                            second_previewheight = size.height;
                            if (previewWidth <= second_previewWidth) {
                                previewWidth = second_previewWidth;
                                previewheight = second_previewheight;
                            }
                        }
                    }
                }

                parameters.setPictureSize(previewWidth, previewheight);
                Zzlog.out(TAG, " previewWidth: " + previewWidth + " , previewheight" + previewheight);

                parameters.setPictureFormat(PixelFormat.JPEG);
                parameters.setPreviewSize(previewWidth, previewheight);
                parameters.setPictureSize(previewWidth, previewheight);
                camera.setParameters(parameters);
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                focusModes = parameters.getSupportedFocusModes();
            } catch (IOException e) {
                camera.release();
                camera = null;
                e.printStackTrace();
            }
        }
    }

    // 在surface创建时激发
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        // 获得Camera对象
        takepicbtn.setEnabled(true);
        if (null == camera) {
            camera = Camera.open();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        if (camera != null) {
            // isContinue = false;
            camera.stopPreview();
            camera.release();
            camera = null;
        }

    }

    /* 相机重置 */
    private void resetCamera() {
        if (camera != null) {
            camera.stopPreview();

        }
    }

    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128
                : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    @SuppressLint("DefaultLocale")
    public void displayResult() {
        Log.i(TAG, "displayResult");
        if (path != null && !path.equals("")) {
            try {
                String logopath = "";
                // String logopath = getSDPath() + "/photo_logo.png";
                Intent intent = new Intent("wintone.idcard");
                Bundle bundle = new Bundle();
                int nSubID[] = null;// {0x0001};
                // bundle.putString("cls", "checkauto.com.IdcardRunner");
                bundle.putInt("nTypeInitIDCard", 0); // 保留，传0即可
                bundle.putString("lpFileName", path);// 指定的图像路径
                bundle.putInt("nTypeLoadImageToMemory", 0);// 0不确定是哪种图像，1可见光图，2红外光图，4紫外光图

                bundle.putInt("nMainID", nMainID); // 证件的主类型。6是行驶证，2是二代证，这里只可以传一种证件主类型。每种证件都有一个唯一的ID号，可取值见证件主类型说明
                bundle.putIntArray("nSubID", nSubID); // 保存要识别的证件的子ID，每个证件下面包含的子类型见证件子类型说明。nSubID[0]=null，表示设置主类型为nMainID的所有证件。
                // bundle.putBoolean("GetSubID", true); //GetSubID得到识别图像的子类型id
                // bundle.putString("lpHeadFileName",
                // "/mnt/sdcard/head.jpg");//保存路径名，后缀只能为jpg、bmp、tif
                // bundle.putBoolean("GetVersionInfo", true); //获取开发包的版本信息
                // 读设置到文件里的sn
                File file = new File(PATH);
                String snString = null;
                if (file.exists()) {
                    String filePATH = PATH + "/idcard.sn";
                    File newFile = new File(filePATH);
                    if (newFile.exists()) {
                        BufferedReader bfReader = new BufferedReader(new FileReader(newFile));
                        snString = bfReader.readLine().toUpperCase();
                        bfReader.close();
                    } else {
                        bundle.putString("sn", "");
                    }
                    if (snString != null && !snString.equals("")) {
                        bundle.putString("sn", snString);
                    } else {
                        bundle.putString("sn", "");
                    }
                } else {
                    bundle.putString("sn", "");
                }

                bundle.putString("sn", ""); // 序列号激活方式,XS4XAYRWEFRY248YY4LHYY178已使用
                bundle.putString("authfile", ""); // 文件激活方式
                bundle.putString("logo", logopath); // logo路径，logo显示在识别等待页面右上角
                bundle.putBoolean("isCut", cut); // 如不设置此项默认自动裁切
                bundle.putString("returntype", "withvalue");// 返回值传递方式withvalue带参数的传值方式（新传值方式）
                intent.putExtras(bundle);
                startActivityForResult(intent, 10);
                // overridePendingTransition(R.anim.zoomin, R.anim.zoomout);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "没有发现处理 wintone.idcard 的命令。", 0).show();
            }

        } else {

        }
    }

    public void displayPopuWindow(String message) {
        int relalayoutwidth = (int) (width - ((height * 4) / 3));
        int scWidth = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
        int scHeight = this.getWindow().getWindowManager().getDefaultDisplay().getHeight();
        int popuWidth = scWidth - relalayoutwidth;
        EditText et = new EditText(this);
        et.setWidth(scWidth);
        et.setHeight(popuWidth);
        PopupWindow popuWindow = new PopupWindow(et, popuWidth, 200, true);
        popuWindow.setWidth(popuWidth);
        popuWindow.setHeight(200);
        popuWindow.setContentView(et);
        ColorDrawable color = new ColorDrawable(Color.BLACK);
        color.setAlpha(60);
        popuWindow.setBackgroundDrawable(color);
        popuWindow.setOutsideTouchable(true);
        popuWindow.setFocusable(true);
        popuWindow.showAtLocation(surfaceView, Gravity.LEFT, 0, scHeight - 200);
        et.setText(message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK) {
            int ReturnAuthority = data.getIntExtra("ReturnAuthority", -100000);// 取激活状态
            int ReturnInitIDCard = data.getIntExtra("ReturnInitIDCard", -100000);// 取初始化返回值
            int ReturnLoadImageToMemory = data.getIntExtra("ReturnLoadImageToMemory", -100000);// 取读图像的返回值
            int ReturnRecogIDCard = data.getIntExtra("ReturnRecogIDCard", -100000);// 取识别的返回值
            if (ReturnAuthority == 0 && ReturnInitIDCard == 0 && ReturnLoadImageToMemory == 0
                    && ReturnRecogIDCard > 0) {
                String result = "";
                String[] fieldname = (String[]) data.getSerializableExtra("GetFieldName");
                String[] fieldvalue = (String[]) data.getSerializableExtra("GetRecogResult");
                Toast.makeText(getApplicationContext(), "fieldvalue", 0).show();
                if (null != fieldname) {
                    int count = fieldname.length;
                    for (int i = 0; i < count; i++) {
                        if (fieldname[i] != null) {
                            result += fieldname[i] + ":" + fieldvalue[i] + ";\n";
                        }
                    }
                }
                displayPopuWindow("识别结果" + ReturnRecogIDCard + "\n" + result);

            } else {
                String str = "";
                if (ReturnAuthority == -100000) {
                    str = getString(R.string.exception) + ReturnAuthority;
                } else if (ReturnAuthority != 0) {
                    str = getString(R.string.exception1) + ReturnAuthority;
                } else if (ReturnInitIDCard != 0) {
                    str = getString(R.string.exception2) + ReturnInitIDCard;
                } else if (ReturnLoadImageToMemory != 0) {
                    if (ReturnLoadImageToMemory == 3) {
                        str = getString(R.string.exception3) + ReturnLoadImageToMemory;
                    } else if (ReturnLoadImageToMemory == 1) {
                        str = getString(R.string.exception4) + ReturnLoadImageToMemory;
                    } else {
                        str = getString(R.string.exception5) + ReturnLoadImageToMemory;
                    }
                } else if (ReturnRecogIDCard != 0) {
                    str = getString(R.string.exception6) + ReturnRecogIDCard;
                }
                displayPopuWindow("识别结果" + ReturnRecogIDCard + "\n" + str);
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myListener != null) {
            sManager.unregisterListener(myListener);
        }
        sManager = null;
        myListener = null;
        if (!isAutoTakePic)
            handler.removeCallbacks(runnable);// 停止计时器，每当拍照或退出时都要执行这段代码。
    }

    private float getStableFloat(float x, float y, float z) {
        float move_Difference = 0.0f;
        float[] floatdata = { x, y, z };
        if (mlist.size() < ListMaxLen) {
            mlist.add(floatdata);
        } else {
            mlist.remove(0);
            mlist.add(floatdata);
        }
        if (mlist.size() < ListMaxLen) {
            return 0.1f;
        }
        float sumx = 0;
        float sumy = 0;
        float sumz = 0;
        int len = mlist.size();
        for (int i = 0; i < len; i++) {
            float[] dd = (float[]) mlist.get(i);
            sumx += dd[0];
            sumy += dd[1];
            sumz += dd[2];
        }
        float avgx = sumx / len;
        float avgy = sumy / len;
        float avgz = sumz / len;
        for (int i = 0; i < len; i++) {
            float[] dd = (float[]) mlist.get(i);
            move_Difference = (dd[0] - avgx) * (dd[0] - avgx) + (dd[1] - avgy) * (dd[1] - avgy)
                    + (dd[2] - avgz) * (dd[2] - avgz);
        }
        return move_Difference;
    }

    public void autoFocus() {

        if (camera != null) {
            try {
                if (camera.getParameters().getSupportedFocusModes() != null && camera.getParameters()
                        .getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    camera.autoFocus(new AutoFocusCallback() {

                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {

                            } else {

                            }
                        }
                    });
                } else {

                    Toast.makeText(getBaseContext(), getString(R.string.unsupport_auto_focus), Toast.LENGTH_LONG)
                            .show();
                }

            } catch (Exception e) {
                e.printStackTrace();
                camera.stopPreview();
                camera.startPreview();
                takepicbtn.setEnabled(true);
                Toast.makeText(this, R.string.toast_autofocus_failure + "黄震", Toast.LENGTH_SHORT).show();

            }
        }

    }

    public void closeCamera() {
        synchronized (this) {
            try {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            } catch (Exception e) {
                Log.i("TAG", e.getMessage());
            }
        }
    }
}