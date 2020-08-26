package com.example.nutc.vdsr;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by  Shawn on 2018/11/29.
 */

public class Util {

    //    first  make image  modCrop
    public static int[] modCrop(Bitmap bitmap) {
        int h, w;
        h = bitmap.getHeight();
        w = bitmap.getWidth();

//        h = h - h % 3;
//        w = w - w % 3;

        int[] HW = new int[2];
//        float[][] modeCropArray = new float[h][w];
//        for (int i = 0; i < h; i++) {
//            for (int j = 0; j < w; j++) {
//                modeCropArray[i][j] = image[i][j];
//            }
//        }
        HW[0] = h;
        HW[1] = w;
        return HW;
    }

    public static float[][][] YCbCr(int[] scaleUpArray, int width, int height) {
//        float[][][] TwoDPixels = new float[height][width];
        float[][][] ThreeDPixels = new float[height][width][3];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int Y = (scaleUpArray[i * width + j] >> 16) & 0xff;
                int Cb = (scaleUpArray[i * width + j] >> 8) & 0xff;
                int Cr = scaleUpArray[i * width + j] & 0xff;
                float[] color={(float) Y,(float)Cb,(float)Cr};
                ThreeDPixels[i][j]=color;
            }
        }

        return ThreeDPixels;
    }

    public static float[][][] RGB(int[] scaleUpArray, int width, int height) {
//        float[][][] TwoDPixels = new float[height][width];
        float[][][] ThreeDPixels = new float[height][width][3];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int Y = (scaleUpArray[i * width + j] >> 16) & 0xff;
                int Cb = (scaleUpArray[i * width + j] >> 8) & 0xff;
                int Cr = scaleUpArray[i * width + j] & 0xff;
                float[] color={(float) Y,(float)Cb,(float)Cr};
                ThreeDPixels[i][j]=color;
            }
        }

        return ThreeDPixels;
    }
    //    get bicubic pi
    public static int[] bicubicScale(int[] src, int width, int height, int dstWidth, int dstHeight) {
        int[] dst = new int[dstWidth * dstHeight];

        Bitmap srcBitmap = Bitmap.createBitmap(src, width, height, Bitmap.Config.ARGB_8888);
        Bitmap dstBitmap = Bitmap.createScaledBitmap(srcBitmap, dstWidth, dstHeight, true);
        dstBitmap.getPixels(dst, 0, dstWidth, 0, 0, dstWidth, dstHeight);

        srcBitmap.recycle();
        dstBitmap.recycle();
        return dst;
    }

    public static int[] bicubicScale11(int[] src, int width, int height, int dstWidth, int dstHeight) throws IOException {
        int[] dst = new int[dstWidth * dstHeight];




        Bitmap srcBitmap = Bitmap.createBitmap(src, width, height, Bitmap.Config.ARGB_8888);
        Bitmap dstBitmap = Bitmap.createScaledBitmap(srcBitmap, dstWidth, dstHeight, true);
        dstBitmap.getPixels(dst, 0, dstWidth, 0, 0, dstWidth, dstHeight);

        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, "input11.png");
        OutputStream outStream = new FileOutputStream(file);

        dstBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        outStream.flush();
        outStream.close();

        srcBitmap.recycle();
        dstBitmap.recycle();
        return dst;
    }


}
