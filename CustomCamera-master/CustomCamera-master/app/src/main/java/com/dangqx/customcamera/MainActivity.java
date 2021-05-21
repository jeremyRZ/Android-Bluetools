package com.dangqx.customcamera;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dangqx.customcamera.method.SetPreviewAndCapture;
import com.dangqx.customcamera.method.VideoRecordUtils;
import com.dangqx.customcamera.util.ImageSaver;
import com.dangqx.customcamera.util.Utils;
import com.dangqx.customcamera.view.ResizeAbleSurfaceView;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private SetPreviewAndCapture setPreviewAndCapture;
    private VideoRecordUtils videoRecordUtils;

    private int currentCameraId = CameraCharacteristics.LENS_FACING_FRONT;//手机后面的摄像头

    private ResizeAbleSurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Size previewSize;//图片尺寸
    private Size mWinSize;//获取屏幕的尺寸
    private ImageReader imageReader;//接受图片数据

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;

    private HandlerThread handlerThread;
    private Handler handler;

    private Spinner spinnerSite;
    private Spinner spinnerLoc;
    private Spinner spinnerDetLoc;
    private Spinner spinnerNoCoup;
    private Spinner spinnerReb;
    private Spinner spinnerCoupNum;
    private Spinner spinnerSelecVideo;

    private String[] listSite;
    private String[] listLoc;
    private String[] listDetLoc;
    private String[] listNoCoup;
    private String[] listReb;
    private String[] listCoupNum;
    private String[] listSelecVideo;
    private ArrayList<String> list2;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWinSize = Utils.loadWinSize(this);
        //动态获取权限
        List<String> permissionList = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            initView();
        }

        //为按钮绑定点击事件
        //正确顺序 Start Pause Stop ZoomIn ZoomOut
        ImageButton picture = (ImageButton) findViewById(R.id.imageButtonZoomout);
        ImageButton change = (ImageButton) findViewById(R.id.imageButtonPause);
        ImageButton record = (ImageButton) findViewById(R.id.imageButtonStart);
        ImageButton stopRecord = (ImageButton) findViewById(R.id.imageButtonStop);
        ImageButton zoom = (ImageButton) findViewById(R.id.imageButtonZoomin);

        picture.setOnClickListener(this);
        change.setOnClickListener(this);
        record.setOnClickListener(this);
        stopRecord.setOnClickListener(this);
        zoom.setOnClickListener(this);

//        initView();
        spinnerSite = (Spinner) findViewById(R.id.spinnerSite);
        spinnerLoc = (Spinner) findViewById(R.id.spinnerLoc);
        spinnerDetLoc = (Spinner) findViewById(R.id.spinnerDetLoc);
        spinnerNoCoup = (Spinner) findViewById(R.id.spinnerNoCoup);
        spinnerReb = (Spinner) findViewById(R.id.spinnerReb);
        spinnerCoupNum = (Spinner) findViewById(R.id.spinnerCoupNum);
        spinnerSelecVideo = (Spinner) findViewById(R.id.spinnerSelectVideo);
//        initData();
        listSite = new String[]{"Zone C 14105D TAWMC","Zone B 14105D TAWMC","Zone A 14105D TAWMC"};
        listLoc = new String[]{"H. 支柱(Stanchion) B3 - Portion A1","H. 支柱(Stanchion) B3 - Portion A2","H. 支柱(Stanchion) B3 - Portion A3"};
        listDetLoc = new String[]{"NC101","NC102","NC103","NC104","NC105","NC106"};
        listNoCoup = new String[]{"T1","T2","T5","B2","B5"};
        listReb = new String[]{"Y40","Y60","Y70"};
        listCoupNum = new String[]{"32","76","51"};
        listSelecVideo = new String[]{"Video1","Video2","Video3"};

        ArrayAdapter<String> adapterSite=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listSite);
        ArrayAdapter<String> adapterLoc=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listLoc);
        ArrayAdapter<String> adapterDetLoc=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listDetLoc);
        ArrayAdapter<String> adapterNoCoup=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listNoCoup);
        ArrayAdapter<String> adapterReb=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listReb);
        ArrayAdapter<String> adapterCoupNum=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listCoupNum);
        ArrayAdapter<String> adapterSelectVideo=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, listSelecVideo);

        adapterSite.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterLoc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterDetLoc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterNoCoup.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterReb.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterCoupNum.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterSelectVideo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSite.setAdapter(adapterSite);
        spinnerLoc.setAdapter(adapterLoc);
        spinnerDetLoc.setAdapter(adapterDetLoc);
        spinnerNoCoup.setAdapter(adapterNoCoup);
        spinnerReb.setAdapter(adapterReb);
        spinnerCoupNum.setAdapter(adapterCoupNum);
        spinnerSelecVideo.setAdapter(adapterSelectVideo);
