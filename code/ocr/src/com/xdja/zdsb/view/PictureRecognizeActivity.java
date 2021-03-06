package com.xdja.zdsb.view;

import java.io.File;

import com.xdja.zdsb.R;
import com.xdja.zdsb.bean.CacheBean;
import com.xdja.zdsb.utils.PictureRecognize;
import com.xdja.zdsb.utils.RecognizerInterface;
import com.xdja.zdsb.utils.Zzlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;


public class PictureRecognizeActivity extends Activity {

    protected static final String TAG = "PictureRecognizeActivity";

    private ImageView pre_view;

    private ImageView imageView5;

    private String picturePath;

    private int picType;

    private Animation animation;

    PictureRecognize pictureRecognize;
    
    boolean isShowDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pictrue_recognize_activity);

        Intent intent = getIntent();

        picturePath = intent.getStringExtra("path");
        isShowDialog = intent.getBooleanExtra("isShowDialog", false);

        pre_view = (ImageView)findViewById(R.id.pre_view);
        imageView5 = (ImageView)findViewById(R.id.imageView5);

        setAnimation();

        picType = intent.getIntExtra("picture_type", 0);

        Zzlog.out(TAG, "picturePath = " + picturePath + ", picType = " + picType);
        
        if (picType == 0) {
            Toast.makeText(this, "未知的识别类型，请重新选择。", Toast.LENGTH_LONG).show();
            this.finish();
        }

        if (picturePath != null) {

            Uri uri = Uri.fromFile(new File(picturePath));
            pre_view.setImageURI(uri);
        }

        PictureRecognizeTask pictureRecognizeTask = new PictureRecognizeTask();
        pictureRecognizeTask.execute();
    }

    class PictureRecognizeTask extends AsyncTask<String, Object, String> {

        @Override
        protected String doInBackground(String... params) {
            pictureRecognize = new PictureRecognize(PictureRecognizeActivity.this);
            pictureRecognize.doRecognize(picturePath, picType, recognizerInterface);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        if (pictureRecognize != null) {
            pictureRecognize.clean();
            pictureRecognize = null;
        }
        super.onDestroy();
    }

    private void setAnimation() {
        animation = AnimationUtils.loadAnimation(this, R.anim.translate);
        imageView5.setAnimation(animation);
        animation.start();
    }

    RecognizerInterface recognizerInterface = new RecognizerInterface() {

        @Override
        public void onRecognizeSucceed(final String result, final String keyNumber) {

            String showMessage = result + "\n号码： " +  keyNumber;
            Zzlog.out(TAG, "showMessage = " + showMessage);
            if (isShowDialog) {
                AlertDialog dialog = new AlertDialog.Builder(PictureRecognizeActivity.this).setTitle(
                        getString(R.string.recognize_result)).setMessage(showMessage)
                        .setPositiveButton(getString(R.string.confirm_btn_string), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent();
                                intent.putExtra("type", picType);
                                intent.putExtra("data", result);
                                intent.putExtra("keyNumber", keyNumber);
                                setResult(RESULT_OK, intent);
                                PictureRecognizeActivity.this.finish();
                            }
                        })

                        .setNegativeButton(getString(R.string.butclose), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // reset parameter.
                                Intent intent = new Intent();
                                setResult(RESULT_CANCELED, intent);
                                PictureRecognizeActivity.this.finish();
                            }
                        }).create();

                dialog.show();
            } else {
                Intent intent = new Intent();
                intent.putExtra("type", picType);
                intent.putExtra("data", result);
                intent.putExtra("keyNumber", keyNumber);
                if (!TextUtils.isEmpty(CacheBean.jsonbean)) {
                    intent.putExtra("json", CacheBean.jsonbean);
                }
                setResult(RESULT_OK, intent);
                PictureRecognizeActivity.this.finish();
            }

        }

        @Override
        public void onRecognizeFailed(int errorCode) {
            Intent intent = new Intent();
            intent.putExtra("type", 0);
            intent.putExtra("errorCode", errorCode);
            intent.putExtra("data", "");
            intent.putExtra("keyNumber", "");
            setResult(RESULT_CANCELED, intent);
            String showMessage = "识别失败！";

            if (isShowDialog) {
                AlertDialog dialog = new AlertDialog.Builder(PictureRecognizeActivity.this).setTitle(
                        getString(R.string.recognize_result)).setMessage(showMessage)
                        .setPositiveButton(getString(R.string.confirm_btn_string),
                           new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PictureRecognizeActivity.this.finish();
                                }
                        }).setNegativeButton(getString(R.string.butclose),
                           new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PictureRecognizeActivity.this.finish();
                                }
                        }).create();

                dialog.show();
            } else {
                finish();
            }
            

        }
    };
}
