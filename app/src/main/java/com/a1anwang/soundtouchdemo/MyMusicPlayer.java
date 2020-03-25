package com.a1anwang.soundtouchdemo;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * Created by a1anwang.com on 2019/12/7.
 */
public class MyMusicPlayer {
    private String TAG="MyMusicPlayer";
    private MediaPlayer mediaPlayer;

    private String musicURL;
    private String musicFilePath;

    boolean autoRepeat=true;

    public void setAutoRepeat(boolean autoRepeat) {
        this.autoRepeat = autoRepeat;
    }

    public static final int PlayerState_Initial=0;
    public static final int PlayerState_Preparing=1;
    public static final int PlayerState_Playing=2;
    public static final int PlayerState_Paused=3;
    public static final int PlayerState_Stopped=4;
    public static final int PlayerState_Seeking=5;
    private int state=PlayerState_Initial;


    public int getState() {
        return state;
    }

    public MyMusicPlayer(){
        mediaPlayer=new MediaPlayer();
        mediaPlayer.setOnPreparedListener(preparedListener);
        mediaPlayer.setOnSeekCompleteListener(seekCompleteListener);
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }


    private boolean preparedComplete=false;


    boolean needPlayWhenPrepared;



    public void start(){
        needPlayWhenPrepared=true;
       // ViseLog.e(" ---- start preparedComplete:"+preparedComplete);
        if(preparedComplete){
            mediaPlayer.start();
            setState(PlayerState_Playing);
        }
    }

    public void reStart(){
        mediaPlayer.stop();
        start();
    }

    public void startPlayFilePath(String path){
        stop();
        this.musicFilePath=path;
        preparedComplete=false;
        needPlayWhenPrepared=true;
        try {

            mediaPlayer.setDataSource(path);
            // 准备播放（异步）
            setState(PlayerState_Preparing);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void pause(){
        mediaPlayer.pause();
        setState(PlayerState_Paused);
    }

    public void resume(){
        mediaPlayer.start();
        setState(PlayerState_Playing);
    }

    public void stop(){
        seekToTime=0;
        mediaPlayer.stop();
        mediaPlayer.reset();
        setState(PlayerState_Stopped);
    }

    private int seekToTime=0;
    public void seekToPlaying(int million){

        if(preparedComplete){
            //加载好了
            mediaPlayer.seekTo(million);
            setState(PlayerState_Seeking);
        }else{
            //还没加载好
            seekToTime=million;
        }
    }

    private float lastVolume=1;
    public void setVolume(float volume) {
        this.lastVolume=volume;
        if(mediaPlayer!=null){
            mediaPlayer.setVolume(volume,volume);
        }
    }

    public float getLastVolume() {
        return lastVolume;
    }

    public void mute() {
        if(mediaPlayer!=null){
            mediaPlayer.setVolume(0,0);
        }
    }

    private void setState(int state){
        this.state=state;
        //ViseLog.e("setState:"+state);
    }

    public int getCurrentPosition(){
        if(mediaPlayer!=null){
            return mediaPlayer.getCurrentPosition();
        }
        return -1;
    }

    public int getDuration() {
        if(mediaPlayer!=null&&preparedComplete){
            return mediaPlayer.getDuration();
        }
        return 0;
    }
    public void destory(){
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
        }

    }




    private MediaPlayer.OnPreparedListener preparedListener=new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            preparedComplete=true;
            Log.e(TAG,"--- mediaPlayer 准备结束，开始播放 "+musicURL);
            int duration= mediaPlayer.getDuration();
            Log.e(TAG," --- duration2: "+duration +" needPlayWhenPrepared:"+needPlayWhenPrepared);
            if(needPlayWhenPrepared){
                mediaPlayer.start();
                setState(PlayerState_Playing);
                seekToPlaying(seekToTime);
            }


        }
    };




    private MediaPlayer.OnSeekCompleteListener seekCompleteListener= new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mp) {

            if(!mediaPlayer.isPlaying()){
                mediaPlayer.start();
            }
            setState(PlayerState_Playing);
        }
    };

    private MediaPlayer.OnCompletionListener onCompletionListener= new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if(autoRepeat){
                //继续播放
                if(state==PlayerState_Playing){
                    seekToPlaying(seekToTime);
                }
            }
            if(state==PlayerState_Playing){

                setState(PlayerState_Stopped);
            }
        }
    };


}
