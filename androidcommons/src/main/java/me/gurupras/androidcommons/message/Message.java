package me.gurupras.androidcommons.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import me.gurupras.androidcommons.Helper;

import android.util.Log;

import static me.gurupras.androidcommons.Helper.pad;

public abstract class Message {
	private static final String TAG = Helper.createTag(Message.class);
	
	private static Map<Class, Integer> classMap = new HashMap<Class, Integer>();
	private static AtomicInteger counter = new AtomicInteger();
	private int messageLength;
	private int messageType;

	protected static void register(Class clazz) {
		if(!classMap.containsKey(clazz) && Message.class.isAssignableFrom(clazz)) {
			classMap.put(clazz, counter.incrementAndGet());
		}
		else throw new IllegalArgumentException(String.format("Class '%s' Does not extend '%s'", clazz, Message.class));
	}

	public Message(Class clazz) {
		this.setMessageType(classMap.get(clazz));
	}

	public static int getID(Class clazz) { return classMap.get(clazz); }

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
	public int getMessageType() {
		return messageType;
	}
	/**
	 * @param messageType the messageType to set
	 */
	private void setMessageType(int messageType) {
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
		int messageType	= bb.getInt();
		
		Log.d(TAG, "Message Length: " + messageLength);
		Log.d(TAG, "Message Type:   " + messageType);

		int remainingBytes = messageLength - bb.position();
		ByteBuffer buffer = ByteBuffer.allocate(remainingBytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(bytes, bb.position(), remainingBytes);
		buffer.rewind();
		bb = null;


		Class messageClass = null;
		for(Map.Entry<Class, Integer> entry : classMap.entrySet()) {
			if(messageType == entry.getValue()) {
				messageClass = entry.getKey();
				break;
			}
		}
		if(messageClass == null) {
			throw new IllegalStateException("Received message of class which was not registered");
		}

		message = (Message) messageClass.newInstance();
		message.setMessageLength(messageLength);
		message.setMessageType(messageType);
		message.parse(buffer.array());
		return message;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(pad("message length", 30) + ":" + messageLength + "\n");
		builder.append(pad("message type", 30)   + ":" + this.getClass().getName().toUpperCase() + "\n");
		return builder.toString();
	}
}

