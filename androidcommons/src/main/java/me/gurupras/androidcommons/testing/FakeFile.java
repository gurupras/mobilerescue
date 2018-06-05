package me.gurupras.androidcommons.testing;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.Random;

public class FakeFile extends File {
    private static final long MAX_FILENAME_LENGTH = 64;
    private static final long MIN_FILENAME_LENGTH = 6;

    private boolean isDirectory;
    private int numChildren;
    private FakeFile[] children;

    public FakeFile(@NonNull String pathname) {
        this(pathname, false);
    }

    public FakeFile(@NonNull String pathname, boolean isDirectory) {
        super(pathname);
        this.isDirectory = isDirectory;
        if (isDirectory) {
            numChildren = (int) (1000 + (Math.random() * 3000));
            children = new FakeFile[numChildren];
            for(int i = 0; i < numChildren; i++) {
                children[i] = new FakeFile(this.getAbsolutePath() + "/" + generateRandomFilename());
            }
        }
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isFile() {
        return !isDirectory;
    }

    @Override
    public File[] listFiles() {
        return children;
    }

    @Override
    public String[] list() {
        String[] ret = new String[numChildren];
        for (int idx = 0; idx < numChildren; idx++) {
            ret[idx] = children[idx].getName();
        }
        return ret;
    }

    @Override
    public long length() {
        return (int) (0 + (Math.random() * 104857600));
    }

    private static String generateRandomFilename() {
        int length = (int) (MIN_FILENAME_LENGTH + (Math.random() * (MAX_FILENAME_LENGTH - MIN_FILENAME_LENGTH + 1)));
        StringBuffer sb = new StringBuffer();
        Random rd = new Random();
        String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        for(int i = 0; i < length; i++) {
            char letter = abc.charAt(rd.nextInt(abc.length()));
            sb.append(letter);
        }
        return sb.toString();
    }

    public void setNumChildren(int numChildren) {
        if (!this.isDirectory) {
            throw new IllegalStateException("File cannot have children");
        }
        if (numChildren > this.numChildren) {
            FakeFile[] newChildren = new FakeFile[numChildren];
            for(int i = 0; i < numChildren; i++) {
                if (i < this.numChildren) {
                    newChildren[i] = children[i];
                } else {
                    newChildren[i] = new FakeFile(this.getAbsolutePath() + "/" + generateRandomFilename());
                }
            }
            this.children = newChildren;
        }
        this.numChildren = numChildren;
    }

    @Override
    public boolean exists () {
        return true;
    }
}