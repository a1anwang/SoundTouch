/////////////////////////////////////////////////////////////////////////////
///
/// Example Android Application/Activity that allows processing WAV 
/// audio files with SoundTouch library
///
/// Copyright (c) Olli Parviainen
///
////////////////////////////////////////////////////////////////////////////////

package com.a1anwang.soundtouchdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;

import net.surina.soundtouch.SoundTouch;

import java.io.File;
import java.util.List;

public class ExampleActivity extends Activity implements OnClickListener
{
	private String Key_LastSourcePath="LastSourcePath";
	TextView textViewConsole = null;
	TextView editSourceFile = null;
	TextView editOutputFile = null;
	EditText editTempo = null;
	EditText editPitch = null;
	EditText editSpeed = null;
	CheckBox checkBoxPlay = null;
	
	StringBuilder consoleText = new StringBuilder();

	MyMusicPlayer myMusicPlayer;
	/// Called when the activity is created
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_example);
		myMusicPlayer=new MyMusicPlayer();
		myMusicPlayer.autoRepeat=false;
		textViewConsole = (TextView)findViewById(R.id.textViewResult);
		editSourceFile = findViewById(R.id.editTextSrcFileName);
		editOutputFile = findViewById(R.id.editTextOutFileName);
		editOutputFile.setText(Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"soundtouch.wav");

		editTempo = (EditText)findViewById(R.id.editTextTempo);
		editPitch = (EditText)findViewById(R.id.editTextPitch);
		editSpeed= (EditText)findViewById(R.id.editTextSpeed);
		Button buttonFileSrc = (Button)findViewById(R.id.buttonSelectSrcFile);
		Button buttonFileOutput = (Button)findViewById(R.id.buttonSelectOutFile);
		Button buttonProcess = (Button)findViewById(R.id.buttonProcess);
		buttonFileSrc.setOnClickListener(this);
		buttonFileOutput.setOnClickListener(this);
		buttonProcess.setOnClickListener(this);

		checkBoxPlay = (CheckBox)findViewById(R.id.checkBoxPlay);

		// Check soundtouch library presence & version
		checkLibVersion();

		String lastSourcePath= SPUtils.getInstance().getString(Key_LastSourcePath);
		if(!StringUtils.isEmpty(lastSourcePath)){
			editSourceFile.setText(lastSourcePath);
			sourceFile=new File(lastSourcePath);
		}
		requestPermission();
	}
	
	private void requestPermission(){
		PermissionUtils.permission(PermissionConstants.STORAGE).callback(new PermissionUtils.FullCallback() {
			@Override
			public void onGranted(List<String> permissionsGranted) {

				LogUtils.e(" onGranted  ");
			}

			@Override
			public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
				//权限被禁止
				if (!permissionsDeniedForever.isEmpty()) {
					//永久禁止

					ToastUtils.showLong("请授予存储权限");
					PermissionUtils.launchAppDetailsSettings();
				} else {
					//禁止

					requestPermission();
				}
			}
		}).request();
	}
		
	/// Function to append status text onto "console box" on the Activity
	public void appendToConsole(final String text)
	{
		// run on UI thread to avoid conflicts
		runOnUiThread(new Runnable()
		{
		    public void run() 
		    {
				consoleText.append(text);
				consoleText.append("\n");
				textViewConsole.setText(consoleText);
		    }
		});
	}
	

	
	/// print SoundTouch native library version onto console
	protected void checkLibVersion()
	{
		String ver = SoundTouch.getVersionString();
		appendToConsole("SoundTouch native library version = " + ver);
	}



	/// Button click handler
	@Override
	public void onClick(View arg0)
	{
		switch (arg0.getId())
		{
			case R.id.buttonSelectSrcFile:
				//选择要变音的wav文件
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("audio/*");
				this.startActivityForResult(intent, 1);
			case R.id.buttonSelectOutFile:
				// one of the file select buttons clicked ... we've not just implemented them ;-)
				//Toast.makeText(this, "File selector not implemented, sorry! Enter the file path manually ;-)", Toast.LENGTH_LONG).show();
				break;
				
			case R.id.buttonProcess:
				// button "process" pushed
				if(sourceFile==null){
					appendToConsole("请选择wav文件");
					return;
				}
				process();
				break;						
		}
		
	}
	File sourceFile;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==1&&resultCode== Activity.RESULT_OK){
			if(data!=null){
				Uri uri= data.getData();
				if(uri!=null){
					sourceFile= UriUtils.uri2File(uri);
					Log.e("A","文件路径："+ UriUtils.uri2File(uri));
					if(sourceFile!=null){

						editSourceFile.setText(sourceFile.getAbsolutePath());
						SPUtils.getInstance().put(Key_LastSourcePath,sourceFile.getAbsolutePath());
					}

				}
			}
		}
	}

	/// Play audio file
	protected void playWavFile(String fileName)
	{
//		myMusicPlayer.startPlayFilePath(fileName);
		File file2play = new File(fileName);
		Intent i = new Intent();
		i.setAction(android.content.Intent.ACTION_VIEW);
		i.setDataAndType(UriUtils.file2Uri(file2play), "audio/wav");
		i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(i);
	}
	
				

	/// Helper class that will execute the SoundTouch processing. As the processing may take
	/// some time, run it in background thread to avoid hanging of the UI.
	protected class ProcessTask extends AsyncTask<ProcessTask.Parameters, Integer, Long>
	{
		/// Helper class to store the SoundTouch file processing parameters
		public final class Parameters
		{
			String inFileName;
			String outFileName;
			float tempo;
			float pitch;
			float speed;
		}

		
		
		/// Function that does the SoundTouch processing
		public final long doSoundTouchProcessing(Parameters params) 
		{
			
			SoundTouch st = new SoundTouch();
			st.setTempo(params.tempo);
			st.setPitchSemiTones(params.pitch);
			st.setSpeed(params.speed);
			Log.i("SoundTouch", "process file " + params.inFileName);
			long startTime = System.currentTimeMillis();
			int res = st.processFile(params.inFileName, params.outFileName);
			long endTime = System.currentTimeMillis();
			float duration = (endTime - startTime) * 0.001f;
			
			Log.i("SoundTouch", "process file done, duration = " + duration);
			appendToConsole("Processing done, duration " + duration + " sec.");
			if (res != 0)
			{
				String err = SoundTouch.getErrorString();
				appendToConsole("Failure: " + err);
				return -1L;
			}
			FileUtils.notifySystemToScan(params.outFileName);
			// Play file if so is desirable
			if (checkBoxPlay.isChecked())
			{
				playWavFile(params.outFileName);
			}
			return 0L;
		}


		
		/// Overloaded function that get called by the system to perform the background processing
		@Override
		protected Long doInBackground(Parameters... aparams)
		{
			return doSoundTouchProcessing(aparams[0]);
		}
		
	}


	/// process a file with SoundTouch. Do the processing using a background processing
	/// task to avoid hanging of the UI
	protected void process()
	{
		try 
		{
			ProcessTask task = new ProcessTask();
			ProcessTask.Parameters params = task.new Parameters();
			// parse processing parameters
			params.inFileName = editSourceFile.getText().toString();
			params.outFileName = editOutputFile.getText().toString();
			params.tempo = 0.01f * Float.parseFloat(editTempo.getText().toString());
			params.pitch = Float.parseFloat(editPitch.getText().toString());
			params.speed = Float.parseFloat(editSpeed.getText().toString());
			// update UI about status
			appendToConsole("Process audio file :" + params.inFileName +" => " + params.outFileName);
			appendToConsole("Tempo = " + params.tempo);
			appendToConsole("Pitch adjust = " + params.pitch);
			appendToConsole("Speed = " + params.speed);
			Toast.makeText(this, "Starting to process file " + params.inFileName + "...", Toast.LENGTH_SHORT).show();

			// start SoundTouch processing in a background thread
			task.execute(params);
//			task.doSoundTouchProcessing(params);	// this would run processing in main thread
			
		}
		catch (Exception exp)
		{
			exp.printStackTrace();
		}
	
	}
}