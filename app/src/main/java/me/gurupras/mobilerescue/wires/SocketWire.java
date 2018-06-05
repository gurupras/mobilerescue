package me.gurupras.mobilerescue.wires;

import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import me.gurupras.androidcommons.FileEntry;
import me.gurupras.androidcommons.Helper;
import me.gurupras.androidcommons.ProgressOutputStream;
import me.gurupras.androidcommons.message.Message;
import me.gurupras.mobilerescue.message.UploadRequestMessage;
import me.gurupras.mobilerescue.message.UploadResponseMessage;

import static me.gurupras.androidcommons.Helper.makeToast;

public class SocketWire extends Wire {
    private static final String TAG = Helper.MAIN_TAG + "->" + SocketWire.class;
    private Socket socket;
    public static int bufferSize = 64 * 1024;

    public SocketWire() {
        super();
    }

    @Override
    public void connect(String hostname, int port, String... extras) throws Exception {
        socket = null;
        try {
            Log.d(TAG, "Attempting to connect to :" + hostname + "@" + port);
            socket = new Socket();
            socket.setSoTimeout(10000);
            socket.connect(new InetSocketAddress(hostname, port));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            makeToast("Unable to connect to " + hostname);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void upload(FileEntry entry) throws Exception {
        final ProgressOutputStream outputStream = new ProgressOutputStream(null);
        outputStream.setOutputStream(socket.getOutputStream());

        UploadRequestMessage urqm = new UploadRequestMessage(entry);
        urqm.init();
        byte[] requestBytes = urqm.build();
        outputStream.write(requestBytes);
        outputStream.flush();

        // Wait for server to send info on whether this file is needed
        UploadResponseMessage ursm = new UploadResponseMessage();
        ursm.init();
        ByteBuffer responseBytes = ByteBuffer.allocate(ursm.getMessageLength());
        DataInputStream socketInputStream = new DataInputStream(socket.getInputStream());
        socketInputStream.read(responseBytes.array());
        ursm = (UploadResponseMessage) Message.parseMessage(responseBytes.array());

        File file = entry.getFile();
        if (ursm.getResponse() == UploadResponseMessage.UploadResponseType.FILE_FOUND) {
            socket.close();
            for(IWireEvents listener : listeners) {
                listener.onRemoteFileExists(urqm);
            }
            return;
        }

        long offset = (long) 0;
        InputStream instream = new FileInputStream(file);
        int defaultBufferSize = bufferSize;
        while (offset < file.length()) {
            int bufferSize = (int) (defaultBufferSize > (file.length() - offset) ? (file.length() - offset) : defaultBufferSize);
            ByteBuffer fileBuffer = ByteBuffer.allocate(bufferSize);
            instream.read(fileBuffer.array());
            outputStream.write(fileBuffer.array());
            for(IWireEvents listener : listeners) {
                listener.onWireProgress(bufferSize);
            }
            offset += bufferSize;
        }
        instream.close();
        outputStream.close();
        Log.d(TAG, "Finished transferring file '" + file.getAbsolutePath() + "'");
    }

    @Override
    public void download(FileEntry src) throws Exception {

    }
}
