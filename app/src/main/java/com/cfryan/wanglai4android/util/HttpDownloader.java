package com.cfryan.wanglai4android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class HttpDownloader {
	
	private URL url = null;

	/**
	 * 根据URL下载文件,前提是这个文件当中的内容是文本,函数的返回值就是文本当中的内容
	 * 1.创建一个URL对象
	 * 2.通过URL对象,创建一个HttpURLConnection对象
	 * 3.得到InputStream
	 * 4.从InputStream当中读取数据
	 * @param fileUrl
	 * @return
	 */
	public String download(String fileUrl){
		StringBuffer sb = new StringBuffer();
		String line = null;
		BufferedReader buffer = null;
		try {
			url = new URL(fileUrl);
			HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
			buffer = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			while( (line = buffer.readLine()) != null){
				sb.append(line);
			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			try {
				buffer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param fileUrl
	 * @param path
	 * @param fileName
	 * @return 
	 * 		-1:�ļ����س���
	 * 		 0:�ļ����سɹ�
	 * 		 1:�ļ��Ѿ�����
	 * @throws IOException 
	 */
	public static int downloadFile(String fileUrl, String dir, String fileName) throws IOException{
		
		InputStream inputStream = null;
	      
		try {
			FileUtils fileUtils = new FileUtils();
			if(!new File(new FileUtils().getSDPATH() + dir).exists())
			{
				fileUtils.createSDDir(new FileUtils().getSDPATH() + dir);
			}
			
			if(fileUtils.isFileExist(dir + fileName)){
				return 1;
			} else {
				
				inputStream = getInputStreamFromURL(fileUrl);
				
				//inputstream->file
				File file = fileUtils.createSDFile(dir + fileName);
				OutputStream os = new FileOutputStream(file);
				int bytesRead = 0;
				byte[] buffer = new byte[8192];
				while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) 
				{
				    os.write(buffer, 0, bytesRead);
				}
				os.close();
				inputStream.close();
				    
//				File resultFile = 
//					fileUtils.writeToSDFromInputStream(dir, fileName, inputStream);
//				if(resultFile == null){
//					return -1;
//				}
			       	
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		finally{
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	
	public static int downloadImage(String fileUrl, String dir, String fileName){
		try {
			FileUtils fileUtils = new FileUtils();
			if(!new File(new FileUtils().getSDPATH() + dir).exists())
			{
				fileUtils.createSDDir(new FileUtils().getSDPATH() + dir);
			}
			if(fileUtils.isFileExist(dir + fileName)){
				return 1;
			} else {
				Bitmap bitmap = getImage(fileUrl);
				if(bitmap == null){
					Log.i("Download Image", "failed");
					return -1;
				}
				Log.i("Download Image", "success");
				fileUtils.writeToSDFromBmp(dir, fileName, bitmap);
				Log.i("Write Image", "success");
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	/**
	 * ����URL�õ�������
	 * @param urlStr
	 * @return
	 */
	private static InputStream getInputStreamFromURL(String fileUrl) {
		try {
			URL url = new URL(fileUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setRequestMethod("GET");
			if (conn.getResponseCode() == 200) {
				InputStream inputStream = conn.getInputStream();
				return inputStream;
			}			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static Bitmap getImage(String fileUrl) throws Exception {
		InputStream is = getInputStreamFromURL(fileUrl);
		if(is != null) {
			Bitmap bitmap = BitmapFactory.decodeStream(is);
			return bitmap;
		}
		return null;
	}
}