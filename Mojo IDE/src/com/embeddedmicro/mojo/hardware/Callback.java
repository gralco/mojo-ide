package com.embeddedmicro.mojo.hardware;

public interface Callback {
	public void onSuccess();
	public void onError(String error);
}
