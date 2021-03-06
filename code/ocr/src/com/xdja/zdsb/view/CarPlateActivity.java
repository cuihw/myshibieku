package com.xdja.zdsb.view;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.wintone.plateid.PlateCfgParameter;
import com.wintone.plateid.PlateRecognitionParameter;
import com.wintone.plateid.RecogService;
import com.xdja.zdsb.R;
import com.xdja.zdsb.auth.RecognizerAuth;
import com.xdja.zdsb.bean.CacheBean;
import com.xdja.zdsb.bean.CarBean;
import com.xdja.zdsb.utils.Constant;
import com.xdja.zdsb.utils.FileUtils;
import com.xdja.zdsb.utils.Utils;
import com.xdja.zdsb.utils.Zzlog;
import com.xdja.zdsb.view.CarResultDialog.IClickListener;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CarPlateActivity extends Activity
        implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CarPlateActivity";

    private Camera camera;

    public RecogService.MyBinder recogBinder;

    private int iInitPlateIDSDK = -1;

    private DisplayMetrics metric;

    private int width, height;

    private SurfaceView surfaceView;

    private SurfaceHolder holder;

    private static final int ROTATION = 90;

    int nums = -1;

    private byte[] tempData;

    private PlateRecognitionParameter prp;

    private boolean setRecogArgs = true;

    private TimerTask timer;

    private FoucsView1 foucsView;

    private ImageButton light_on;

    private ImageButton backbtn;

    private boolean savePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);//
        setContentView(R.layout.car_plate_id_layout);
        Zzlog.out(TAG, "onCreate()");

        initView();
        Intent intent = getIntent();
        if (!RecognizerAuth.check(intent)) {
            Intent retIntent = new Intent();
            retIntent.putExtra("number", "");
            retIntent.putExtra("data", "authentication failed.");
            setResult(RESULT_OK, retIntent);
            finish();
        }
        
    }

    private void initView() {

        metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        width = metric.widthPixels;
        height = metric.heightPixels;

        foucsView = new FoucsView1(this, width/2, height/8);
        foucsView.setTransparency(true);
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.relative_layout);
        relativeLayout.addView(foucsView);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceViwe_video);

        light_on = (ImageButton)findViewById(R.id.light_on);

        backbtn = (ImageButton)findViewById(R.id.backbtn);
        
        light_on.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View v) {
		Camera.Parameters parameters = camera.getParameters();
		List<String> flashList = parameters.getSupportedFlashModes();
		if (flashList != null && flashList.contains(Camera.Parameters.FLASH_MODE_TORCH)) {

		    String mode = parameters.getFlashMode();
		    Zzlog.out(TAG, "mode = " + mode);
		    if (mode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
			light_on.setBackgroundResource(R.drawable.flash_on);
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			camera.setParameters(parameters);
		    } else {
			light_on.setBackgroundResource(R.drawable.flash_off);
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			camera.setParameters(parameters);
		    }
		} else {
		    Toast.makeText(getApplicationContext(), getString(R.string.unsupportflash), Toast.LENGTH_SHORT)
			    .show();
		}
	    }
        });

        backbtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                releaseCamera();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Zzlog.out(TAG, "onResume()");
        holder = surfaceView.getHolder();
        holder.addCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Zzlog.out(TAG, "onPause()... ");
        releaseCamera();
    }

    @Override
    protected void onStop() {
        Zzlog.out(TAG, "onPause()... ");
        super.onStop();
    }

    public ServiceConnection recogConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            recogBinder = (RecogService.MyBinder) service;

            iInitPlateIDSDK = recogBinder.getInitPlateIDSDK();

            Zzlog.out(TAG, "onServiceConnected .. iInitPlateIDSDK = " + iInitPlateIDSDK);

            if (iInitPlateIDSDK != 0) {
                String[] str = {"" + iInitPlateIDSDK};
                getResult(str);
            }

            PlateCfgParameter cfgparameter = new PlateCfgParameter();
            cfgparameter.armpolice = 4;
            cfgparameter.armpolice2 = 16;
            cfgparameter.embassy = 12;
            cfgparameter.individual = 0;
            cfgparameter.nOCR_Th = 0;
            cfgparameter.nPlateLocate_Th = 5;
            cfgparameter.onlylocation = 15;
            cfgparameter.tworowyellow = 2;
            cfgparameter.tworowarmy = 6;
            cfgparameter.szProvince = "";
            cfgparameter.onlytworowyellow = 11;
            cfgparameter.tractor = 8;
            cfgparameter.bIsNight = 1;

            int imageformat = 6;// NV21 -->6
            int bVertFlip = 0;
            int bDwordAligned = 1;
            recogBinder.setRecogArgu(cfgparameter, imageformat, bVertFlip, bDwordAligned);

            Zzlog.out(TAG, "onServiceConnected()! !");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Zzlog.out(TAG, "onServiceDisconnected()! !");
            recogBinder = null;
        }
    };

    private int preWidth, preHeight;

    // SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera(holder, ROTATION);
    }

    Camera.PictureCallback pcb = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            recogProcess(data);
            initCamera(holder, ROTATION);
        }
    };

    @SuppressWarnings("deprecation")
    private void initCamera(SurfaceHolder holder, int r) {
        CacheBean.jsonbean = null;
        setRecogArgs = true;
        Zzlog.out(TAG, "surfaceCreated()");
        if (camera == null) {
            try {
                camera = Camera.open();
            } catch (Exception e) {
                e.printStackTrace();Zzlog.eOut(e);
                return;
            }
        }

        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        int length = list.size();
        int previewWidth = 640;
        int previewheight = 480;
        int second_previewWidth = 0;
        int second_previewheight = 0;
        Camera.Size size;
        if (length == 1) {
            size = list.get(0);
            previewWidth = size.width;
            previewheight = size.height;
            Zzlog.out(TAG, "previewWidth = " + previewWidth + ", previewheight = " + previewheight);
        } else {
            for (int i = 0; i < length; i++) {
                size = list.get(i);
                Zzlog.out(TAG, "preview size = " + size.width + ", " + size.height );
                if (size.height <= 480 || size.width <= 640) {
                    second_previewWidth = size.width;
                    second_previewheight = size.height;
                    if (previewWidth <= second_previewWidth) {
                        previewWidth = second_previewWidth;
                        previewheight = second_previewheight;
                    }
                }
            }
        }

        preWidth = previewWidth;
        preHeight = previewheight;
        Zzlog.out(TAG, "Preview Resolution: preWidth = " + preWidth + ", preHeight = " + preHeight);
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setPreviewSize(preWidth, preHeight);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        camera.setPreviewCallback(this);
        camera.setParameters(parameters);
        camera.setDisplayOrientation(r);

        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Zzlog.eOut(e);
            e.printStackTrace();Zzlog.eOut(e);
        }

        camera.startPreview();

        Timer time = new Timer();
        if (timer == null) {
            timer = new TimerTask() {
                public void run() {

                    if (camera != null) {
                        try {
                            camera.autoFocus(new AutoFocusCallback() {
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (success) {
                                        Zzlog.out(TAG, "onAutoFocus ().");
                                        startRecognizing();
                                    }
                                }

                            });
                        } catch (Exception e) {
                            e.printStackTrace();Zzlog.eOut(e);
                        }
                    }
                };
            };
            time.schedule(timer, 2000, 2500);
        }
    }

    private boolean isFocus = false;
    private void startRecognizing() {
        isFocus = true;
        Zzlog.out(TAG, "startRecognizing().. isFocus = true");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    isFocus = false;
                    Zzlog.out(TAG, "startRecognizing().. isFocus = false");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }}).start();
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                Zzlog.out(TAG, "release camera");
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();Zzlog.eOut(e);
        }

        unbindRegservice();
    }

    private void unbindRegservice() {
        if (recogBinder != null) {
            try {
                unbindService(recogConn);
            } catch (Exception e) {
                e.printStackTrace();Zzlog.eOut(e);
            }
            iInitPlateIDSDK = -1;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (camera != null) {
            final SurfaceHolder sh = holder;
            camera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        synchronized (camera) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    initCamera(sh, ROTATION);
                                }
                            }).start();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Zzlog.out(TAG, "surfaceDestroyed()... ");
    }

    @Override // PreviewCallback
    public void onPreviewFrame(byte[] data, Camera camera) {
        Zzlog.out(TAG, "onPreviewFrame()");
        if (isFocus) {
            recogProcess(data);
        }
    }

    private void recogProcess(byte[] data) {
        Zzlog.out(TAG, "recogProcess++++ nums = " + nums + ", iInitPlateIDSDK = " + iInitPlateIDSDK);
        nums++;
        if (setRecogArgs) {
            Intent intent = new Intent(this, RecogService.class);
            bindService(intent, recogConn, Service.BIND_AUTO_CREATE);
            setRecogArgs = false;
        }

        if (iInitPlateIDSDK == 0) {
            tempData = data;

            // FileUtils.writeJpgFile(data);

            prp = new PlateRecognitionParameter();
            prp.height = preHeight;//
            prp.width = preWidth;//
            prp.picByte = data;
            prp.devCode = Constant.DEVCODE;
            prp.plateIDCfg.bRotate = 1;

            Zzlog.out(TAG, "preHeight = " + preHeight + ", preWidth = " + preWidth);

            Log.i(TAG, "bRotate = " + prp.plateIDCfg.bRotate + ","
                    + " left = " + prp.plateIDCfg.left + ", right = " 
                    + prp.plateIDCfg.right + ", top = " + prp.plateIDCfg.top
                    + ", bottom = " + prp.plateIDCfg.bottom);

            String[] fieldvalue = null;
            int nRet = -1;
            try {
                fieldvalue = recogBinder.doRecogDetail(prp);
                nRet = recogBinder.getnRet();
            } catch (Exception e) {
                e.printStackTrace();Zzlog.eOut(e);
            }

            if (nRet != 0) {
                String[] str = {"" + nRet};
                getResult(str);
            } else {
                if (fieldvalue != null) getResult(fieldvalue);
            }
        }

        Zzlog.out(TAG, "recogProcess-----");
    }

    /**
     * @Title: getResult 
     * @param @param fieldvalue
     * @return void
     */
    private void getResult(String[] fieldvalue) {

        Zzlog.out(TAG, "getResult: ......................");

        if (fieldvalue != null && fieldvalue.length > 0) {
            for (int i = 0; i < fieldvalue.length; i++) {
                Zzlog.out(TAG, "fieldvalue[" + i + "] = " + fieldvalue[i]);
            }
        } else {
            Zzlog.out(TAG, "fieldvalue id empty!");
        }

        if (iInitPlateIDSDK != 0) {
            errorNotify();
        } else {
            String[] resultString;
            String boolString = "";
            boolString = fieldvalue[0];

            if (!TextUtils.isEmpty(boolString)) {
                camera.stopPreview();

                resultString = boolString.split(";");
                int length = resultString.length;
                String firstNumber = null;
                if (length > 0) {
                    firstNumber = resultString[0];
                    for (int i = 0; i < length; i++) {
                        Zzlog.out(TAG, "resultString[" + i + "] = " + resultString[i]);
                    }
                }
                if (!TextUtils.isEmpty(firstNumber)) {
                    String color = null;
                    if (fieldvalue.length>2) {
                        color = fieldvalue[1];                        
                    }

                    
                    CarResultDialog dialog =
                            new CarResultDialog(this, clickListener,
                                    getString(R.string.plateid_result),
                                    color, firstNumber);
                    
                    Gson gson = new Gson();
                    CarBean car = new CarBean();
                    car.setColor(color);
                    car.setNumber(firstNumber);
                    CacheBean.jsonbean = gson.toJson(car);

                    dialog.show();
                    Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
                    vibrator.vibrate(300);
                    releaseCamera();
                }
            }
        }
        fieldvalue = null;
    }

    IClickListener clickListener = new IClickListener() {

        @Override
        public void doConfirm(String color, String number) {
            Zzlog.out(TAG, "color = " + color + ", number = " + number);
            String result = "color:" + color + ";number:" + number;
            // save picture.
            if (savePic) {
                savePicure();
            }
            Intent intent = new Intent();
            intent.putExtra("number", number);
            intent.putExtra("data", result);
            setResult(RESULT_OK, intent);
            CarPlateActivity.this.finish();
        }

        @Override
        public void doRetry() {
            initCamera(holder, ROTATION);
        }
    };

    private void errorNotify() {

        String nretString = iInitPlateIDSDK + "";
        if (nretString.equals("-1001")) {
            Toast.makeText(this, getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                    + getString(R.string.failed_readJPG_error), Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10001")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_noInit_function),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10003")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_validation_faile),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10004")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_serial_number_null),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10005")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_disconnected_server),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10006")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_obtain_activation_code),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10007")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_noexist_serial_number),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10008")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_serial_number_used),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10009")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_unable_create_authfile),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10010")) {
            Toast.makeText(this,
                    getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                            + getString(R.string.failed_check_activation_code),
                    Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10011")) {
            Toast.makeText(this, getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                    + getString(R.string.failed_other_errors), Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10012")) {
            Toast.makeText(this, getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                    + getString(R.string.failed_not_active), Toast.LENGTH_SHORT).show();

        } else if (nretString.equals("-10015")) {
            Toast.makeText(this, getString(R.string.recognize_result) + iInitPlateIDSDK + "\n"
                    + getString(R.string.failed_check_failure), Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, getString(R.string.recognize_result) + iInitPlateIDSDK + "\n",
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void savePicure() {

        if (tempData != null) {

            int[] datas = Utils.convertYUV420_NV21toARGB8888(tempData, preWidth, preHeight);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inInputShareable = true;
            opts.inPurgeable = true;
            Bitmap bitmap1 = Bitmap.createBitmap(datas, preWidth, preHeight,
                    android.graphics.Bitmap.Config.ARGB_8888);
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.setRotate(90);
            bitmap1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.getWidth(), bitmap1.getHeight(),
                    matrix, true);

            String filename = FileUtils.getStringFileName(FileUtils.MEDIA_TYPE_IMAGE);
            FileUtils.savePicture(filename, bitmap1);
        }
    }

}
