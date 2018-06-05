package me.gurupras.androidcommons;

public interface IFile {
    public String getName();
    public String getAbsolutePath();
    public IFile getFile();
    public IFile[] listFiles();
    public String[] list();
    public long length();
    public boolean exists();
    public boolean isDirectory();
    public boolean isFile();
}