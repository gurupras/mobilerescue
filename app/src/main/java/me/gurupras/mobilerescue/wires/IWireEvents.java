package me.gurupras.mobilerescue.wires;

import me.gurupras.mobilerescue.message.UploadRequestMessage;

public interface IWireEvents {
    public void onRemoteFileExists(UploadRequestMessage urqm);
    public void onWireProgress(int transferred);

    public enum WireEvent {
        CONNECT_FAILED,
        REMOTE_FILE_EXISTS,
        ON_WIRE_PROGRESS,
    }
}
