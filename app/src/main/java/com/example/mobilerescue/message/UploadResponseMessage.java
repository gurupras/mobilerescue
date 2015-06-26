package com.example.mobilerescue.message;

import com.example.androidcommons.message.Message;

import static com.example.androidcommons.Helper.pad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UploadResponseMessage extends Message {
	private UploadResponseType response;

	static {
		Message.register(UploadResponseMessage.class);
	}

	public UploadResponseMessage() {
		super(UploadResponseMessage.class);
	}

	public UploadResponseMessage(UploadResponseType response, int fileCount) {
		this();
		this.setResponse(response);
	}

	@Override
	public void init() {
		setMessageLength(((Integer.SIZE * 2) / 8) + (Integer.SIZE / 8));
	}

	@Override
	public byte[] build() {
		ByteBuffer buffer = ByteBuffer.allocate(getMessageLength());
		buffer.putInt(getMessageLength());
		buffer.putInt(getMessageType());
		buffer.putInt(getResponse().ordinal());

		return buffer.array();
	}

	@Override
	public void parse(byte[] bytes) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int ordinal = buffer.getInt();
		this.setResponse(UploadResponseType.values()[ordinal]);

		if(buffer.hasRemaining()) {
			throw new Exception("Message format error! Bytes Remaining :" + buffer.remaining());
		}
	}

	/**
	 * @return the response
	 */
	public UploadResponseType getResponse() {
		return response;
	}

	/**
	 * @param response the response to set
	 */
	public void setResponse(UploadResponseType response) {
		this.response = response;
	}

	public static enum UploadResponseType {
		FILE_NOT_FOUND,
		FILE_FOUND,
		;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append(pad("response", 30)  + ":" + response.name() + "\n");
		return builder.toString();
	}
}
