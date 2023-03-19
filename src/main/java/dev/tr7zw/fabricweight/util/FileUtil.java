package dev.tr7zw.fabricweight.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Predicate;

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
    
    public static void moveFiles(File from, File to, Predicate<File> skip) throws IOException {
        for(File f : from.listFiles()) {
            if(skip.test(f)) {
                continue;
            }
            if(f.isFile()) {
                File target = new File(to, f.getName());
                target.getParentFile().mkdirs();
                Files.move(f.toPath(), target.toPath());
            } else {
                moveFiles(f, new File(to, f.getName()), skip);
            }
        }
    }
    
    public static void delete(File file, Predicate<File> skip) {
        if(!file.exists()) {
            return;
        }
        if(file.isDirectory()) {
            for(File f : file.listFiles())
                delete(f, skip);
        }
        if(skip.test(file)) {
            return;
        }
        file.delete();
    }
    
    public static void deleteEmptyDirectories(File file, Predicate<File> skip) {
        if(!file.exists()) {
            return;
        }
        if(file.isDirectory()) {
            for(File f : file.listFiles())
                deleteEmptyDirectories(f, skip);
            if(file.listFiles().length == 0 && !skip.test(file)) {
                file.delete();
            }
        }
    }
    
    
}
