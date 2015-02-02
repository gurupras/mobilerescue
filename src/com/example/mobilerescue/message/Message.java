package com.example.mobilerescue.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.example.mobilerescue.Helper.pad;

public abstract class Message {

	private int messageLength;
	private MessageType messageType;

	/**
	 * @return the messageLength
	 */
	public int getMessageLength() {
		return messageLength;
	}
	/**
	 * @param messageLength the messageLength to set
	 */
	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}
	/**
	 * @return the messageType
	 */
	public MessageType getMessageType() {
		return messageType;
	}
	/**
	 * @param messageType the messageType to set
	 */
	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}
	public abstract byte[] build();
	public abstract void init();
	public abstract void parse(byte[] bytes) throws Exception;




	public static Message parseMessage(byte[] bytes) throws Exception {
		Message message = null;

		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		int messageLength 	= bb.getInt();
		int messageTypeInt	= bb.getInt();
		MessageType messageType = MessageType.values()[messageTypeInt];

		int remainingBytes = messageLength - bb.position();
		ByteBuffer buffer = ByteBuffer.allocate(remainingBytes);
		buffer.put(bytes, bb.position(), remainingBytes);
		buffer.rewind();
		bb = null;

		switch(messageType) {
		case UPLOAD_REQUEST:
			UploadRequestMessage dreqm = new UploadRequestMessage();
			dreqm.parse(buffer.array());
			message = dreqm;
			break;
		case UPLOAD_RESPONSE:
			UploadResponseMessage dresm = new UploadResponseMessage();
			dresm.parse(buffer.array());
			message = dresm;
			break;
		default:
			throw new IllegalStateException("Unimplemented message type :" + messageType);
		}

		message.setMessageLength(messageLength);
		message.setMessageType(messageType);
		return message;
	}

	public static enum MessageType {
		UPLOAD_REQUEST,
		UPLOAD_RESPONSE,
		;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(pad("message length", 30) + ":" + messageLength + "\n");
		builder.append(pad("message type", 30)   + ":" + messageType.name() + "\n");
		return builder.toString();
	}
}

