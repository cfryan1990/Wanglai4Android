package com.cfryan.wanglai4android.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	private String SDROOT;

	private int BUFFSIZE = 4 * 1024;

	public String getSDPATH(){
		return SDROOT;
	}

	public FileUtils(){
		//得到当前外部存储设备的目录( /SDCARD )
		SDROOT = Environment.getExternalStorageDirectory() + "/";
	}

	/**
	 * 在SD卡上创建文件
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public File createSDFile(String fileName) throws IOException{
		File file = new File(SDROOT + fileName);
		Log.i("createSDFile",SDROOT + fileName);
		file.createNewFile();
		return file;
	}

	/**
	 * 如果保存目录不存在则创建目录
	 *
	 * @param destDirName
	 * @return
	 */
	public boolean createSDDir(String destDirName)
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

	/**
	 * 判断SD卡上的文件夹是否存在
	 * @param fileName
	 * @return
	 */
	public boolean isFileExist(String fileName){
		File file = new File(SDROOT + fileName);
		return file.exists();
	}

	/**
	 * 将一个InputStream里面的数据写入到SD卡中
	 * @param dirName
	 * @param fileName
	 * @param is
	 * @return
	 */
	public File writeToSDFromInputStream(String dirName,String fileName, InputStream is){

		File file = null;
		OutputStream os = null;
		try {
			file = createSDFile(dirName + fileName);
			os = new FileOutputStream(file);

			byte[] buffer = new byte[BUFFSIZE];
			while((is.read(buffer)) > 0){
				os.write(buffer);
			}

			os.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}


	/**
	 * 将一个Bitmap里面的数据写入到SD卡中
	 * @param dirName
	 * @param fileName
	 * @param bmp
	 * @return
	 */
	public void writeToSDFromBmp(String dirName, String fileName, Bitmap bmp) {

		try {
			//	Log.i("writeToSDFromBmp",dirName);
			//	createSDDir(dirName);
			File file = createSDFile(dirName + fileName);
			compressBmpToFile(bmp, file, false);
		} catch (FileNotFoundException e) {
			Log.e("writeToSDFromBmp", "FileNotFoundException failed");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("writeToSDFromBmp", "IOException failed");
			e.printStackTrace();
		}

	}

	public void compressBmpToFile(Bitmap bmp, File file, boolean compress) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int options;
		if(compress){
			options = 80;
			bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
			while (baos.toByteArray().length / 1024 > 100) {
				baos.reset();
				options -= 10;
				bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
			}
		}else{
			bmp.compress(Bitmap.CompressFormat.JPEG, 1, baos);
		}
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baos.toByteArray());
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
