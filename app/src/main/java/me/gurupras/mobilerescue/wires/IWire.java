package me.gurupras.mobilerescue.wires;

import android.app.Dialog;

import java.io.File;

import me.gurupras.androidcommons.FileEntry;
import me.gurupras.androidcommons.FileMetadata;

public interface IWire {
    public void connect(String hostname, int port, String ...extras) throws Exception;
    public void upload(FileEntry entry) throws Exception;
    public void download(FileEntry src) throws Exception;
    public void disconnect() throws Exception;
}
