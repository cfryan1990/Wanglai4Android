package com.cfryan.wanglai4android.util;

import com.cfryan.wanglai4android.R;

public enum StatusMode {
	offline(R.string.status_offline, -1), // 离线状态，没有图标
	dnd(R.string.status_dnd, R.mipmap.status_shield), // 请勿打扰
	xa(R.string.status_xa, R.mipmap.status_invisible), // 隐身
	away(R.string.status_away, R.mipmap.status_leave), // 离开
	available(R.string.status_online, R.mipmap.status_online), // 在线
	chat(R.string.status_chat, R.mipmap.status_qme);// Q我吧

	private final int textId;
	private final int drawableId;

	StatusMode(int textId, int drawableId) {
		this.textId = textId;
		this.drawableId = drawableId;
	}

	public int getTextId() {
		return textId;
	}

	public int getDrawableId() {
		return drawableId;
	}

	@Override
	public String toString() {
		return name();
	}

	public static StatusMode fromString(String status) {
		return StatusMode.valueOf(status);
	}

}