//        list2 = new ArrayList<>();
//        list2.add("Android");
//        list2.add("IOS");
//        list2.add("H5");
//        spinner3.setAdapter(new MyAdapter());
//        initListener();
        spinnerSite.setOnItemSelectedListener(this);
        spinnerLoc.setOnItemSelectedListener(this);
        spinnerDetLoc.setOnItemSelectedListener(this);
        spinnerNoCoup.setOnItemSelectedListener(this);
        spinnerReb.setOnItemSelectedListener(this);
        spinnerCoupNum.setOnItemSelectedListener(this);
        spinnerSelecVideo.setOnItemSelectedListener(this);
    }

    /**
     * 请求权限回调
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "拒绝权限无法使用", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    initView();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    /**
     * 加载布局，初始化组件
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initView(){
        surfaceView = findViewById(R.id.surfaceView);
        //surfaceView.resize(1080,1080);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                //打开相机同时开启预览
                setAndOpenCamera();
                //解决预览拉升
                int height = surfaceView.getHeight();
                int width = surfaceView.getWidth();
                if (height > width) {
                    //正常情况，竖屏
                    float justH = width * 4.f / 3;
                    //设置View在水平方向的缩放比例,保证宽高比为3:4
                    surfaceView.setScaleX(height / justH);
                } else {
                    float justW = height * 4.f / 3;
                    surfaceView.setScaleY(width / justW);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                //关闭相机释放资源
                closeCamera();
            }
        });

        //获取相机管理
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        //开启子线程，处理某些耗时操作
        handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }
    private void setAndOpenCamera(){
        //获取摄像头属性描述
        CameraCharacteristics cameraCharacteristics = null;
        try{
            //根据摄像头id获取摄像头属性类
            cameraCharacteristics = cameraManager.getCameraCharacteristics(String.valueOf(currentCameraId));
            //获取支持的缩放
            float maxZoom = (cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*10;
            Log.d("最大缩放倍数", "switchCamera: "+maxZoom);
            //获取该摄像头支持输出的图片尺寸
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //根据屏幕尺寸即摄像头输出尺寸计算图片尺寸，或者直接选取最大的图片尺寸进行输出
            previewSize = Utils.fitPhotoSize(map,mWinSize);
            //初始化imageReader
            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG,2);
            //设置回调处理接受图片数据
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //发送数据进子线程处理
                    handler.post(new ImageSaver(reader.acquireNextImage(),MainActivity.this));
                }
            },handler);
            //打开相机，先检查权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED){
                return;
            }
            //打开摄像头
            cameraManager.openCamera(String.valueOf(currentCameraId), stateCallback,null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 打开相机后的状态回调，获取CameraDevice对象
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            //打开相机后开启预览，以及拍照的工具类,主要是将CameraDevice对象传递进工具类
            setPreviewAndCapture = new SetPreviewAndCapture(cameraDevice, surfaceHolder,
                    imageReader, handler,MainActivity.this, previewSize);
            setPreviewAndCapture.startPreview();
            //初始化录像的工具类
            videoRecordUtils = new VideoRecordUtils();
            videoRecordUtils.create(surfaceView, cameraDevice, VideoRecordUtils.WH_720X480);

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            finish();
        }
    };

    /**
     * 切换前后摄像头的方法
     */
    private void switchCamera(){
        try{
            for (String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                //判断当前摄像头为后置摄像头，且存在前置摄像头
                if (currentCameraId == CameraCharacteristics.LENS_FACING_FRONT &&
                        cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK){
                    //后置转前置
                    currentCameraId = CameraCharacteristics.LENS_FACING_BACK;
                    //重新打开相机
                    cameraDevice.close();
                    setAndOpenCamera();
                    break;
                }else if (currentCameraId == CameraCharacteristics.LENS_FACING_BACK &&
                        cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    //前置转后置
                    currentCameraId = CameraCharacteristics.LENS_FACING_FRONT;
                    cameraDevice.close();
                    setAndOpenCamera();
                    break;
                }
            }
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {

        //关闭相机
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        //关闭拍照处理器
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    /**
     * 点击不同按钮的事件
     * @param v
     */
    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageButtonStop:
                setPreviewAndCapture.takePhoto();
                break;
            case R.id.imageButtonZoomin:
                switchCamera();
                break;
            case R.id.imageButtonStart:
                videoRecordUtils.startRecord(MainActivity.this,handler);
                break;
            case R.id.imageButtonPause:
                videoRecordUtils.stopRecord();
                setPreviewAndCapture.startPreview();
            case R.id.imageButtonZoomout:
                setPreviewAndCapture.setZoom();
                break;
            default:
                break;
        }
    }

    /**
     * Spinner Data （HardCode）
     */


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()){
//            case R.id.spinner1:
//                String[] letter = getResources().getStringArray(R.array.letter);
//                Log.i("spinner1点击------",letter[position]);
//                break;
            case R.id.spinnerSite:
                Log.i("spinnerSite------",listSite[position]);
                break;
            case R.id.spinnerLoc:
                Log.i("spinnerLoc------",listLoc[position]);
                break;
            case R.id.spinnerDetLoc:
                Log.i("spinnerDetLoc------",listDetLoc[position]);
                break;
            case R.id.spinnerNoCoup:
                Log.i("spinnerNoCoup------",listNoCoup[position]);
                break;
            case R.id.spinnerReb:
                Log.i("spinnerReb------",listReb[position]);
                break;
            case R.id.spinnerCoupNum:
                Log.i("spinnerCoupNum------",listCoupNum[position]);
                break;
            case R.id.spinnerSelectVideo:
                Log.i("spinnerCoupNum------",listSelecVideo[position]);
                break;
//            case R.id.spinner3:
//                Log.i("spinner3点击------",list2.get(position));
//                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * 自定义的Adapter
     */
    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list2.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder ;
            if(convertView==null){
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_text, viewGroup, false);
                holder = new ViewHolder();
                holder.itemText= (TextView) convertView.findViewById(R.id.item_text);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }
            holder.itemText.setText(list2.get(position));
            return convertView;
        }
    }
    class ViewHolder {
        TextView itemText;
    }
}