package com.example.nutc.vdsr;

/**
 * Created by  Shawn on 2018/11/21.
 */

public class OutputImageItem {

    public float[][][] array;

    public OutputImageItem(float[][][] initialArray){
        array = initialArray;
    }

    public float[][][] getArray() {
        return array;
    }
}
