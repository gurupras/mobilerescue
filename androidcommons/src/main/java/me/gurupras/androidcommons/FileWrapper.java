package me.gurupras.androidcommons;

import java.io.File;

public class FileWrapper implements IFile {
    private File file;

    public FileWrapper(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public IFile getFile() {
        return this;
    }

    @Override
    public IFile[] listFiles() {
        File[] children = file.listFiles();
        IFile[] ret = new IFile[children.length];
        for (int idx = 0; idx < children.length; idx++) {
            ret[idx] = new FileWrapper(children[idx]);
        }
        return ret;
    }

    @Override
    public String[] list() {
        return file.list();
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }
}
