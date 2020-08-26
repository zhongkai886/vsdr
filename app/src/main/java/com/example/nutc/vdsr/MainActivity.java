package com.example.nutc.vdsr;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TFlite mTflite;
    private Bitmap inputBitmap;
    private Bitmap burrybitmap;

    private int[] inputBitmapPixel;

    float[][][] originInputBitmapArray;
    float[][][] inputColorArray;

    private static int nHeight = 0;
    private static int nWidth = 0;

    private ArrayList<SubImageItem> inputSubImageArrayList;
    private ArrayList<SubImageItem> inputOriginImageArrayList;
    private ImageView imageView;
    private ImageView burryimageView;


    private float[][][] outputArray;
    private Button button;
    private Button mCameraButton;
    private Button mVdsr;
    private CheckBox mCheckBox;
    private CheckBox mCheckBoxGPU;
    //   36,144,36,48,49
//    final String[] set5 = {"butterfly_x3_GT.bmp", "baby_GT.bmp", "head_GT.bmp", "woman_GT.bmp", "bird_x3_GT.bmp"};
    final String[] set5 = {"butterfly_GT_mod_ycbcr.png", "baby_GT_mod_ycbcr.png", "head_GT_mod_ycbcr.png", "woman_GT_mod_ycbcr.png"};
    final String[] set5_blurry = {"butterfly_GT_mod.png", "baby_GT_mod.png", "head_GT_mod.png", "woman_GT_mod.png"};

    public static int IMAGE_SIZE = 40;

    private File photoFile ;
    private String imageFilePath;
    private static final int REQUEST_CAPTURE_IMAGE = 100;
    private String BitmapUri = "";
    Uri outputFileUri;
    public Bitmap bmp;

    private float[][] transformMatrixRgb =
            {{0.299f, -0.1687f, 0.5f},
                    {0.587f, -0.3313f, -0.4187f},
                    {0.114f, 0.5f, -0.0813f}};
    float[][][] YcbcrArray;
    private Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }
    public void convertToColorImage(float[][][] arr) {

        Log.d("yyyy", "convertToColorImage: "+nHeight+"////"+nWidth);
        Bitmap bm = Bitmap.createBitmap(1080 , 2220 , Bitmap.Config.ARGB_8888);
        for (int row = 0; row < 2220; row++) {
            for (int col = 0; col < 1080; col++) {
                bm.setPixel(col, row, Color.argb(255, (int) (arr[row][col][0]), (int) (arr[row][col][1]), (int) (arr[row][col][2])));
            }
        }
        onBackImg.finish(bm);

        this.bitmap = bm;
    }
    public float[][][] rgb2Ycbcr(float[][][] outputArray){
//        RGBArray = new float[240][240][3];
        YcbcrArray = new float[2220][1080][3];
//        RGB  轉 ycbcr (矩陣乘法)
        for (int h = 0; h < outputArray.length; h++) {
            Log.d("ttttt",""+outputArray[1].length);
            for (int w = 0; w < outputArray[0].length; w++) {
                float R = outputArray[h][w][0];
                float G = outputArray[h][w][1];
                float B = outputArray[h][w][2];

                YcbcrArray[h][w][0] = R * transformMatrixRgb[0][0] + G * transformMatrixRgb[1][0] + B * transformMatrixRgb[2][0];
                YcbcrArray[h][w][1] = R * transformMatrixRgb[0][1] + G * transformMatrixRgb[1][1] + B * transformMatrixRgb[2][1] +128;
                YcbcrArray[h][w][2] = R * transformMatrixRgb[0][2] + G * transformMatrixRgb[1][2] + B * transformMatrixRgb[2][2] +128;
            }
        }
        convertToColorImage(YcbcrArray);
        return YcbcrArray ;

    }
    private void init() {
        inputSubImageArrayList = new ArrayList<>();
        inputOriginImageArrayList = new ArrayList<>();
        setView();
        button = findViewById(R.id.button);
        mVdsr = findViewById(R.id.vdsr);
        mCameraButton = findViewById(R.id.camera);
        mVdsr.setOnClickListener(this);
        mCameraButton.setOnClickListener(this);
        button.setOnClickListener(this);
        imageView = findViewById(R.id.imagev);
//        burryimageView = findViewById(R.id.burry);


//        mTflite.setonBackImg(onBackImg);

//      下拉選單 圖片
        Spinner spinner = findViewById(R.id.spn);
        ArrayAdapter<String> lunchList = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                set5_blurry);
        spinner.setAdapter(lunchList);

