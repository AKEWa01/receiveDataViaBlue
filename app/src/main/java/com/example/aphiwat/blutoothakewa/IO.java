package com.example.aphiwat.blutoothakewa;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class IO {
    public static void saveFile(String str) {

        File file = new File("GG.dat");
        Log.d("Save", str);
        try {
            ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(file));
            writer.writeObject(str);
            writer.close();
        }catch (Exception e){}
    }
    public static String loadStr(){
        String str;
        try
        {
            FileInputStream fis = new FileInputStream("GG.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            str = (String) ois.readObject();
            ois.close();
            fis.close();
        }catch(IOException ioe){
            //System.out.println("File Not Found . Create a New Flie");
            return null;
        }catch(ClassNotFoundException c){
            //System.out.println("Class not found");
            c.printStackTrace();
            return null;
        }
        Log.d("Load", "TTTTTTTT");
        Log.d("Load", str);
        return str;
    }
}
