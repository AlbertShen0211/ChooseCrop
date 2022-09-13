package com.choosecrop;

import static android.os.Environment.DIRECTORY_DCIM;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView mHeader_iv;

    //相册请求码
    private static final int DICM_REQUEST_CODE = 1;
    //相机请求码
    private static final int CAMERA_REQUEST_CODE = 2;
    //剪裁请求码
    private static final int CROP_REQUEST_CODE = 3;

    //调用照相机返回图片文件
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        mHeader_iv = (ImageView) findViewById(R.id.mHeader_iv);
        Button mGoCamera_btn = (Button) findViewById(R.id.mGoCamera_btn);
        Button mGoAlbm_btn = (Button) findViewById(R.id.mGoAlbm_btn);
        mGoCamera_btn.setOnClickListener(this);
        mGoAlbm_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mGoCamera_btn:
                getPicFromCamera();
                break;
            case R.id.mGoAlbm_btn:
                getPicFromAlbm();
                break;
            default:
                break;
        }
    }


    /**
     * 从相机获取图片
     */
    private void getPicFromCamera() {
        //用于保存调用相机拍照后所生成的文件
        tempFile = new File(this.getExternalFilesDir(DIRECTORY_DCIM), System.currentTimeMillis() + ".png");
        //跳转到调用系统相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //判断版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //如果在Android7.0以上,使用FileProvider获取Uri
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "com.choosecrop.fileprovider", tempFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);

        } else {    //否则使用Uri.fromFile(file)方法获取Uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
        }
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    /**
     * 从相册获取图片
     */
    private void getPicFromAlbm() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
//        photoPickerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        photoPickerIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        photoPickerIntent.setType("image/*");
        //   photoPickerIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(photoPickerIntent, DICM_REQUEST_CODE);
    }


    /**
     * 裁剪图片
     */
    private void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        // Intent intent = new Intent("android.intent.action.EDIT");
        // intent.setAction("android.intent.action.EDIT");
        //  intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(uri, "image/*");

        //  intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);

        intent.putExtra("outputX", 100);
        intent.putExtra("outputY", 100);
        intent.putExtra("return-data", false);
        File cropTemp = this.getExternalFilesDir(DIRECTORY_DCIM);
        File cropTempName = new File(cropTemp, System.currentTimeMillis() + "_crop_temp.png");
        Log.e("getPath", cropTempName.getAbsolutePath());

        Uri uriForFile = FileProvider.getUriForFile(this, "com.choosecrop.fileprovider", cropTempName);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);

        grantPermissionFix(intent, uriForFile);

        startActivityForResult(intent, CROP_REQUEST_CODE);

    }

    private void grantPermissionFix(Intent intent, Uri uri) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setAction(null);
            intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.e("d", "--------------222222222-------requestCode--: " + requestCode);
        switch (requestCode) {

            case CAMERA_REQUEST_CODE:   //调用相机后返回
                if (resultCode == RESULT_OK) {
                    //用相机返回的照片去调用剪裁也需要对Uri进行处理
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "com.choosecrop.fileprovider", tempFile);
                        cropPhoto(contentUri);
                    } else {
                        cropPhoto(Uri.fromFile(tempFile));
                    }
                }
                break;
            case DICM_REQUEST_CODE:    //调用相册后返回
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    //mHeader_iv.setImageURI(uri);
                    if (null != uri)
                        cropPhoto(uri);
                    else {
                        Log.e("e", "null");
                    }
                }
                break;
            case CROP_REQUEST_CODE:     //调用剪裁后返回
                Log.e("d", "--------------222222222-------");
                if (null != intent) {
                    Log.e("d", "---------------------not null");
                    Uri data = intent.getData();
                    mHeader_iv.setImageURI(data);
//                    Bundle bundle = intent.getExtras();
//                    if (bundle != null) {
//                        //在这里获得了剪裁后的Bitmap对象，可以用于上传
//                        Bitmap image = bundle.getParcelable("data");
//                        //设置到ImageView上
//                        mHeader_iv.setImageBitmap(image);
//                        //也可以进行一些保存、压缩等操作后上传
////                    String path = saveImage("crop", image);
//                    }
                } else {
                    Log.e("d", "---------null");
                }
                break;
        }
    }

    public String save(String name, Bitmap bmp) {
        File appDir = new File(Environment.getExternalStorageDirectory().getPath());
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = name + ".png";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
