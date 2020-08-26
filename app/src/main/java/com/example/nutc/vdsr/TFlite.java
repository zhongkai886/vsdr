package com.example.nutc.vdsr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * Created by  Shawn on 2018/11/10.
 */

public class TFlite {
    private Interpreter tflite;
    private ByteBuffer inputBuffer;
    private Context context;
    private MappedByteBuffer tfliteModel;
    private Interpreter.Options tfliteOptions;

    private float[][][][] subOutputArray;
    private float[][][] floatArr;
    private ArrayList<SubImageItem> inputImageArrayList;
    private ArrayList<OutputImageItem> outputArrayList;
    private Bitmap bitmap;
    float[][][] outputArray;
    float[][][] RGBArray;
    private int nHeight = 0;
    private int nWidth = 0;

    public interface OnBackImg {
        void finish(Bitmap bmp);
        void finish_mes();
    }
    private ArrayList<Long> inferenceList = new ArrayList<>();
    private OnBackImg onBackImg;
    private GpuDelegate gpuDelegate = null;

    public void setonBackImg(OnBackImg onBackImg) {
        this.onBackImg = onBackImg;
    }

    private static  final int IMAGE_SIZE =40;

//   Ycbcr轉 RGB 參數
    private float[][] transformMatrix =
            {{1, 1, 1},
            {0, -0.34404f, 1.772f},
            {1.402f, -.71404f, 0}};
    //   RGB 轉 Ycbcr 參數
    private float[][] transformMatrixRgb =
                    {{0.299f, -0.1687f, 0.5f},
                    {0.587f, -0.3313f, -0.4187f},
                    {0.114f, 0.5f, -0.0813f}};
    private String modelName = "size40_6c14d_ch64_ep30.tflite";

    public TFlite(Context context, boolean nnapi, boolean gpu, OnBackImg onBackImg, int nHeight, int nWidth) {
        this.context = context;
        this.onBackImg = onBackImg;
        this.nHeight = nHeight;
        this.nWidth = nWidth;
        try {
            tfliteModel = loadModelFile(context);
        } catch (IOException e) {
            e.printStackTrace();
        }


//        建立option 起用NNAPI 或 GPU
        tfliteOptions = new Interpreter.Options();
        tfliteOptions.setNumThreads(1);

        if (gpu) {
            gpuDelegate = new GpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
        } else {
            tfliteOptions.setUseNNAPI(nnapi);

        }

        tflite = new Interpreter(tfliteModel, tfliteOptions);
        floatArr = new float[IMAGE_SIZE][IMAGE_SIZE][1];

        outputArray = new float[240][240][1];
        RGBArray = new float[240][240][1];

        outputArrayList = new ArrayList<>();
    }


//    Mainactivity 丟資料
    public void setInputImageArrayList(ArrayList<SubImageItem> imageItems) {
        this.inputImageArrayList = imageItems;
        convertBitmapToByteBuffer();
    }


    public void recreateInterpreter(boolean open) {
        tfliteOptions.setUseNNAPI(open);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        if (tflite != null) {
            tflite.close();
            tflite = new Interpreter(tfliteModel, tfliteOptions);
        }
    }


    private void convertBitmapToByteBuffer() {

//      從list中拿出子圖像 並給予buffer空間

        for (int i = 0; i < inputImageArrayList.size(); i++) {
//          長 * 寬 * 通道 * float byte
            inputBuffer = ByteBuffer.allocateDirect(40 * 40 * 4);
            inputBuffer.order(ByteOrder.nativeOrder());
            inputBuffer.rewind();

            float[][][] input2dArr = inputImageArrayList.get(i).getYArray();
            for (int j = 0; j < 40; ++j) {
                for (int k = 0; k < 40; ++k) {
                    float Y = input2dArr[j][k][0] / 255;
                    addPixelValue(Y);

                }
            }
//   疑似沒有clear 小圖片會一直出現同一張
//          inputBuffer.clear();
//            開始執行
            outputArrayList.add(new OutputImageItem(runInference(i)));

        }


//      array 轉 bitmap
        bitmapFromArray(outputArrayList);
    }