//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                inputBitmap = getBitmapFromAsset(MainActivity.this, set5[position]);
////                inputBitmap = getBitmapFromAsset(MainActivity.this,BitmapUri);
//                burrybitmap = getBitmapFromAsset(MainActivity.this, set5_blurry[position]);
//                burryimageView.setImageBitmap(burrybitmap);
//
//
//                Toast.makeText(MainActivity.this, "您選擇了:" + set5[position], Toast.LENGTH_SHORT).show();
//
////               取得輸入圖片大小 並刪除多餘像素
//                inputBitmapPixel = new int[inputBitmap.getWidth() * inputBitmap.getHeight()];
//
//
//
////              取得YCbCr像素
//                int[] modcropHW = Util.modCrop(inputBitmap);
//                inputBitmap.getPixels(inputBitmapPixel, 0, inputBitmap.getWidth(), 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight());
//
//                inputColorArray = Util.YCbCr(inputBitmapPixel, modcropHW[1], modcropHW[0]);
//
////              分割圖片多個40*40子圖像
//                spliteImage(inputColorArray);
//                bitmapFromArray(inputSubImageArrayList);
//
////              建立tfltie 類別並丟資料
//                mTflite = new TFlite(MainActivity.this, mCheckBox.isChecked(), mCheckBoxGPU.isChecked(), onBackImg, nHeight, nWidth);
//                mTflite.setInputImageArrayList(inputSubImageArrayList);
//                nHeight=0;
//                nWidth=0;
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });

    }


//   開啟 GPU or NNAPI
    private void setView() {
        mCheckBox = findViewById(R.id.checkbox);
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mTflite.recreateInterpreter(true);
                } else {
                    mTflite.recreateInterpreter(false);
                }
            }
        });
        mCheckBoxGPU = findViewById(R.id.checkbox_1);
        mCheckBoxGPU.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
    }

//    運算完成set view
    private TFlite.OnBackImg onBackImg = new TFlite.OnBackImg() {
        @Override
        public void finish(Bitmap bmp) {
            imageView.setImageBitmap(bmp);
            inputSubImageArrayList.clear();

        }

        @Override
        public void finish_mes() {
        }
    };

//   分割子圖像 原圖左上方開始切
    private void spliteImage(float[][][] inputColorArray) {
        for (int x = 0; x <= inputColorArray.length - IMAGE_SIZE; x += IMAGE_SIZE) {
            nHeight += 1;
            nWidth = 0;
            for (int y = 0; y <= inputColorArray[0].length - IMAGE_SIZE; y += IMAGE_SIZE) {
                nWidth += 1;
///             切子圖
                saveInputSubImage(x, y);
            }
        }
    }


//  切子圖像用 Arraylist 儲存 SubImageItem class
    private void saveInputSubImage(int x, int y) {

        int resetY = y;
        float[][][] YArray = new float[IMAGE_SIZE][IMAGE_SIZE][1];
        float[][][] CbCrArray = new float[IMAGE_SIZE][IMAGE_SIZE][2];

        for (int i = 0; i < IMAGE_SIZE; i++) {
            for (int j = 0; j < IMAGE_SIZE; j++) {
                YArray[i][j][0] = inputColorArray[x][y][0];
                CbCrArray[i][j][0] = inputColorArray[x][y][1];
                CbCrArray[i][j][1] = inputColorArray[x][y][2];

                y++;
            }
            y = resetY;
            x++;
        }
        inputSubImageArrayList.add(new SubImageItem(YArray, CbCrArray));
    }

