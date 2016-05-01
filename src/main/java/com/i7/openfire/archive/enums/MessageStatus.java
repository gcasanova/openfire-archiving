package com.i7.openfire.archive.enums;

public enum MessageStatus {
	SENT(0), DELIVERED(1), READ(2);

	private int value;

	MessageStatus(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	public static MessageStatus findByValue(int value) {
		for (MessageStatus status : MessageStatus.values()) {
			if (status.getValue() == value) {
				return status;
			}
		}
		return null;
	}
}
