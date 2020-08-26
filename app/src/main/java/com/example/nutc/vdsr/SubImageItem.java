package com.example.nutc.vdsr;

/**
 * Created by  Shawn on 2018/11/21.
 */

public class SubImageItem {

    public float[][][] YArray;
    public float[][][] cbcrArray;


    public SubImageItem(float[][][] YArray,float[][][] cbcrArray){
        this.YArray = YArray;
        this.cbcrArray = cbcrArray;
    }



    public float[][][] getYArray() {
        return YArray;
    }
    public float[][][] getCbCrArray() {
        return cbcrArray;
    }


}