//  Asset拿資料
    private Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream;
        Bitmap bitmap = null;
        try {
            inputStream = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {

        }

        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    //    save out put image
    public static void saveOutputBitmap(Bitmap pBitmap) throws IOException {
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, "output.png");

        OutputStream outStream = new FileOutputStream(file);

        pBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);

        outStream.flush();
        outStream.close();
    }



    public static void saveInputBitmap(float[][][] arr) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(arr[0].length, arr.length, Bitmap.Config.ARGB_8888);
        for (int row = 0; row < arr.length; row++) {
            for (int col = 0; col < arr[0].length; col++) {
                bitmap.setPixel(col, row, Color.argb(255, (int) (arr[row][col][0]), (int) (arr[row][col][0]), (int) (arr[row][col][0])));
            }
        }
        try {
            File root = Environment.getExternalStorageDirectory();
            File file = new File(root, "input.png");
            OutputStream outStream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            Log.e("TAG", "saveInputBitmap:" + e.toString());
        }

    }

    public void bitmapFromArray(ArrayList<SubImageItem> originArrayList) {

        outputArray = new float[nHeight * IMAGE_SIZE][nWidth * IMAGE_SIZE][3];
        boolean checkImage = true;
        for (int idx = 0; idx < originArrayList.size(); idx++) {
            int i = idx % nWidth; //01234567
            int j = idx / nWidth;//0  1  2 3 4 5 6 7 8 9  ~48
            int countX = 0;
            int countY = 0;
            float[][][] subImage = originArrayList.get(idx).getYArray();
            for (int h = IMAGE_SIZE * j; h < IMAGE_SIZE * j + IMAGE_SIZE; h++) {
                for (int w = IMAGE_SIZE * i; w < IMAGE_SIZE * i + IMAGE_SIZE; w++) {
                    outputArray[h][w] = subImage[countX][countY];//2121
                    if (checkImage) {
                        checkImage = false;
                        try {
                            saveInputBitmap(subImage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    countY++;
                }
                countY = 0;
                countX++;
            }
        }

    }



//  一次跑5張圖片
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < set5.length; i++) {
                    inputBitmap = getBitmapFromAsset(MainActivity.this, set5[i]);
//            Toast.makeText(MainActivity.this, "您選擇了:" + set5[i], Toast.LENGTH_SHORT).show();
                    inputBitmapPixel = new int[inputBitmap.getWidth() * inputBitmap.getHeight()];
                    int[] modcropHW = Util.modCrop(inputBitmap);
                    inputBitmap.getPixels(inputBitmapPixel, 0, inputBitmap.getWidth(), 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight());

                    inputColorArray = Util.YCbCr(inputBitmapPixel, modcropHW[1], modcropHW[0]);

                    spliteImage(inputColorArray);
                    bitmapFromArray(inputSubImageArrayList);
                    mTflite = new TFlite(this, mCheckBox.isChecked(),mCheckBoxGPU.isChecked(), onBackImg, nHeight, nWidth);
                    mTflite.setInputImageArrayList(inputSubImageArrayList);
                    inputSubImageArrayList.clear();
                    nWidth = 0;
                    nHeight = 0;
                }
                button.setBackgroundColor(Color.GREEN);


            case R.id.camera:

//                Intent intent = new Intent(
//                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                File tmpFile = new File(
                        Environment.getExternalStorageDirectory(), "bbc.png");
                BitmapUri=Environment.getExternalStorageDirectory()+"/bbc.png";
                Log.d("",""+BitmapUri);
                outputFileUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider",tmpFile);
                bmp = BitmapFactory.decodeFile(BitmapUri);
                BitmapDrawable bitmapDrawable = new BitmapDrawable(bmp);
                ImageView imageView =findViewById(R.id.photo);
                imageView.setImageDrawable(bitmapDrawable);


                inputBitmap = bmp;
                inputBitmapPixel = new int[inputBitmap.getWidth() * inputBitmap.getHeight()];

//
                inputBitmap.getPixels(inputBitmapPixel, 0, inputBitmap.getWidth(), 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight());

                int[] modcropHW = Util.modCrop(inputBitmap);
                inputColorArray = Util.RGB(inputBitmapPixel, modcropHW[1], modcropHW[0]);


                inputColorArray = rgb2Ycbcr(inputColorArray);

//              取得YCbCr像素
//                int[] modcropHW = Util.modCrop(inputBitmap);
//                inputBitmap.getPixels(inputBitmapPixel, 0, inputBitmap.getWidth(), 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight());
//                inputColorArray = Util.YCbCr(inputBitmapPixel, modcropHW[1], modcropHW[0]);

//              分割圖片多個40*40子圖像
                spliteImage(inputColorArray);
                bitmapFromArray(inputSubImageArrayList);

//              建立tfltie 類別並丟資料
                mTflite = new TFlite(MainActivity.this, mCheckBox.isChecked(), mCheckBoxGPU.isChecked(), onBackImg, nHeight, nWidth);
                mTflite.setInputImageArrayList(inputSubImageArrayList);
                nHeight=0;
                nWidth=0;


        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK) {
            Toast.makeText(this, "成功了", Toast.LENGTH_SHORT).show();

        }
    }
}
