package com.cfryan.wanglai4android.smack;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.text.TextUtils;
import android.util.Log;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.db.AddPhonesProvider;
import com.cfryan.wanglai4android.db.AddPhonesProvider.PhoneConstants;
import com.cfryan.wanglai4android.db.AvatarProvider;
import com.cfryan.wanglai4android.db.AvatarProvider.AvatarConstants;
import com.cfryan.wanglai4android.db.ChatProvider;
import com.cfryan.wanglai4android.db.ChatProvider.ChatConstants;
import com.cfryan.wanglai4android.db.NewFriendsProvider;
import com.cfryan.wanglai4android.db.NewFriendsProvider.NewFriendsConstants;
import com.cfryan.wanglai4android.db.RosterProvider;
import com.cfryan.wanglai4android.db.RosterProvider.RosterConstants;
import com.cfryan.wanglai4android.exception.XXMPException;
import com.cfryan.wanglai4android.service.XXService;
import com.cfryan.wanglai4android.util.FileUtils;
import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;
import com.cfryan.wanglai4android.util.PreferenceUtils;
import com.cfryan.wanglai4android.util.StatusMode;
import com.cfryan.wanglai4android.util.HttpDownloader;
import com.cfryan.wanglai4android.util.HttpUploader;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class SmackImpl implements Smack
{
	// 客户端名称和类型。主要是向服务器登记，有点类似QQ显示iphone或者Android手机在线的功能
	public static final String XMPP_IDENTITY_NAME = "XMPP";// 客户端名称
	public static final String XMPP_IDENTITY_TYPE = "phone";// 客户端类型

	public static final String REMOTE_HOST = "http://192.168.1.100:8080";

	private static final int PACKET_TIMEOUT = 30000;// 超时时间
	// 发送离线消息的字段
	final static private String[] SEND_OFFLINE_PROJECTION = new String[]
	{ ChatConstants._ID, ChatConstants.JID, ChatConstants.MESSAGE, ChatConstants.DATE, ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_URL,
			ChatConstants.MEDIA_SIZE, ChatConstants.PACKET_ID };
	// 发送离线消息的搜索数据库条件，自己发出去的OUTGOING，并且状态为DS_NEW
	final static private String SEND_OFFLINE_SELECTION = ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING + " AND "
			+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	static
	{
		registerSmackProviders();
	}

	// 做一些基本XMPP协议配置
	static void registerSmackProviders()
	{
		ProviderManager pm = ProviderManager.getInstance();
		// add IQ handling
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
		// add delayed delivery notifications
		pm.addExtensionProvider("delay", "urn:xmpp:delay", new DelayInfoProvider());
		pm.addExtensionProvider("x", "jabber:x:delay", new DelayInfoProvider());
		// add carbons and forwarding
		pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE, new Forwarded.Provider());
		pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
		pm.addExtensionProvider("received", Carbon.NAMESPACE, new Carbon.Provider());
		// add delivery receipts
		pm.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
		pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceiptRequest.Provider());
		// add my message extension
		// pm.addExtensionProvider(MessagePacketExtension.ELEMENT,
		// MessagePacketExtension.NAMESPACE, new
		// MessagePacketExtensionProvider());

		// add XMPP Ping (XEP-0199)
		pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());
		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		ServiceDiscoveryManager.setIdentityName(XMPP_IDENTITY_NAME);
		ServiceDiscoveryManager.setIdentityType(XMPP_IDENTITY_TYPE);
	}

	//操作对象
	private ConnectionConfiguration mXMPPConfig;// 连接配置
	private XMPPConnection mXMPPConnection;// 连接对象
	private XXService mService;// 主服务
	private Roster mRoster;// 联系人对象
	private final ContentResolver mContentResolver;// 数据库操作对象
	private final ContentResolver resolver;//通讯录操作对象

	//各类监听器
	private RosterListener mRosterListener;// 联系人动态监听
	private PacketListener mPacketListener;// 消息动态监听
	private PacketListener mSendFailureListener;// 消息发送失败动态监听
	private PacketListener mPongListener;// ping pong服务器动态监听
	private PacketListener mAvatarListener;// Avatar的动态监听
	private PacketListener mAddListener;//添加朋友监听

	// ping-pong服务器相关
	private String mPingID;// ping服务器的id
	private long mPingTimestamp;// 时间戳
	private PendingIntent mPingAlarmPendIntent;// 是通过闹钟来控制ping服务器的时间间隔
	private PendingIntent mPongTimeoutAlarmPendIntent;// 判断服务器连接超时的闹钟
	private static final String PING_ALARM = "com.cfryan.wanglai4android.PING_ALARM";// ping服务器闹钟BroadcastReceiver的Action
	private static final String PONG_TIMEOUT_ALARM = "com.cfryan.wanglai4android.PONG_TIMEOUT_ALARM";// 判断连接超时的闹钟BroadcastReceiver的Action
	private Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();

	// 不在线状态
	private static final String OFFLINE_EXCLUSION = RosterConstants.STATUS_MODE + " != " + StatusMode.offline.ordinal();
	// 联系人查询序列
	private static final String[] ROSTER_QUERY = new String[]
	{ RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS, RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE, };
	private static final String[] AVATAR_QUERY = new String[]
	{ AvatarConstants._ID, AvatarConstants.JID, AvatarConstants.ALIAS, AvatarConstants.PHOTO_HASH };
	private static final String[] PHONE_QUERY = new String[]
	{ PhoneConstants._ID, PhoneConstants.PHONE_NUM, PhoneConstants.NAME };
	
	private static final String[] PHONES_PROJECTION = new String[] {
		Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID, Phone.CONTACT_ID };

	private static final int PHONES_DISPLAY_NAME_INDEX = 0;

	private static final int PHONES_NUMBER_INDEX = 1;

	private static final int PHONES_PHOTO_ID_INDEX = 2;

	private static final int PHONES_CONTACT_ID_INDEX = 3;

	// ping-pong服务器

	public SmackImpl(XXService service)
	{
		String customServer = PreferenceUtils.getPrefString(service, PreferenceConstants.CUSTOM_SERVER, "");// 用户手动设置的服务器名称，本来打算给用户指定服务器的
		int port = PreferenceUtils.getPrefInt(service, PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);// 端口号，也是留给用户手动设置的
		String server = PreferenceUtils.getPrefString(service, PreferenceConstants.Server, PreferenceConstants.DEFAULT_SERVER);// 默认的服务器，即谷歌服务器
		boolean smackdebug = PreferenceUtils.getPrefBoolean(service, PreferenceConstants.SMACKDEBUG, false);// 是否需要smack
																											// debug
		boolean requireSsl = PreferenceUtils.getPrefBoolean(service, PreferenceConstants.REQUIRE_TLS, false);// 是否需要ssl安全配置
		if (customServer.length() > 0 || port != PreferenceConstants.DEFAULT_PORT_INT)
			this.mXMPPConfig = new ConnectionConfiguration(customServer, port, server);
		else
			this.mXMPPConfig = new ConnectionConfiguration(server); // use SRV

		this.mXMPPConfig.setReconnectionAllowed(true);
		this.mXMPPConfig.setSendPresence(true);
		this.mXMPPConfig.setCompressionEnabled(false); // disable for now
		this.mXMPPConfig.setDebuggerEnabled(true/* smackdebug */);
		//解决“java.security.KeyStoreException: KeyStore jks implementation not found”报错
		this.mXMPPConfig.setTruststorePath("/system/etc/security/cacerts.bks");
		this.mXMPPConfig.setTruststorePassword("changeit");
		this.mXMPPConfig.setTruststoreType("bks");
		if (requireSsl)
			this.mXMPPConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
		this.mService = service;
		mContentResolver = service.getContentResolver();
		resolver = service.getContentResolver();
	}

	/**
	 * 登录
	 * @param account
	 *            账号
	 * @param password
	 *            密码
	 * @return
	 * @throws XXMPException
	 */
	@Override
	public boolean login(String account, String password) throws XXMPException
	{
		try
		{
			if (mXMPPConnection.isConnected())
			{// 首先判断是否还连接着服务器，需要先断开
				try
				{
					mXMPPConnection.disconnect();
				} catch (Exception e)
				{
					L.d("conn.disconnect() failed: " + e);
				}
			}
			SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);// 设置超时时间
			SmackConfiguration.setKeepAliveInterval(-1);
			SmackConfiguration.setDefaultPingInterval(0);
			registerRosterListener();// 监听联系人动态变化
			mXMPPConnection.connect();
			if (!mXMPPConnection.isConnected())
			{
				throw new XXMPException("SMACK connect failed without exception!");
			}
			mXMPPConnection.addConnectionListener(new ConnectionListener()
			{
				@Override
				public void connectionClosedOnError(Exception e)
				{
					mService.postConnectionFailed(e.getMessage());// 连接关闭时，动态反馈给服务
				}

				@Override
				public void connectionClosed()
				{
				}

				@Override
				public void reconnectingIn(int seconds)
				{
				}

				@Override
				public void reconnectionFailed(Exception e)
				{
				}

				@Override
				public void reconnectionSuccessful()
				{
				}
			});
			initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated())
			{
				String ressource = PreferenceUtils.getPrefString(mService, PreferenceConstants.RESSOURCE, XMPP_IDENTITY_NAME);
				mXMPPConnection.login(account, password, ressource);
			}
			setStatusFromConfig();// 更新在线状态

		} catch (XMPPException e)
		{
			throw new XXMPException(e.getLocalizedMessage(), e.getWrappedThrowable());
		} catch (Exception e)
		{
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			L.e(SmackImpl.class, "login(): " + Log.getStackTraceString(e));
			throw new XXMPException(e.getLocalizedMessage(), e.getCause());
		}
		registerAllListener();// 注册监听其他的事件，比如新消息
		return mXMPPConnection.isAuthenticated();
	}

	/**
	 * 注册
	 * 
	 * @param account
	 *            注册帐号
	 * @param password
	 *            注册密码
	 * @return 1、注册成功 0、服务器没有返回结果2、这个账号已经存在3、注册失败
	 */
	public String register(String account, String password)
	{
		try
		{
			if (mXMPPConnection.isConnected())
			{// 首先判断是否还连接着服务器，需要先断开
				try
				{
					mXMPPConnection.disconnect();
				} catch (Exception e)
				{
					L.d("conn.disconnect() failed: " + e);
				}
			}
			mXMPPConnection.connect();
		} catch (XMPPException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (mXMPPConnection == null)
			return "0";
		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);
		reg.setTo(mXMPPConnection.getServiceName());
		reg.setUsername(account);// 注意这里createAccount注册时，参数是username，不是jid，是“@”前面的部分。
		reg.setPassword(password);
		reg.addAttribute("android", "geolo_createUser_android");// 这边addAttribute不能为空，否则出错。所以做个标志是android手机创建
		PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()), new PacketTypeFilter(IQ.class));
		PacketCollector collector = mXMPPConnection.createPacketCollector(filter);
		mXMPPConnection.sendPacket(reg);
		IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
		// Stop queuing results
		collector.cancel();// 停止请求results（是否成功的结果）
		if (result == null)
		{
			Log.e("RegistActivity", "No response from server.");
			return "0";
		} else if (result.getType() == IQ.Type.RESULT)
		{
			return "1";
		} else
		{ // if (result.getType() == IQ.Type.ERROR)
			if (result.getError().toString().equalsIgnoreCase("conflict(409)"))
			{
				Log.e("RegistActivity", "IQ.Type.ERROR: " + result.getError().toString());
				return "2";
			} else
			{
				Log.e("RegistActivity", "IQ.Type.ERROR: " + result.getError().toString());
				return "3";
			}
		}
	}

	/**
	 * 注册所有的监听
	 */
	private void registerAllListener()
	{
		// actually, authenticated must be true now, or an exception must have
		// been thrown.
		if (isAuthenticated())
		{
			registerMessageListener();// 注册新消息监听
			registerMessageSendFailureListener();// 注册消息发送失败监听
			registerPongListener();// 注册服务器回应ping消息监听
			registerAvatarListener();// 注册头像更新监听
			registerAddLinster();//注册好友添加监听
			
			sendOfflineMessages();// 发送离线消息
			if (mService == null)
			{
				mXMPPConnection.disconnect();
				return;
			}
			// we need to "ping" the service to let it know we are actually
			// connected, even when no roster entries will come in
			mService.rosterChanged();
		}
	}

	/************ start 新消息处理 ********************/
	/************ 需要加入包的处理，根据包的不同类型：文本、语言、图片、文件 ***************/
	private void registerMessageListener()
	{
		// do not register multiple packet listeners
		if (mPacketListener != null)
			mXMPPConnection.removePacketListener(mPacketListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mPacketListener = new PacketListener()
		{
			@Override
			public void processPacket(Packet packet)
			{
				try
				{
					if (packet instanceof Message)
					{// 如果是消息类型
						Message msg = (Message) packet;
						String mediaType = (String) msg.getProperty(ChatConstants.MEDIA_TYPE);
						String chatMessage = msg.getBody();
						Log.i("Message:", "receive a msg!");

						// try to extract a carbon
						Carbon cc = CarbonManager.getCarbon(msg);
						if (cc != null && cc.getDirection() == Carbon.Direction.received)
						{// 收到的消息
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded().getForwardedPacket();
							chatMessage = msg.getBody();
							Log.i("Receive Message:", chatMessage);

							// fall through
						} else if (cc != null && cc.getDirection() == Carbon.Direction.sent)
						{// 如果是自己发送的消息，则添加到数据库后直接返回
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded().getForwardedPacket();
							chatMessage = msg.getBody();
							if (chatMessage == null)
								return;

							Log.i("Send Message:", chatMessage);

							addChatMessageToDB(ChatConstants.OUTGOING, msg, ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(),
									msg.getPacketID());

							return;// 记得要返回
						}

						if (chatMessage == null && !isMultimediaPkt(mediaType))
						{
							return;// 如果消息为空，直接返回了
						}

						if (msg.getType() == Message.Type.error)
						{
							chatMessage = "<Error> " + chatMessage;// 错误的消息类型
						}

						long ts;// 消息时间戳
						DelayInfo timestamp = (DelayInfo) msg.getExtension("delay", "urn:xmpp:delay");
						if (timestamp == null)
							timestamp = (DelayInfo) msg.getExtension("x", "jabber:x:delay");
						if (timestamp != null)
							ts = timestamp.getStamp().getTime();
						else
							ts = System.currentTimeMillis();

						if (isMultimediaPkt(mediaType))
						{
							String mediaUrl = (String) msg.getProperty(ChatConstants.MEDIA_URL);
							if (mediaUrl != null)
								Log.i("receive a media msg", mediaUrl);
							StartReceiveMediaFile(msg, ts, mediaType);
						} else
						{
							addChatMessageToDB(ChatConstants.INCOMING, msg, ChatConstants.DS_NEW, ts, msg.getPacketID());

							String fromJID = getJabberID(msg.getFrom());// 消息来自对象
							mService.newMessage(fromJID, chatMessage);// 通知service，处理是否需要显示通知栏，
						}

					}
				} catch (Exception e)
				{
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, filter);// 这是最关健的了，少了这句，前面的都是白费功夫
	}

	private class DownloadImgThread extends Thread
	{
		private Message mMsg;
		private long mTs;

		public DownloadImgThread(Message msg, long ts)
		{
			mMsg = msg;
			mTs = ts;
		}

		@Override
		public void run()
		{
			try
			{
				String fileUrl = (String) mMsg.getProperty(ChatConstants.MEDIA_URL);
				String dirName = "wanglai_in" + File.separator + "Images" + File.separator;
				String fileName = fileUrl.substring(fileUrl.lastIndexOf(File.separator) + 1);

				int result = HttpDownloader.downloadImage(fileUrl, dirName, fileName);
				if (result == 0)
				{
					Log.i("File path", new FileUtils().getSDPATH() + dirName + fileName);
					mMsg.setProperty(ChatConstants.MEDIA_URL, new FileUtils().getSDPATH() + dirName + fileName);
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_SUCCESS, mTs, mMsg.getPacketID());

					String fromJID = mMsg.getFrom();
					mService.newMessage(fromJID, ChatConstants.MEDIA_TYPE_IMAGE);
				} else
				{
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_FAILED, mTs, mMsg.getPacketID());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private class DownloadFileThread extends Thread
	{
		private Message mMsg;
		private long mTs;
		private String mMediaType;

		public DownloadFileThread(Message msg, long ts, String mediaType)
		{
			mMsg = msg;
			mTs = ts;
			mMediaType = mediaType;
		}

		@Override
		public void run()
		{
			try
			{
				String packetid = mMsg.getPacketID();
				String fileUrl = (String) mMsg.getProperty(ChatConstants.MEDIA_URL);
				String fileName = fileUrl.substring(fileUrl.lastIndexOf(File.separator) + 1);
				String dirName = null;
				if (ChatConstants.MEDIA_TYPE_AUDIO.equals(mMediaType))
				{
					dirName = "wanglai_in" + File.separator + "Audio" + File.separator;
				} else if (ChatConstants.MEDIA_TYPE_FILE.equals(mMediaType))
				{
					dirName = "wanglai_in" + File.separator + "Files" + File.separator;
				}

				int result = HttpDownloader.downloadFile(fileUrl, dirName, fileName);
				if (result == 0)
				{
					changeMessageDeliveryStatus(packetid, ChatConstants.DS_DOWNLOAD_SUCCESS);
					//media_url存放文件在本地的全路径
					mMsg.setProperty(ChatConstants.MEDIA_URL, android.os.Environment.getExternalStorageDirectory() + "/" + dirName + fileName);
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_SUCCESS, mTs, mMsg.getPacketID());
					String fromJID = mMsg.getFrom();
					mService.newMessage(fromJID, mMediaType);
				} else
				{
					changeMessageDeliveryStatus(packetid, ChatConstants.DS_DOWNLOAD_FAILED);
					addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_FAILED, mTs, mMsg.getPacketID());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	protected void StartReceiveMediaFile(Message msg, long ts, String mediaType)
	{
		if (ChatConstants.MEDIA_TYPE_IMAGE.equals(mediaType))
		{
			DownloadImgThread thread = new DownloadImgThread(msg, ts);
			thread.start();
		} else if (ChatConstants.MEDIA_TYPE_AUDIO.equals(mediaType))
		{
			DownloadFileThread thread = new DownloadFileThread(msg, ts, ChatConstants.MEDIA_TYPE_AUDIO);
			thread.start();
		} else if (ChatConstants.MEDIA_TYPE_FILE.equals(mediaType))
		{
			addChatMessageToDB(ChatConstants.INCOMING, msg, ChatConstants.DS_DOWNLOAD_READY, ts, msg.getPacketID());
		//	DownloadFileThread thread = new DownloadFileThread(msg, ts, ChatConstants.MEDIA_TYPE_FILE);
		//	thread.start();
		}
		
	}

	/**
	 * 将消息添加到数据库
	 * 
	 * @param direction
	 *            是否为收到的消息INCOMING为收到，OUTGOING为自己发出
	 * @param message
	 *            消息内容
	 * @param delivery_status
	 *            消息状态 DS_NEW为新消息，DS_SENT_OR_READ为自己发出或者已读的消息
	 * @param ts
	 *            消息时间戳
	 * @param packetID
	 *            服务器为了区分每一条消息生成的消息包的id
	 */

	private void addChatMessageToDB(int direction, Message message, int delivery_status, long ts, String packetID)
	{
		ContentValues values = new ContentValues();

		String jid;
		String mediaUrl = null;
		String mediaSize = null;

		String mediaType = (String) message.getProperty("mediaType");
		if (!ChatConstants.MEDIA_TYPE_NORMAL.equals(mediaType))
		{
			mediaUrl = (String) message.getProperty("mediaUrl");
			mediaSize = (String) message.getProperty("mediaSize");
		}

		if (direction == ChatConstants.OUTGOING)
		{
			jid = getJabberID(message.getTo());
		} else
		{
			jid = getJabberID(message.getFrom());
		}

		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, jid);

		values.put(ChatConstants.MESSAGE, message.getBody());
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.MEDIA_TYPE, mediaType);
		values.put(ChatConstants.MEDIA_URL, mediaUrl);
		values.put(ChatConstants.MEDIA_SIZE, mediaSize);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);

		mContentResolver.insert(ChatProvider.CONTENT_URI, values);
	}

	/************ end 新消息处理 ********************/

	/***************** start 处理消息发送失败状态 ***********************/
	private void registerMessageSendFailureListener()
	{
		// do not register multiple packet listeners
		if (mSendFailureListener != null)
			mXMPPConnection.removePacketSendFailureListener(mSendFailureListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mSendFailureListener = new PacketListener()
		{
			@Override
			public void processPacket(Packet packet)
			{
				try
				{
					if (packet instanceof Message)
					{
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();

						Log.d("SmackableImp",
								"message " + chatMessage + " could not be sent (ID:" + (msg.getPacketID() == null ? "null" : msg.getPacketID()) + ")");
						changeMessageDeliveryStatus(msg.getPacketID(), ChatConstants.DS_NEW);// 当消息发送失败时，将此消息标记为新消息，下次再发送
					}
				} catch (Exception e)
				{
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketSendFailureListener(mSendFailureListener, filter);
	}

	/**
	 * 改变消息状态
	 * 
	 * @param packetID
	 *            消息的id
	 * @param new_status
	 *            新状态类型
	 */
	public void changeMessageDeliveryStatus(String packetID, int new_status)
	{
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID + " = ? AND " + ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
				new String[]
				{ packetID });
	}

	/**
	 * 改变消息状态
	 * 
	 * @param packetID
	 *            消息的id
	 * @param new_status
	 *            新状态类型
	 */
	public void changeMessageDeliveryStatusAndURL(String packetID, int new_status, String newURL)
	{
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		cv.put(ChatConstants.MEDIA_URL, newURL);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID + " = ? AND " + ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
				new String[]
				{ packetID });
	}

	/**
	 * 改变消息状态
	 * 
	 * @param packetID
	 *            消息的id
	 * @param new_status
	 *            新状态类型
	 */
	public void changeUploaingMediaMessageDeliveryStatus(String packetID, int new_status)
	{
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID + " = ? AND " + ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
				new String[]
				{ packetID });
	}

	/***************** end 处理消息发送失败状态 ***********************/

	/***************** start 处理ping服务器消息 ***********************/
	private void registerPongListener()
	{
		// reset ping expectation on new connection
		mPingID = null;// 初始化ping的id

		if (mPongListener != null)
			mXMPPConnection.removePacketListener(mPongListener);// 先移除之前监听对象

		mPongListener = new PacketListener()
		{

			@Override
			public void processPacket(Packet packet)
			{
				if (packet == null)
					return;

				if (packet.getPacketID().equals(mPingID))
				{// 如果服务器返回的消息为ping服务器时的消息，说明没有掉线
					L.i(String.format("Ping: server latency %1.3fs", (System.currentTimeMillis() - mPingTimestamp) / 1000.));
					mPingID = null;
					((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).cancel(mPongTimeoutAlarmPendIntent);// 取消超时闹钟
				}
			}

		};

		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(IQ.class));// 正式开始监听
		mPingAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPingAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);// 定时ping服务器，以此来确定是否掉线
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);// 超时闹钟
		mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(PING_ALARM));// 注册定时ping服务器广播接收者
		mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(PONG_TIMEOUT_ALARM));// 注册连接超时广播接收者
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mPingAlarmPendIntent);// 15分钟ping以此服务器
	}

	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent i)
		{
			L.d("Ping: timeout for " + mPingID);
			mService.postConnectionFailed(XXService.PONG_TIMEOUT);
			logout();// 超时就断开连接
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 * 广播触发，ping服务器
	 */
	private class PingAlarmReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent i)
		{
			if (mXMPPConnection.isAuthenticated())
			{
				sendServerPing();// 收到ping服务器的闹钟，即ping一下服务器
			} else
				L.d("Ping: alarm received, but not connected to server.");
		}
	}

	/***************** end 处理ping服务器消息 ***********************/

	/***************** start 发送离线消息 ***********************/

	public void sendOfflineMessage(Message message)
	{
		long ts = 0;
		String packetID = null;
		int _id = 0;

		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);

		DelayInformation delay = new DelayInformation(new Date(ts));
		message.addExtension(delay);
		message.addExtension(new DelayInfo(delay));
		message.addExtension(new DeliveryReceiptRequest());

		if ((packetID != null) && (packetID.length() > 0))
		{
			message.setPacketID(packetID);
		} else
		{
			packetID = message.getPacketID();
			mark_sent.put(ChatConstants.PACKET_ID, packetID);
		}

		Uri rowUri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS + "/" + _id);
		// 将消息标记为已发送再调用发送，因为，假设此消息又未发送成功，有SendFailListener重新标记消息
		mContentResolver.update(rowUri, mark_sent, null, null);
		mXMPPConnection.sendPacket(message); // must be after marking
												// delivered, otherwise it
												// may override the
												// SendFailListener
	}

	/***************** start 发送离线消息 ***********************/
	public void sendOfflineMessages()
	{
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI, SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);// 查询数据库获取离线消息游标

		final int _ID_COL = cursor.getColumnIndexOrThrow(BaseColumns._ID);
		final int JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int MEDIA_TYPE_COL = cursor.getColumnIndexOrThrow(ChatConstants.MEDIA_TYPE);
		final int MEDIA_URL_COL = cursor.getColumnIndexOrThrow(ChatConstants.MEDIA_URL);
		final int MEDIA_SIZE_COL = cursor.getColumnIndexOrThrow(ChatConstants.MEDIA_SIZE);
		final int PACKETID_COL = cursor.getColumnIndexOrThrow(ChatConstants.PACKET_ID);

		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext())
		{// 遍历之后将离线消息发出
			int _id = cursor.getInt(_ID_COL);
			String toJID = cursor.getString(JID_COL);
			String body = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			long ts = cursor.getLong(TS_COL);
			L.d("sendOfflineMessages: " + toJID + " > " + body);

			Message message = new Message(toJID, Message.Type.chat);
			message.setBody(body);
			String mediaType = cursor.getString(MEDIA_TYPE_COL);
			message.setProperty(ChatConstants.MEDIA_TYPE, cursor.getString(MEDIA_TYPE_COL));
			if (!mediaType.equals("Normal"))
			{
				message.setProperty(ChatConstants.MEDIA_URL, cursor.getString(MEDIA_URL_COL));

				message.setProperty(ChatConstants.MEDIA_SIZE, cursor.getString(MEDIA_SIZE_COL));
			}

			DelayInformation delay = new DelayInformation(new Date(ts));
			message.addExtension(delay);
			message.addExtension(new DelayInfo(delay));
			message.addExtension(new DeliveryReceiptRequest());

			if ((packetID != null) && (packetID.length() > 0))
			{
				message.setPacketID(packetID);
			} else
			{
				packetID = message.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}

			Uri rowUri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/" + PreferenceConstants.TABLE_CHATS + "/" + _id);
			// 将消息标记为已发送再调用发送，因为，假设此消息又未发送成功，有SendFailListener重新标记消息
			mContentResolver.update(rowUri, mark_sent, null, null);
			mXMPPConnection.sendPacket(message); // must be after marking
													// delivered, otherwise it
													// may override the
													// SendFailListener
		}
		cursor.close();
	}

	/**
	 * 作为离线消息存储起来，当自己掉线时调用
	 * 
	 * @param cr
	 * @param toJID
	 * @param message
	 */
	public static void saveAsOfflineMessage(ContentResolver cr, String toJID, String message)
	{
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, toJID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	public static void saveAsOfflineMessage(ContentResolver cr, Message message)
	{

		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, message.getTo());
		values.put(ChatConstants.MESSAGE, message.getBody());
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.MEDIA_TYPE, (String) message.getProperty("mediaType"));
		values.put(ChatConstants.MEDIA_URL, (String) message.getProperty("mediaUrl"));
		values.put(ChatConstants.MEDIA_SIZE, (String) message.getProperty("mediaSize"));
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	public static void saveImageMessageOnUploading(ContentResolver cr, Message message, int ds)
	{
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, message.getTo());
		values.put(ChatConstants.MESSAGE, message.getBody());
		values.put(ChatConstants.DELIVERY_STATUS, ds);
		values.put(ChatConstants.MEDIA_TYPE, (String) message.getProperty("mediaType"));
		values.put(ChatConstants.MEDIA_URL, (String) message.getProperty("mediaUrl"));
		values.put(ChatConstants.MEDIA_SIZE, (String) message.getProperty("mediaSize"));
		values.put(ChatConstants.DATE, System.currentTimeMillis());
		values.put(ChatConstants.PACKET_ID, message.getPacketID());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	/***************** end 发送离线消息 ***********************/

	/******************************* start 联系人数据库事件处理 **********************************/
	private void registerRosterListener()
	{
		mRoster = mXMPPConnection.getRoster();
		mRosterListener = new RosterListener()
		{
			private boolean isFirstGetRoster;

			@Override
			public void presenceChanged(Presence presence)
			{// 联系人状态改变，比如在线或离开、隐身之类
				L.i("presenceChanged(" + presence.getFrom() + "): " + presence);
				String jabberID = getJabberID(presence.getFrom());
				RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				updateRosterEntryInDB(rosterEntry);// 更新联系人数据库
				mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
			}

			@Override
			public void entriesUpdated(Collection<String> entries)
			{// 更新数据库，第一次登陆
				// TODO
				// Auto-generated
				// method
				// stub
				L.i("entriesUpdated(" + entries + ")");
				for (String entry : entries)
				{
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
			}

			@Override
			public void entriesDeleted(Collection<String> entries)
			{// 有好友删除时，
				L.i("entriesDeleted(" + entries + ")");
				for (String entry : entries)
				{
					deleteRosterEntryFromDB(entry);
				}
				mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
			}

			@Override
			public void entriesAdded(Collection<String> entries)
			{// 有人添加好友时，我这里没有弹出对话框确认，直接添加到数据库
				L.i("entriesAdded(" + entries + ")");
				ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				for (String entry : entries)
				{
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
				}
				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				if (isFirstGetRoster)
				{
					isFirstGetRoster = false;
					mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
				}
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}

	private String getJabberID(String from)
	{
		String[] res = from.split("/");
		return res[0].toLowerCase();
	}

	/**
	 * 更新联系人数据库
	 * 
	 * @param entry
	 *            联系人RosterEntry对象
	 */
	private void updateRosterEntryInDB(final RosterEntry entry)
	{
		final ContentValues values = getContentValuesForRosterEntry(entry);

		if (mContentResolver.update(RosterProvider.CONTENT_URI, values, RosterConstants.JID + " = ?", new String[]
		{ entry.getUser() }) == 0)// 如果数据库无此好友
			addRosterEntryToDB(entry);// 则添加到数据库
	}

	/**
	 * 添加到数据库
	 * 
	 * @param entry
	 *            联系人RosterEntry对象
	 */
	private void addRosterEntryToDB(final RosterEntry entry)
	{
		ContentValues values = getContentValuesForRosterEntry(entry);
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		L.i("addRosterEntryToDB: Inserted " + uri);
	}

	/**
	 * 将联系人从数据库中删除
	 * 
	 * @param jabberID
	 */
	private void deleteRosterEntryFromDB(final String jabberID)
	{
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI, RosterConstants.JID + " = ?", new String[]
		{ jabberID });
		L.i("deleteRosterEntryFromDB: Deleted " + count + " entries");
	}

	/**
	 * 将联系人RosterEntry转化成ContentValues，方便存储数据库
	 * 
	 * @param entry
	 * @return
	 */
	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry)
	{
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));

		Presence presence = mRoster.getPresence(entry.getUser());
		values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
		values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));
		values.put(RosterConstants.OWNER, mXMPPConnection.getUser());

		return values;
	}

	/**
	 * 遍历获取组名
	 * 
	 * @param groups
	 * @return
	 */
	private String getGroup(Collection<RosterGroup> groups)
	{
		for (RosterGroup group : groups)
		{
			return group.getName();
		}
		return "";
	}

	/**
	 * 获取联系人名称
	 * 
	 * @param rosterEntry
	 * @return
	 */
	private String getName(RosterEntry rosterEntry)
	{
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0)
		{
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0)
		{
			return name;
		}
		return rosterEntry.getUser();
	}

	/**
	 * 获取状态
	 * 
	 * @param presence
	 * @return
	 */
	private StatusMode getStatus(Presence presence)
	{
		if (presence.getType() == Presence.Type.available)
		{
			if (presence.getMode() != null)
			{
				return StatusMode.valueOf(presence.getMode().name());
			}
			return StatusMode.available;
		}
		return StatusMode.offline;
	}

	private int getStatusInt(final Presence presence)
	{
		return getStatus(presence).ordinal();
	}

	/******************************* end 联系人数据库事件处理 **********************************/

	/**
	 * 与服务器交互消息监听,发送消息需要回执，判断对方是否已读此消息
	 */
	private void initServiceDiscovery()
	{
		// register connection features
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mXMPPConnection);
		if (sdm == null)
			sdm = new ServiceDiscoveryManager(mXMPPConnection);

		sdm.addFeature("http://jabber.org/protocol/disco#info");

		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(10 * 1000);
		// reference DeliveryReceiptManager, add listener

		DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.registerReceiptReceivedListener(new DeliveryReceiptManager.ReceiptReceivedListener()
		{
			@Override
			public void onReceiptReceived(String fromJid, String toJid, String receiptId)
			{
				L.d(SmackImpl.class, "got delivery receipt for " + receiptId);
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);// 标记为对方已读，实际上遇到了点问题，所以其实没有用上此状态
			}
		});
	}

	@Override
	public void setStatusFromConfig()
	{// 设置自己的当前状态，供外部服务调用
		boolean messageCarbons = PreferenceUtils.getPrefBoolean(mService, PreferenceConstants.MESSAGE_CARBONS, true);
		String statusMode = PreferenceUtils.getPrefString(mService, PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		String statusMessage = PreferenceUtils
				.getPrefString(mService, PreferenceConstants.STATUS_MESSAGE, mService.getString(R.string.status_online));
		int priority = PreferenceUtils.getPrefInt(mService, PreferenceConstants.PRIORITY, 0);
		if (messageCarbons)
			CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(true);

		Presence presence = new Presence(Presence.Type.available);
	//	Mode mode = Mode.valueOf(statusMode);
	//	presence.setMode(mode);
	//	presence.setStatus(statusMessage);
	//	presence.setPriority(priority);
		mXMPPConnection.sendPacket(presence);
	}

	@Override
	public boolean isAuthenticated()
	{// 是否与服务器连接上，供本类和外部服务调用
		if (mXMPPConnection != null)
		{
			return (mXMPPConnection.isConnected() && mXMPPConnection.isAuthenticated());
		}
		return false;
	}

	@Override
	public void addRosterItem(String user, String alias, String group) throws XXMPException
	{// 添加联系人，供外部服务调用
		addRosterEntry(user, alias, group);
		
	}

	private void addRosterEntry(String user, String alias, String group) throws XXMPException
	{
		mRoster = mXMPPConnection.getRoster();
		try
		{
			mRoster.createEntry(user, alias, new String[]
			{ group });
		} catch (XMPPException e)
		{
			throw new XXMPException(e.getLocalizedMessage());
		}
	}

	@Override
	public void removeRosterItem(String user) throws XXMPException
	{// 删除联系人，供外部服务调用
		// TODO
		// Auto-generated
		// method
		// stub
		L.d("removeRosterItem(" + user + ")");

		removeRosterEntry(user);
		mService.rosterChanged();
	}

	private void removeRosterEntry(String user) throws XXMPException
	{
		mRoster = mXMPPConnection.getRoster();
		try
		{
			RosterEntry rosterEntry = mRoster.getEntry(user);

			if (rosterEntry != null)
			{
				mRoster.removeEntry(rosterEntry);
			}
		} catch (XMPPException e)
		{
			throw new XXMPException(e.getLocalizedMessage());
		}
	}

	@Override
	public void renameRosterItem(String user, String newName) throws XXMPException
	{// 重命名联系人，供外部服务调用
		// TODO Auto-generated method stub
		mRoster = mXMPPConnection.getRoster();
		RosterEntry rosterEntry = mRoster.getEntry(user);

		if (!(newName.length() > 0) || (rosterEntry == null))
		{
			throw new XXMPException("JabberID to rename is invalid!");
		}
		rosterEntry.setName(newName);
	}

	@Override
	public void moveRosterItemToGroup(String user, String group) throws XXMPException
	{// 移动好友到其他分组，供外部服务调用
		// TODO Auto-generated method stub
		tryToMoveRosterEntryToGroup(user, group);
	}

	private void tryToMoveRosterEntryToGroup(String userName, String groupName) throws XXMPException
	{

		mRoster = mXMPPConnection.getRoster();
		RosterGroup rosterGroup = getRosterGroup(groupName);
		RosterEntry rosterEntry = mRoster.getEntry(userName);

		removeRosterEntryFromGroups(rosterEntry);

		if (groupName.length() == 0)
			return;
		else
		{
			try
			{
				rosterGroup.addEntry(rosterEntry);
			} catch (XMPPException e)
			{
				throw new XXMPException(e.getLocalizedMessage());
			}
		}
	}

	private void removeRosterEntryFromGroups(RosterEntry rosterEntry) throws XXMPException
	{// 从对应组中删除联系人，供外部服务调用
		Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

		for (RosterGroup group : oldGroups)
		{
			tryToRemoveUserFromGroup(group, rosterEntry);
		}
	}

	private void tryToRemoveUserFromGroup(RosterGroup group, RosterEntry rosterEntry) throws XXMPException
	{
		try
		{
			group.removeEntry(rosterEntry);
		} catch (XMPPException e)
		{
			throw new XXMPException(e.getLocalizedMessage());
		}
	}

	private RosterGroup getRosterGroup(String groupName)
	{// 获取联系人分组
		RosterGroup rosterGroup = mRoster.getGroup(groupName);

		// create group if unknown
		if ((groupName.length() > 0) && rosterGroup == null)
		{
			rosterGroup = mRoster.createGroup(groupName);
		}
		return rosterGroup;

	}

	@Override
	public void renameRosterGroup(String group, String newGroup)
	{// 重命名分组
		// TODO
		// Auto-generated
		// method
		// stub
		L.i("oldgroup=" + group + ", newgroup=" + newGroup);
		mRoster = mXMPPConnection.getRoster();
		RosterGroup groupToRename = mRoster.getGroup(group);
		if (groupToRename == null)
		{
			return;
		}
		groupToRename.setName(newGroup);
	}

	@Override
	public void requestAuthorizationForRosterItem(String user)
	{// 重新向对方发出添加好友申请
		// TODO
		// Auto-generated
		// method stub
		Presence response = new Presence(Presence.Type.subscribe);
		response.setTo(user);
		mXMPPConnection.sendPacket(response);
		Log.i("adduser", user);
	}

	@Override
	public void addRosterGroup(String group)
	{// 增加联系人组
		// TODO Auto-generated method
		// stub
		mRoster = mXMPPConnection.getRoster();
		mRoster.createGroup(group);
	}

	public void sendMessage(Message message, int ds, Boolean compress)
	{
		message.addExtension(new DeliveryReceiptRequest());
		String mediaType = (String) message.getProperty("mediaType");
		if (isAuthenticated())
		{
			if (ds == ChatConstants.DS_SENT_OR_READ)
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendChatMessage(message);
			} else if (ds == ChatConstants.DS_UPLOADING && mediaType.equals("Image"))
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendImageMessage(message, compress);
			} else if (ds == ChatConstants.DS_UPLOADING && mediaType.equals("Audio"))
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendAudioMessage(message);
			} else if (ds == ChatConstants.DS_UPLOADING && mediaType.equals("File"))
			{
				addChatMessageToDB(ChatConstants.OUTGOING, message, ds, System.currentTimeMillis(), message.getPacketID());
				sendFileMessage(message);
			}
		} else
		{
			// send offline -> store to DB
			addChatMessageToDB(ChatConstants.OUTGOING, message, ChatConstants.DS_NEW, System.currentTimeMillis(), message.getPacketID());
		}

	}

	private void sendFileMessage(Message message)
	{
		UploadFileThread thread = new UploadFileThread(message);
		thread.start();
	}

	private class UploadFileThread extends Thread
	{
		public static final String REQYEST_URL = REMOTE_HOST + "/AndroidUploadFileWeb/FileUploadServlet";
		private static final String REMOTE_URL_ROOT = REMOTE_HOST + "/AndroidUploadFileWeb/Files/";

		private Message message;

		public UploadFileThread(Message message)
		{
			this.message = message;
		}

		@Override
		public void run()
		{
			try
			{
				String fileUrl = (String) message.getProperty("mediaUrl");

				int result = HttpUploader.uploadFile(fileUrl, REQYEST_URL);
				String packetID = message.getPacketID();
				if (HttpUploader.SUCCESS == result)
				{
					// 标记改消息为上传成功
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_SUCCESS);

					String fileRemoteURL = REMOTE_URL_ROOT + fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
					Log.i("上传文件", fileRemoteURL);
					message.setProperty(ChatConstants.MEDIA_URL, fileRemoteURL);

					sendChatMessage(message);
				} else
				{
					// 标记改消息为上传失败
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_FAILED);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendAudioMessage(Message message)
	{
		UploadFileThread thread = new UploadFileThread(message);
		thread.start();
	}

	/*
	 * private class UploadAudioThread extends Thread{
	 * 
	 * public static final String REQYEST_URL=
	 * "http://192.168.1.100:8080/AndroidUploadFileWeb/FileImageUploadServlet";
	 * 
	 * private Message message;
	 * 
	 * public UploadAudioThread(Message message){ this.message = message; }
	 * 
	 * @Override public void run() { try { String imageUrl = (String)
	 * message.getProperty("mediaUrl");
	 * 
	 * Bitmap bmp = BitmapFactory.decodeFile(imageUrl); File file = new
	 * File(imageUrl); compressBmpToFile(bmp, file);
	 * 
	 * String result = UploadUtils.uploadFile(file, REQYEST_URL); String
	 * packetID = message.getPacketID();
	 * if(UploadUtils.SUCCESS.equalsIgnoreCase(result)){ //标记改消息为上传成功
	 * changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_SUCCESS);
	 * sendChatMessage(message); }else { //标记改消息为上传失败
	 * changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_FAILED); }
	 * } catch (Exception e) { e.printStackTrace(); } }
	 * 
	 * private void compressBmpToFile(Bitmap bmp, File file){
	 * ByteArrayOutputStream baos = new ByteArrayOutputStream(); int options =
	 * 80; bmp.compress(Bitmap.CompressFormat.JPEG, options, baos); while
	 * (baos.toByteArray().length / 1024 > 100) { baos.reset(); options -= 10;
	 * bmp.compress(Bitmap.CompressFormat.JPEG, options, baos); } try {
	 * FileOutputStream fos = new FileOutputStream(file);
	 * fos.write(baos.toByteArray()); fos.flush(); fos.close(); } catch
	 * (Exception e) { e.printStackTrace(); } } }
	 */

	private void sendChatMessage(Message message)
	{
		mXMPPConnection.sendPacket(message);
	}

	private void sendImageMessage(Message message, Boolean compress)
	{
		UploadImgThread thread = new UploadImgThread(message, compress);
		thread.start();
	}

	private class UploadImgThread extends Thread
	{
		public static final String REQYEST_URL = REMOTE_HOST + "/AndroidUploadImageWeb/ImageUploadServlet";
		private static final String REMOTE_URL_ROOT = REMOTE_HOST + "/AndroidUploadImageWeb/Images/";

		private Message message;
		private Boolean compress;

		public UploadImgThread(Message message, Boolean compress)
		{
			this.message = message;
			this.compress = compress;
		}

		@Override
		public void run()
		{
			String packetID = message.getPacketID();
			try
			{
				String fileUrl = (String) message.getProperty("mediaUrl");

				int result = HttpUploader.uploadImage(fileUrl, REQYEST_URL, compress);
				if (HttpUploader.SUCCESS == result)
				{
					// 标记改消息为上传成功
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_SUCCESS);

					int start = fileUrl.lastIndexOf("/");
					String imageRemoteURL = REMOTE_URL_ROOT + fileUrl.substring(start + 1);
					Log.i("mediaUrl", imageRemoteURL);
					message.setProperty(ChatConstants.MEDIA_URL, imageRemoteURL);
					sendChatMessage(message);
				} else
				{
					// 标记改消息为上传失败
					changeMessageDeliveryStatus(packetID, ChatConstants.DS_UPLOAD_FAILED);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void sendServerPing()
	{
		if (mPingID != null)
		{// 此时说明上一次ping服务器还未回应，直接返回，直到连接超时
			L.d("Ping: requested, but still waiting for " + mPingID);
			return; // a ping is still on its way
		}
		Ping ping = new Ping();
		ping.setType(Type.GET);
		ping.setTo(PreferenceUtils.getPrefString(mService, PreferenceConstants.Server, PreferenceConstants.DEFAULT_SERVER));
		mPingID = ping.getPacketID();// 此id其实是随机生成，但是唯一的
		mPingTimestamp = System.currentTimeMillis();
		L.d("Ping: sending ping " + mPingID);
		mXMPPConnection.sendPacket(ping);// 发送ping消息

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + PACKET_TIMEOUT
				+ 3000, mPongTimeoutAlarmPendIntent);// 此时需要启动超时判断的闹钟了，时间间隔为30+3秒
	}

	@Override
	public String getNameForJID(String jid)
	{
		if (null != this.mRoster.getEntry(jid) && null != this.mRoster.getEntry(jid).getName() && this.mRoster.getEntry(jid).getName().length() > 0)
		{
			return this.mRoster.getEntry(jid).getName();
		} else
		{
			return jid;
		}
	}

	@Override
	public boolean logout()
	{// 注销登录
		L.d("unRegisterCallback()");
		// remove callbacks _before_ tossing old connection
		try
		{
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection.removePacketSendFailureListener(mSendFailureListener);
			mXMPPConnection.removePacketListener(mPongListener);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).cancel(mPingAlarmPendIntent);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).cancel(mPongTimeoutAlarmPendIntent);
			mService.unregisterReceiver(mPingAlarmReceiver);
			mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		} catch (Exception e)
		{
			// ignore it!
			return false;
		}
		if (mXMPPConnection.isConnected())
		{
			// work around SMACK's #%&%# blocking disconnect()
			new Thread()
			{
				@Override
				public void run()
				{
					L.d("shutDown thread started");
					mXMPPConnection.disconnect();
					L.d("shutDown thread finished");
				}
			}.start();
		}
		// setStatusOffline();
		this.mService = null;
		return true;
	}

	/**
	 * 将所有联系人标记为离线状态
	 */
	public void setStatusOffline()
	{
		ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}

	/**
	 * 获取头像保存到本地
	 * 
	 */
	public void initAvatar()
	{
		try
		{
			getUserVCard(mXMPPConnection);
		} catch (XMPPException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mRoster = mXMPPConnection.getRoster();
		System.out.println(mRoster.getEntries().size());

		Collection<RosterGroup> entriesGroup = mRoster.getGroups();
		for (RosterGroup group : entriesGroup)
		{
			Collection<RosterEntry> entries = group.getEntries();
			Log.i("---", group.getName());
			for (RosterEntry entry : entries)
			{
				// 获取每个Roster的Avatar信息
				updateAvatarRosterInDB(entry);
				Log.i("---", "user: " + entry.getUser());
				getUserImage(mXMPPConnection, entry.getUser(), false);
				Log.i("---", "type: " + entry.getType());
				Log.i("---", "status: " + entry.getStatus());
				Log.i("---", "groups: " + entry.getGroups());
			}
		}
	}

	/**
	 * 获取用户的vcard信息
	 * 
	 * @param connection
	 * @return VCard
	 * @throws XMPPException
	 */
	private VCard getUserVCard(XMPPConnection connection) throws XMPPException
	{
		final String me = getJabberID(connection.getUser()).split("@")[0]; 
		// 检查头像是否存在
		if (new File("mnt/sdcard/Wanglai/Avatar/" + me + ".png").exists())
		{
			System.out.println("文件已存在");
			return null;
		}
		
		final VCard vcard = new VCard();
		vcard.load(connection);
		
		if (vcard == null || vcard.getAvatar() == null)
		{
			return null;
		}

		File dir = new File("mnt/sdcard/Wanglai/Avatar/");
		// 如果临时文件所在目录不存在，首先创建
		if (!dir.exists())
		{
			if (!createDir("mnt/sdcard/Wanglai/Avatar/"))
			{
				System.out.println("创建临时文件失败，不能创建临时文件所在的目录！");
				return null;
			}
		}
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				FileOutputStream out = null;
				try
				{
					out = new FileOutputStream("mnt/sdcard/Wanglai/Avatar/" + me + ".png");
					out.write(vcard.getAvatar());
					out.close();
				} catch (Exception e)
				{
					e.printStackTrace();
				}

			}
		});
		t.start();

		return vcard;
	}

	/**
	 * 获取用户头像信息
	 */
	public static void getUserImage(XMPPConnection connection, String user, boolean isSub)
	{
		final String userString = user;
		try
		{
			if (!isSub)
			{
				// 检查头像是否存在
				if (new File("mnt/sdcard/Wanglai/Avatar/" + userString + ".png").exists())
				{
					System.out.println("文件已存在");
					return;
				}
			}

			System.out.println("获取用户头像信息: " + user);
			final VCard vcard = new VCard();
			vcard.load(connection, user.substring(0, user.indexOf("@")) + "@" + connection.getServiceName());

			if (vcard == null || vcard.getAvatar() == null)
			{
				return;
			}

			File dir = new File("mnt/sdcard/Wanglai/Avatar/");
			// 如果临时文件所在目录不存在，首先创建
			if (!dir.exists())
			{
				if (!createDir("mnt/sdcard/Wanglai/Avatar/"))
				{
					System.out.println("创建临时文件失败，不能创建临时文件所在的目录！");
					return;
				}
			}

			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					FileOutputStream out = null;
					try
					{
						out = new FileOutputStream("mnt/sdcard/Wanglai/Avatar/" + userString + ".png");
						out.write(vcard.getAvatar());
						out.close();
					} catch (Exception e)
					{
						e.printStackTrace();
					}

				}
			});
			t.start();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 如果保存目录不存在则创建目录
	 * 
	 * @param destDirName
	 * @return
	 */
	public static boolean createDir(String destDirName)
	{
		File dir = new File(destDirName);
		if (dir.exists())
		{
			System.out.println("创建目录" + destDirName + "失败，目标目录已经存在");
			return false;
		}
		if (!destDirName.endsWith(File.separator))
		{
			destDirName = destDirName + File.separator;
		}
		// 创建目录
		if (dir.mkdirs())
		{
			System.out.println("创建目录" + destDirName + "成功！");
			return true;
		} else
		{
			System.out.println("创建目录" + destDirName + "失败！");
			return false;
		}
	}

	private List<AvatarRoster> getRosters(String groupname)
	{
		List<AvatarRoster> childList = new ArrayList<AvatarRoster>();

		String selectWhere = RosterConstants.GROUP + " = ?";
		Cursor cursor = mContentResolver.query(RosterProvider.CONTENT_URI, ROSTER_QUERY, selectWhere, new String[]
		{ groupname }, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			AvatarRoster roster = new AvatarRoster();

			roster.setJid(cursor.getString(cursor.getColumnIndexOrThrow(RosterConstants.JID)));
			roster.setAlias(cursor.getString(cursor.getColumnIndexOrThrow(RosterConstants.ALIAS)));
			childList.add(roster);
			cursor.moveToNext();
		}
		cursor.close();
		return childList;
	}

	private boolean isMultimediaPkt(String mediaType)
	{
		return ChatConstants.MEDIA_TYPE_AUDIO.equals(mediaType) || ChatConstants.MEDIA_TYPE_FILE.equals(mediaType)
				|| ChatConstants.MEDIA_TYPE_IMAGE.equals(mediaType);
	}

	public class AvatarRoster
	{
		private String jid;
		private String alias;

		public String getJid()
		{
			return jid;
		}

		public void setJid(String jid)
		{
			this.jid = jid;
		}

		public String getAlias()
		{
			return alias;
		}

		public void setAlias(String alias)
		{
			this.alias = alias;
		}

	}

	private void registerAvatarListener()
	{
		// do not register multiple packet listeners
		if (mAvatarListener != null)
			mXMPPConnection.removePacketListener(mAvatarListener);

		PacketTypeFilter filter = new PacketTypeFilter(Presence.class);

		mAvatarListener = new PacketListener()
		{
			@Override
			public void processPacket(Packet packet)
			{
				try
				{
					if (packet instanceof Presence)
					{// 如果是消息类型
						Presence msg = (Presence) packet;
						String publisher = msg.getFrom().substring(0, msg.getFrom().indexOf("@"));
						// 过滤掉Presence中非"vcard-temp:x:update"
						if (msg.getExtension("x", "vcard-temp:x:update") == null)
						{
							return;
						}

						String photoHashGet = msg.getExtension("x", "vcard-temp:x:update").toXML();

						L.i(photoHashGet);
						// 联系人状态改变，比如在线或离开、隐身之类
						L.i("presenceChanged(" + publisher + "): " + msg);

						Cursor c = mContentResolver.query(AvatarProvider.CONTENT_URI, AVATAR_QUERY, null, null, null);
						c.moveToFirst();
						while (!c.isAfterLast())
						{
							String name = c.getString(c.getColumnIndex(RosterConstants.JID)).substring(0, c.getString(c.getColumnIndex(RosterConstants.JID)).indexOf("@"));
							if (name.equals(publisher))
							{
								System.out.println("获取的photohash：" + photoHashGet);
								System.out.println("数据库中的photohash：" + c.getString(c.getColumnIndex(AvatarConstants.PHOTO_HASH)));
								if (c.getString(c.getColumnIndex(AvatarConstants.PHOTO_HASH)) == null
										|| !(c.getString(c.getColumnIndex(AvatarConstants.PHOTO_HASH)).equals(photoHashGet)))
								{
									String jabberID = getJabberID(msg.getFrom());
									getUserImage(mXMPPConnection, jabberID, true);
									System.out.println(jabberID);
									RosterEntry rosterEntry = mRoster.getEntry(jabberID);
									updateAvatarInDB(rosterEntry, photoHashGet);// 更新联系人数据库
								}
							}
							c.moveToNext();
						}
						c.close();
					}
				} catch (Exception e)
				{
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mAvatarListener, filter);// 这是最关健的了，少了这句，前面的都是白费功夫
	}

	private void updateAvatarInDB(RosterEntry rosterEntry, String photoHashGet)
	{
		final ContentValues values = new ContentValues();
		values.put(AvatarConstants.PHOTO_HASH, photoHashGet);
		mContentResolver.update(AvatarProvider.CONTENT_URI, values, RosterConstants.JID + " = ?", new String[]
		{ rosterEntry.getUser() });
		Log.i("Avatar Update", "OK");
	}

	private void updateAvatarRosterInDB(RosterEntry rosterEntry)
	{
		// TODO Auto-generated method stub
		final ContentValues values = getContentValuesForAvatar(rosterEntry);
		if (mContentResolver.update(AvatarProvider.CONTENT_URI, values, AvatarConstants.JID + " = ?", new String[]
		{ rosterEntry.getUser() }) == 0)// 如果数据库无此好友
			addAvatarRosterToDB(rosterEntry);// 则添加到数据库
	}

	private void addAvatarRosterToDB(RosterEntry rosterEntry)
	{
		// TODO Auto-generated method stub
		ContentValues values = getContentValuesForAvatar(rosterEntry);
		Uri uri = mContentResolver.insert(AvatarProvider.CONTENT_URI, values);
		L.i("addAvatarToDB: Inserted " + uri);

	}

	private ContentValues getContentValuesForAvatar(final RosterEntry entry)
	{
		final ContentValues values = new ContentValues();

		values.put(AvatarConstants.JID, entry.getUser());
		values.put(AvatarConstants.ALIAS, getName(entry));
		return values;
	}

	/**
	 * 修改用户头像
	 * 
	 * @param image
	 * 
	 * @throws XMPPException
	 * 
	 * @throws IOException
	 */
	public boolean setUserImage(final byte[] image) throws XMPPException
	{
		final VCard card = new VCard();
		card.load(mXMPPConnection);
		boolean state = false;
		try
		{
			PacketFilter filter = new AndFilter(new PacketIDFilter(card.getPacketID()), new PacketTypeFilter(IQ.class));
			PacketCollector collector = mXMPPConnection.createPacketCollector(filter);
			String encodeImage = StringUtils.encodeBase64(image);
			card.setAvatar(image, encodeImage);
			card.setEncodedImage(encodeImage);
			card.setField("PHOTO", "<TYPE>image/jpg</TYPE><BINVAL>" + encodeImage + "</BINVAL>", true);
			Log.i("other", "上传头像的方法！");
			card.save(mXMPPConnection);
			IQ iq = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
			if (iq != null && iq.getType() == IQ.Type.RESULT)
			{
				state = true;
			}
			else 
			{
				state = false;
			}
		} catch (XMPPException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return state;
	}
	
	/**
	 * 添加好友中,手机通讯录联系人存入本地数据库，登录时检查联系人是否更新
	 */
	@SuppressLint("NewApi")
	public void getPhoneContacts() {

		Cursor phoneCursor = resolver.query(Phone.CONTENT_URI,
				PHONES_PROJECTION, null, null, null);
		ArrayList <String>repeatNum = new ArrayList<String>();
		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {
				boolean state = true;
				 for (int i=0,lsize=repeatNum.size();i<lsize;i++)
				 { 
					 if(repeatNum.get(i).equals(phoneCursor.getPosition()+""))
						{
						 Log.i("ddddddddddddd",phoneCursor.getPosition()+"");
						 state = false;
						}
				 } 
				 if(!state)
				 {
					 continue;
				 }
				//联系人号码规范化
				String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX).replaceAll("-", "").replaceAll("^(\\+86)", "").trim();;
				if (TextUtils.isEmpty(phoneNumber))
					continue;
				
				String contactName = phoneCursor
						.getString(PHONES_DISPLAY_NAME_INDEX);
				//保存当前查询指针状态
				int position = phoneCursor.getPosition();
				
				Log.i("dsdsdsd",position+"");
				while (phoneCursor.moveToNext())
				{
					String number = phoneCursor.getString(PHONES_NUMBER_INDEX);
					if (TextUtils.isEmpty(number))
						continue;
					
					if(phoneNumber.equals(number))
					{
						int positionrepeat = phoneCursor.getPosition();
						repeatNum.add(positionrepeat+"");
						Log.i("www",phoneNumber);
						String otherContactName = phoneCursor
								.getString(PHONES_DISPLAY_NAME_INDEX);
						contactName = contactName + " 或  " + otherContactName;
					}
				}
				//恢复查询指针
				phoneCursor.moveToPosition(position);
				
			//	Long contactid = phoneCursor.getLong(PHONES_CONTACT_ID_INDEX);

//				Long photoid = phoneCursor.getLong(PHONES_PHOTO_ID_INDEX);
//
//				Bitmap contactPhoto = null;
//
//				if (photoid > 0) {
//					Uri uri = ContentUris.withAppendedId(
//							ContactsContract.Contacts.CONTENT_URI, contactid);
//					InputStream input = ContactsContract.Contacts
//							.openContactPhotoInputStream(resolver, uri);
//					contactPhoto = BitmapFactory.decodeStream(input);
//				} else {
//					contactPhoto = BitmapFactory.decodeResource(getResources(),
//							R.drawable.default_mobile_avatar);
//				}
				
				//判断通讯录里的号码是否已经被添加，存入数据库
				String status = "need to add";
				mRoster = mXMPPConnection.getRoster();
				Collection<RosterGroup> entriesGroup = mRoster.getGroups();
				for (RosterGroup group : entriesGroup)
				{
					Collection<RosterEntry> entries = group.getEntries();
					for (RosterEntry entry : entries)
					{
						if(phoneNumber.equals(entry.getName()))
						{
							status = "added";
						}
					}
				}
				updateConstantsInDB(contactName, phoneNumber, status);
			}
			phoneCursor.close();
		}
	}
	
	private void updateConstantsInDB(String Name, String phoneNum, String status)
	{
		final ContentValues values = getContentValuesForPhones(Name, phoneNum, status);
		if (mContentResolver.update(AddPhonesProvider.CONTENT_URI, values, PhoneConstants.PHONE_NUM + " = ?", new String[]
		{ phoneNum }) == 0)// 如果数据库无此号码
			addPhoneToDB(Name, phoneNum, status);// 则添加到数据库
	}

	private void addPhoneToDB(String Name, String phoneNum, String status)
	{
		ContentValues values = getContentValuesForPhones(Name, phoneNum, status);
		Uri uri = mContentResolver.insert(AddPhonesProvider.CONTENT_URI, values);
		L.i("addPhoneToDB: Inserted " + uri);

	}

	private ContentValues getContentValuesForPhones(String Name, String phoneNum, String status)
	{
		final ContentValues values = new ContentValues();
		
		values.put(PhoneConstants.PHONE_NUM, phoneNum);
		values.put(PhoneConstants.NAME, Name);
		values.put(PhoneConstants.STATUS, status);
		return values;
	}

	/**
	 * 新朋友申请加入入数据库
	 */
	private void registerAddLinster()
	{
		// do not register multiple packet listeners
		if (mAddListener != null)
					mXMPPConnection.removePacketListener(mAddListener);
		 Log.i("Presence", "PresenceService-----" + (mXMPPConnection == null));  
	        if (mXMPPConnection != null && mXMPPConnection.isConnected()  
	                && mXMPPConnection.isAuthenticated()) {//已经认证的情况下，才能正确收到Presence包（也就是登陆）  
	            final String loginuser = mXMPPConnection.getUser().substring(0,  
	            		mXMPPConnection.getUser().lastIndexOf("@"));  
	            //理解为条件过滤器   过滤出Presence包  
	            PacketFilter filter = new AndFilter(new PacketTypeFilter(  
	                    Presence.class));  
	            mAddListener = new PacketListener() {  
	                @Override  
	                public void processPacket(Packet packet) {  
	                    Log.i("Presence", "PresenceService------" + packet.toXML());  
	                    //看API可知道   Presence是Packet的子类  
	                    if (packet instanceof Presence) {  
	                        Log.i("Presence", packet.toXML());  
	                        Presence presence = (Presence) packet;  
	                        //Presence还有很多方法，可查看API   
	                        String from = presence.getFrom();//发送方  
	                        String to = presence.getTo();//接收方  
	                        //Presence.Type有7中状态  
	                        if (presence.getType().equals(Presence.Type.subscribe)) {//好友申请  
	                        	Log.i("subscribe","OK");
	                        	updateNewFriendsInDB(from, to, "subscribe");
	                              
	                        } else if (presence.getType().equals(  
	                                Presence.Type.subscribed)) {//同意添加好友  
	                        	Log.i("subscribed","OK");
	                              
	                        } else if (presence.getType().equals(  
	                                Presence.Type.unsubscribe)) {//拒绝添加好友  和  删除好友  
	                        	Log.i("unsubscribe","OK");
	                              
	                        } else if (presence.getType().equals(  
	                                Presence.Type.unsubscribed)) {//这个我没用到  
	                        } else if (presence.getType().equals(  
	                                Presence.Type.unavailable)) {//好友下线   要更新好友列表，可以在这收到包后，发广播到指定页面   更新列表  
	                              
	                        } else {//好友上线  
	                              
	                        }  
	                    }  
	                }  
	            };  
	            mXMPPConnection.addPacketListener(mAddListener, filter);  
	        }  
	}
	
	private void updateNewFriendsInDB(String Name, String JID, String status)
	{
		final ContentValues values = getContentValuesForNewFriends(Name, JID, status);
		if (mContentResolver.update(NewFriendsProvider.CONTENT_URI, values, NewFriendsConstants.JID + " = ?", new String[]
		{ JID }) == 0)// 如果数据库无此JID
			addNewFriendsToDB(Name, JID, status);// 则添加到数据库
	}

	private void addNewFriendsToDB(String Name, String JID, String status)
	{
		ContentValues values = getContentValuesForNewFriends(Name, JID, status);
		Uri uri = mContentResolver.insert(NewFriendsProvider.CONTENT_URI, values);
		L.i("addNewFriendsToDB: Inserted " + uri);
	}

	private ContentValues getContentValuesForNewFriends(String Name, String JID, String status)
	{
		final ContentValues values = new ContentValues();
		
		values.put(NewFriendsConstants.JID, JID);
		values.put(NewFriendsConstants.NAME, Name);
		values.put(NewFriendsConstants.STATUS, status);
		return values;
	}

	public void sendPacket(Presence response)
	{
		mXMPPConnection.sendPacket(response);
	}
	
}
