package me.gurupras.androidcommons;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutputStream extends OutputStream {
	private long bytesWritten;
	private OutputStream out;
	
	public ProgressOutputStream(OutputStream out) {
		this.out = out;
		bytesWritten = 0;
	}
	
	@Override
	public void write(int oneByte) throws IOException {
		out.write(oneByte);
		bytesWritten++;
	}
	
	@Override
	public void write(byte[] buffer) throws IOException {
		this.write(buffer, 0, buffer.length);
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		int maxBufSize = 512 * 1024;
		if(count > maxBufSize) {
			int localOffset = 0;
			while(localOffset < count) {
				int len = maxBufSize > (count - localOffset) ? (count - localOffset) : maxBufSize;
				out.write(buffer, offset + localOffset, len);
				localOffset += len;
				bytesWritten += len;
			}
		}
		else {
			out.write(buffer, offset, count);
			bytesWritten += count;
		}
	}

	public long getBytesWritten() {
		return this.bytesWritten;
	}
	
	
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}
	
	@Override
	public void close() throws IOException {
		out.close();
	}
}
