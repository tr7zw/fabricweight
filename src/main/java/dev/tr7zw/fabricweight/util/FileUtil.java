package dev.tr7zw.fabricweight.util;

import java.io.File;

public class FileUtil {

    public static void delete(File file) {
        if(!file.exists()) {
            return;
        }
        if(file.isDirectory()) {
            for(File f : file.listFiles())
                delete(f);
        }
        file.delete();
    }
    
}