    private void addPixelValue(float pixelValue) {
        inputBuffer.putFloat(pixelValue);
    }


    public float[][][] runInference(int c) {
        subOutputArray = new float[1][IMAGE_SIZE][IMAGE_SIZE][1];
        floatArr = new float[IMAGE_SIZE][IMAGE_SIZE][1];
        Long stime = System.currentTimeMillis();
        tflite.run(inputBuffer, subOutputArray);
        Long inftime = System.currentTimeMillis() - stime;
        inferenceList.add(inftime);

//      四維轉三維
        for (int j = 0; j < IMAGE_SIZE; j++) {
            for (int k = 0; k < IMAGE_SIZE; k++) {
                floatArr[j][k] = subOutputArray[0][j][k];
            }
        }

//        儲存輸出圖片用
//        try {
//            saveInputBitmap(floatArr, c);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        subOutputArray = null;

        return floatArr;
    }

//
    public void bitmapFromArray(ArrayList<OutputImageItem> outputArrayList) {
        outputArray = new float[nHeight * IMAGE_SIZE][nWidth * IMAGE_SIZE][3];
        RGBArray = new float[nHeight * IMAGE_SIZE][nWidth * IMAGE_SIZE][3];

        ArrayList<OutputImageItem> finalOutputArrayList = new ArrayList<>();
        boolean b = true;

//       將CbCr加回去
        for (int i = 0; i < outputArrayList.size(); i++) {
//          input  h w c
            float[][][] newColorImg = new float[IMAGE_SIZE][IMAGE_SIZE][3];
            float[][][] subimg = outputArrayList.get(i).getArray();

            for (int h = 0; h < IMAGE_SIZE; h++) {
                for (int w = 0; w < IMAGE_SIZE; w++) {
                    newColorImg[h][w][0] = subimg[h][w][0];
                    newColorImg[h][w][1] = inputImageArrayList.get(i).getCbCrArray()[h][w][0];
                    newColorImg[h][w][2] = inputImageArrayList.get(i).getCbCrArray()[h][w][1];
                }
            }
            finalOutputArrayList.add(new OutputImageItem(newColorImg));
        }


//        將子圖像合併成 原始大圖像
        for (int idx = 0; idx < finalOutputArrayList.size(); idx++) {
//
            int i = idx % nWidth;
            int j = idx / nWidth;
            int countX = 0;
            int countY = 0;
            float[][][] subImage = finalOutputArrayList.get(idx).getArray();

            for (int h = IMAGE_SIZE * j; h < IMAGE_SIZE * j + IMAGE_SIZE; h++) {
                for (int w = IMAGE_SIZE * i; w < IMAGE_SIZE * i + IMAGE_SIZE; w++) {
                    outputArray[h][w] = subImage[countX][countY];
                    countY++;
                }
                countY = 0;
                countX++;
            }
        }

//          YCbCr 轉成 RGB  (矩陣乘法)
        for (int h = 0; h < outputArray.length; h++) {
            for (int w = 0; w < outputArray[0].length; w++) {
                float Y = outputArray[h][w][0] * 255;
                float cb = outputArray[h][w][1] - 128;
                float cr = outputArray[h][w][2] - 128;

                RGBArray[h][w][0] = Y * transformMatrix[0][0] + cb * transformMatrix[1][0] + cr * transformMatrix[2][0];
                RGBArray[h][w][1] = Y * transformMatrix[0][1] + cb * transformMatrix[1][1] + cr * transformMatrix[2][1];
                RGBArray[h][w][2] = Y * transformMatrix[0][2] + cb * transformMatrix[1][2] + cr * transformMatrix[2][2];

            }
        }

//


////          find Y  min and max
        float minR = 0.0f, maxR = 0.0f;
        float minGreen = 0.0f, maxGreen = 0.0f;
        float minBlue = 0.0f, maxBlue = 0.0f;
        for (int i = 0; i < nHeight * IMAGE_SIZE; i++) {
            for (int j = 0; j < nWidth * IMAGE_SIZE; j++) {
                if (RGBArray[i][j][0] < minR) minR = RGBArray[i][j][0];
                else if (RGBArray[i][j][0] > maxR) maxR = RGBArray[i][j][0];
//
            }
        }

        for (int i = 0; i < nHeight * IMAGE_SIZE; i++) {
            for (int j = 0; j < nWidth * IMAGE_SIZE; j++) {
                if (RGBArray[i][j][1] < minGreen) minGreen = RGBArray[i][j][1];
                else if (RGBArray[i][j][1] > maxGreen) maxGreen = RGBArray[i][j][1];
//
            }
        }

        for (int i = 0; i < nHeight * IMAGE_SIZE; i++) {
            for (int j = 0; j < nWidth * IMAGE_SIZE; j++) {
                if (RGBArray[i][j][2] < minBlue) minBlue = RGBArray[i][j][2];
                else if (RGBArray[i][j][2] > maxBlue) maxBlue = RGBArray[i][j][2];
//
            }
        }
        Log.d("dddd",""+minR);

        for (int h = 0; h < RGBArray.length; h++) {
            for (int w = 0; w < RGBArray[0].length; w++) {
               RGBArray[h][w][0]= ((RGBArray[h][w][0]-minR)/(maxR-minR))*255;
               RGBArray[h][w][1]= ((RGBArray[h][w][1]-minGreen)/(maxGreen-minGreen))*255;
               RGBArray[h][w][2]= ((RGBArray[h][w][2]-minBlue)/(maxBlue-minBlue))*255;
               Log.d("ddddd",""+RGBArray[h][w][2]);
            }
        }

        convertToColorImage(RGBArray);
    }

//   陣列成圖片
    @SuppressLint("NewApi")
    public void convertToColorImage(float[][][] arr) {
        Log.d("yyyy", "convertToColorImage: "+nHeight+"////"+nWidth);
        Bitmap bm = Bitmap.createBitmap(nWidth * IMAGE_SIZE, nHeight * IMAGE_SIZE, Bitmap.Config.ARGB_8888);
        for (int row = 0; row < nHeight * IMAGE_SIZE; row++) {
            for (int col = 0; col < nWidth * IMAGE_SIZE; col++) {
                bm.setPixel(col, row, Color.argb(255, (int) (arr[row][col][0]), (int) (arr[row][col][1]), (int) (arr[row][col][2])));
            }
        }
        onBackImg.finish(bm);

        this.bitmap = bm;
    }


//    讀Asset 模型
    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = context.getAssets().openFd(modelName);

        } catch (Exception e) {
            Log.e("TAG", "loadModelFile:" + e.toString());
        }

        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    public static void saveInputBitmap(float[][][] arr, int c) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(arr[0].length, arr.length, Bitmap.Config.ARGB_8888);

        for (int row = 0; row < arr.length; row++) {
            for (int col = 0; col < arr[0].length; col++) {
                bitmap.setPixel(col, row, Color.argb(255, (int) (arr[row][col][0]) * 255, (int) (arr[row][col][0]) * 255, (int) (arr[row][col][0]) * 255));
            }
        }
        try {
            File root = Environment.getExternalStorageDirectory();
            File file = new File(root + "/inputtest/", String.valueOf(c) + ".png");
            OutputStream outStream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
        }

    }

    public Bitmap rgb2Ycbcr(){
//        RGB  轉 ycbcr (矩陣乘法)
        for (int h = 0; h < outputArray.length; h++) {
            for (int w = 0; w < outputArray[0].length; w++) {
                float Y = outputArray[h][w][0] * 255;
                float cb = outputArray[h][w][1] + 128;
                float cr = outputArray[h][w][2] + 128;

                RGBArray[h][w][0] = Y * transformMatrixRgb[0][0] + cb * transformMatrixRgb[1][0] + cr * transformMatrixRgb[2][0];
                RGBArray[h][w][1] = Y * transformMatrixRgb[0][1] + cb * transformMatrixRgb[1][1] + cr * transformMatrixRgb[2][1];
                RGBArray[h][w][2] = Y * transformMatrixRgb[0][2] + cb * transformMatrixRgb[1][2] + cr * transformMatrixRgb[2][2];

            }
        }
        convertToColorImage(RGBArray);
        return bitmap ;

    }

}
