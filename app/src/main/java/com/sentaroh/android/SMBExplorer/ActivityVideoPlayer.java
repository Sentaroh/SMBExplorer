package com.sentaroh.android.SMBExplorer;
/*
The MIT License (MIT)
Copyright (c) 2011-2019 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.sentaroh.android.SMBExplorer.Log.LogUtil;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

public class ActivityVideoPlayer extends FragmentActivity {

    private int mRestartStatus=0;
    private Context mContext=null;
    
    @SuppressWarnings("unused")
	private boolean mApplicationTerminated=false;

    private GlobalParameters mGp=null;
    
    private Handler mUiHandler=null;
    
    private LogUtil mLog=null;
//    private CustomContextMenu mCcMenu=null;
    private CommonDialog mCommonDlg=null;

	private SurfaceView mSurfaceView=null;
	private SurfaceHolder mSurfaceHolder=null;
//	private Activity mActivity=null;

	private final static int VIDEO_STATUS_STOPPED=0;
	private final static int VIDEO_STATUS_PLAYING=1;
	private final static int VIDEO_STATUS_PAUSING=2;
	private int mVideoPlayerStatus=VIDEO_STATUS_STOPPED;
	
	private boolean mIsVideoReadyToBePlayed=true;
	private boolean mIsPlayRequiredAfterMoveFrame=false;
	
	private SeekBar mSbPlayPosition=null;
	private TextView mTvPlayPosition=null;
	private TextView mTvEndPosition=null;
	private ImageButton mIbPlay=null;
	private ImageButton mIbShare=null;
	private NonWordwrapTextView mTvTitle=null;
	private ImageButton mIbCapture=null;
	private ImageButton mIbForward=null, mIbBackward=null;
//	private LinearLayout mLayoutTop=null;
//	private LinearLayout mLayoutBottom=null;
    private RelativeLayout mOperationView=null;


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	};  

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mRestartStatus=2;
	};

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    mLog.addDebugMsg(1,"I","onConfigurationChanged Entered, orientation="+newConfig.orientation);
	    Point dsz=new Point();
	    getWindowManager().getDefaultDisplay().getSize(dsz);
//	    Log.v("","x="+dsz.x+", y="+dsz.y);
	    
	    if (!isVideoPlayerStatusStopped()) {
			if (mGp.settingsVideoPlaybackKeepAspectRatio) {
			    float video_Width = mMediaPlayer.getVideoWidth();
			    float video_Height = mMediaPlayer.getVideoHeight();
			    float ratio_width = dsz.x/video_Width;
			    float ratio_height = dsz.y/video_Height;
			    float aspectratio = video_Width/video_Height;
			    android.view.ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
			    if (ratio_width > ratio_height){
				    layoutParams.width = (int) (dsz.y * aspectratio);
				    layoutParams.height = dsz.y;
			    }else{
			    	layoutParams.width = dsz.x;
			    	layoutParams.height = (int) (dsz.x / aspectratio);
			    }
			    mSurfaceView.setLayoutParams(layoutParams);
			}
	    }
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.video_player);
        mContext=this.getApplicationContext();
        mUiHandler=new Handler();
        mGp=GlobalWorkArea.getGlobalParameters(mContext);

        mLog=new LogUtil(mContext, "VideoPlayer");
        
        mLog.addDebugMsg(1, "I","onCreate entered");
        
//        mCcMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());
        mCommonDlg=new CommonDialog(mContext, getSupportFragmentManager());

//        if (mGp.settingsDeviceOrientationPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mTcPlayer=new ThreadCtrl();
        mMediaPlayer = new MediaPlayer();
        
		mSbPlayPosition=(SeekBar)findViewById(R.id.video_player_dlg_played_pos);
		mTvPlayPosition=(TextView)findViewById(R.id.video_player_dlg_played_time);
		mTvEndPosition=(TextView)findViewById(R.id.video_player_dlg_played_endpos);
		mSurfaceView=(SurfaceView)findViewById(R.id.video_player_dlg_video);
		mIbPlay=(ImageButton)findViewById(R.id.video_player_dlg_start_stop);
		mIbShare=(ImageButton)findViewById(R.id.video_player_dlg_share);
		mIbCapture=(ImageButton)findViewById(R.id.video_player_dlg_capture);
		mIbForward=(ImageButton)findViewById(R.id.video_player_dlg_forward);
		mIbBackward=(ImageButton)findViewById(R.id.video_player_dlg_backward);
		mTvTitle=(NonWordwrapTextView)findViewById(R.id.video_player_dlg_title);
//		mLayoutTop=(LinearLayout)findViewById(R.id.video_player_dlg_top_panel);
//		mLayoutBottom=(LinearLayout)findViewById(R.id.video_player_dlg_bottom_panel);

        mOperationView=(RelativeLayout)findViewById(R.id.video_player_operation_view);
    };

    @Override
    public void onResume() {
    	super.onResume();
    	mLog.addDebugMsg(1, "I","onResume entered, restartStatus="+mRestartStatus);
    	if (mRestartStatus==1) {
    	} else {
    		initFileList();
			if (mRestartStatus==0) {
				
			} else if (mRestartStatus==2) {
				
			}
	        mRestartStatus=1;
	        
	        setMainViewListener();
    	}
    };
    
    @Override
    public void onPause() {
    	super.onPause();
    	mLog.addDebugMsg(1,"I","onPause entered");
    	if (isVideoPlayerStatusPlaying()) {
    		pauseVideoPlaying();
    	}
    };

    @Override
    public void onStop() {
    	super.onStop();
    	mLog.addDebugMsg(1,"I","onStop entered");
    };

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	mLog.addDebugMsg(1,"I","onDestroy entered");
    	mApplicationTerminated=true;
		if (!isVideoPlayerStatusStopped()) {
			mMediaPlayer.stop();
			mMediaPlayer.reset();
		}
    	mMediaPlayer.release();
    	mLog.flushLog();
    };



//	public String getRealPathFromURI(Context context, Uri contentUri) {
//	  Cursor cursor = null;
//	  if (contentUri.toString().startsWith("content://jp.naver.line.android.line")) {
//	      return contentUri.toString().replace("content://jp.naver.line.android.line.common.FileProvider/primary-storage/","/storage/emulated/0/");
//      } else {
//          try {
//              String[] proj = { MediaStore.Images.Media.DATA };
//              cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
//              int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
//              cursor.moveToFirst();
//              return cursor.getString(column_index);
//          } finally {
//              if (cursor != null) {
//                  cursor.close();
//              }
//          }
//      }
//	};


    private SafFile3 mMainFile=null;

	private void initFileList() {
		Intent intent=getIntent();
		String s_fn="";
//		Log.v("","ext="+intent.getExtras()+", data="+intent.getData().toString());
		if (intent.getData()!=null) {//Invoked by other application
		    mMainFile=new SafFile3(mContext, intent.getData());
//		    mUriFileInfo= ContentProviderUtil.getUriFileInfo(mContext, intent.getData(), intent.getFlags());
		}

	};

	private boolean isVideFile(String fn) {
	    String mt=getMimeTypeFromFileExtention(fn);
	    if (mt==null) return false;
	    else if (mt.startsWith("video/")) return true;
        return false;
    }

    private String getMimeTypeFromFileExtention(String fn) {
        String fid="", mt=null;
        if (fn.lastIndexOf(".") > 0) {
            fid = fn.substring(fn.lastIndexOf(".") + 1, fn.length());
            fid=fid.toLowerCase();
        }
        mt= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
        return mt;
    }

	public void setMainViewListener() {

        mSurfaceHolder=mSurfaceView.getHolder();
//        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
//        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback(){
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mLog.addDebugMsg(1,"I","surfaceCreated entered, Player status="+getVideoPlayerStatus());
				if (mIsVideoReadyToBePlayed) {
					mIsVideoReadyToBePlayed=false;
					mIbPlay.performClick();
				} else {
					if (isVideoPlayerStatusPausing()) {
						mMediaPlayer.setDisplay(mSurfaceHolder);
						mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition());
					}
				}
			};

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				mLog.addDebugMsg(1,"I","surfaceChanged entered, width="+width+", height="+height+
						", Player status="+getVideoPlayerStatus());
			};

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				mLog.addDebugMsg(1,"I","surfaceDestroyed entered, Player status="+getVideoPlayerStatus());
				if (isVideoPlayerStatusPlaying()) {
					mMediaPlayer.pause();
					setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
				}
			};
        });

        mSurfaceView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOperationView.getVisibility()==RelativeLayout.GONE) mOperationView.setVisibility(RelativeLayout.VISIBLE);
                else mOperationView.setVisibility(RelativeLayout.GONE);
            }
        });

		mIbShare.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                intent.putExtra(Intent.EXTRA_STREAM, mMainFile.getUri());
//                intent.setType("image/*");
                if (mMainFile.getMimeType()!=null) intent.setType(mMainFile.getMimeType());
                else intent.setType("video/mp4");
                try {
                    mContext.startActivity(intent);
                } catch(Exception e) {
                    mGp.commonDlg.showCommonDialog(false, "E", "startActivity() failed at shareItem() for songle item. message="+e.getMessage(), "", null);
                }

            }
		});

		setCaptureBtnEnabled(true);
		mIbCapture.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setCaptureBtnEnabled(false);
				final int c_pos=mSbPlayPosition.getProgress();
				Thread th=new Thread() {
					@Override
					public void run() {
						MediaMetadataRetriever mr=new MediaMetadataRetriever();
                        try {
                            ParcelFileDescriptor pfd=mContext.getContentResolver().openFileDescriptor(mMainFile.getUri(), "r");
                            mr.setDataSource(pfd.getFileDescriptor());
                            Bitmap bm=mr.getFrameAtTime(c_pos*1000);
                            putPicture(bm);
                            mUiHandler.post(new Runnable(){
                                @Override
                                public void run() {
                                    setCaptureBtnEnabled(true);
                                }
                            });
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
					}
				};
				th.start();
			}
		});

		setForwardBtnEnabled(true);
		mIbForward.setOnTouchListener(new OnTouchListener(){
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (isVideoPlayerStatusPlaying()) {
					mIsPlayRequiredAfterMoveFrame=true;
					pauseVideoPlaying();
				} else {
					if (isVideoPlayerStatusStopped()) {
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(true);
						prepareVideo(false);
						setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
					}
				}
				if (event.getAction()== MotionEvent.ACTION_DOWN) {
					if (mMoveFrameActive) {
						stopMoveFrame();
					}
					mTcMoveFrame.setEnabled();
					startMoveFrame(mTcMoveFrame,"F");
				} else if (event.getAction()== MotionEvent.ACTION_UP) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				} else if (event.getAction()== MotionEvent.ACTION_CANCEL) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				}
				return false;
			}
		});

		setBackwardBtnEnabled(true);
		mIbBackward.setOnTouchListener(new OnTouchListener(){
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (isVideoPlayerStatusPlaying()) {
					mIsPlayRequiredAfterMoveFrame=true;
					pauseVideoPlaying();
				} else {
					if (isVideoPlayerStatusStopped()) {
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(true);
						prepareVideo(false);
						setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
					}
				}
				if (event.getAction()== MotionEvent.ACTION_DOWN) {
					if (mMoveFrameActive) {
						stopMoveFrame();
					}
					mTcMoveFrame.setEnabled();
					startMoveFrame(mTcMoveFrame,"B");
				} else if (event.getAction()== MotionEvent.ACTION_UP) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				} else if (event.getAction()== MotionEvent.ACTION_CANCEL) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				}
				return false;
			}
		});

		mSbPlayPosition.setProgress(0);
		mSbPlayPosition.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar sb, int progress, boolean byUser) {
				if (byUser) {
					mMediaPlayer.seekTo(progress);
					mTvPlayPosition.setText(getTimePosition(progress));
					setMoveFrameBtnEnabled(progress, sb.getMax());
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				mMediaPlayer.setVolume(0f, 0f);
			}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				mMediaPlayer.setVolume(1.0f, 1.0f);
			}
		});

		mIbPlay.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isVideoPlayerStatusPlaying()) {
					pauseVideoPlaying();
				} else {
					//Start
					setPlayBtnPause(true);
					if (isVideoPlayerStatusStopped()) {
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(true);
						prepareVideo(true);
						setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
					} else {
						resumePlayVideo();
					}
				}
			}
		});
	};

	private void resumePlayVideo() {
		int c_pos=mMediaPlayer.getCurrentPosition();
		int c_max=mMediaPlayer.getDuration();
		if (c_pos>=c_max) {
			mMediaPlayer.seekTo(0);
			mSbPlayPosition.setProgress(0);
		}
		mSbPlayPosition.setEnabled(true);
		setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
		mTcPlayer.setEnabled();
		startVideoThread();
	};
	
	private Thread mThMoveFrame=null;
	private boolean mMoveFrameActive=false;
	private ThreadCtrl mTcMoveFrame=new ThreadCtrl();
	private void waitMoveFrameThread() {
		if (mThMoveFrame!=null) {
			try {
				mThMoveFrame.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	private void stopMoveFrame() {
		mTcMoveFrame.setDisabled();
		synchronized(mTcMoveFrame) {
			mTcMoveFrame.notify();
		}
		waitMoveFrameThread();
	};
		
	private final int mStepIntervalTime=3000;
	private void startMoveFrame(final ThreadCtrl tc, final String direction) {
		final Handler hndl=new Handler();
		mMoveFrameActive=true;
		mThMoveFrame=new Thread(){
			@Override
			public void run() {
				long b_wt=0, e_wt=0;
				long wait_time=150;
				while(tc.isEnabled()) {
					hndl.post(new Runnable(){
						@Override
						public void run() {
							moveFrame(direction);
						}
					});
					synchronized(tc) {
						try {
							b_wt= System.currentTimeMillis();
							tc.wait();
							e_wt=wait_time-(System.currentTimeMillis()-b_wt);
							if (e_wt>10) tc.wait(e_wt);
//							tc.wait(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				mMoveFrameActive=false;
				tc.setEnabled();
			}
		};
		SystemClock.sleep(100);
		mThMoveFrame.start();
	};
	
	private void setMoveFrameBtnEnabled(int pos, int max) {
		if (pos>0) {
			setBackwardBtnEnabled(true);
		} else if (pos<=0) {
			setBackwardBtnEnabled(false);
			stopMoveFrame();
			setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
			mIsPlayRequiredAfterMoveFrame=false;
		} 
		if (pos>=max) {
			setForwardBtnEnabled(false);
			stopMoveFrame();
			setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
			mIsPlayRequiredAfterMoveFrame=false;
		} else if (pos<max) {
			setForwardBtnEnabled(true);
		}
	};

	private void moveFrame(String direction) {
		int c_pos=mMediaPlayer.getCurrentPosition();
		int c_max=mMediaPlayer.getDuration();
		if (direction.equals("F")) {
			int n_pos=c_pos+mStepIntervalTime;
			if ((c_max-c_pos)<mStepIntervalTime) n_pos=c_max;
			mMediaPlayer.seekTo(n_pos);
			mSbPlayPosition.setProgress(n_pos);
			mTvPlayPosition.setText(getTimePosition(n_pos));
			setMoveFrameBtnEnabled(n_pos,c_max);
		} else {
			int n_pos=0;
			if (c_pos>mStepIntervalTime) n_pos=c_pos-mStepIntervalTime;
			mMediaPlayer.seekTo(n_pos);
			mSbPlayPosition.setProgress(n_pos);
			mTvPlayPosition.setText(getTimePosition(n_pos));
			setMoveFrameBtnEnabled(n_pos,c_max);
		}

	}

//    private Uri getMediaUri(String fp) {
//    	Uri image_uri=null;
//		Uri base_uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//		Uri query = base_uri.buildUpon().appendQueryParameter("limit", "1").build();
//		String [] projection = new String[] {ImageColumns.DATA, ImageColumns._ID};
//		String selection = ImageColumns.MIME_TYPE + "='image/jpeg'";
//		String order = ImageColumns.DATA;
//		Cursor ci = null;
//		try {
//			ci = getContentResolver().query(base_uri, projection, selection, null, order);
//			if( ci != null) {
//		        while( ci.moveToNext() ){
//		        	String file_path=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DATA));
//		        	Log.v("","data="+file_path);
//		        	if (file_path.equals(fp)) {
//						long id = ci.getLong(ci.getColumnIndex( MediaStore.Images.Media._ID));
//						image_uri = ContentUris.withAppendedId(base_uri, id);
//						image_uri = base_uri;
//						Log.v("","fp="+fp+", id="+id);
//						break;
//		        	}
//		        }
//		        ci.close();
//			}
//		}
//		finally {
//			if( ci != null ) {
//				ci.close();
//			}
//		}
//		return image_uri;
//    };
	
	private void pauseVideoPlaying() {
		stopVideoPlayer();
		setPlayBtnEnabled(true);
		setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
	};
	
	private void setCaptureBtnEnabled(boolean p) {
		mIbCapture.setEnabled(p);
		if (p) mIbCapture.setImageResource(R.drawable.capture_enabled);
		else mIbCapture.setImageResource(R.drawable.capture_disabled);
	};
	
	private void setForwardBtnEnabled(boolean p) {
		mIbForward.setEnabled(p);
		if (p) mIbForward.setImageResource(R.drawable.player_fast_forward_enabled);
		else mIbForward.setImageResource(R.drawable.player_fast_forward_disabled);
	};

	private void setBackwardBtnEnabled(boolean p) {
		mIbBackward.setEnabled(p);
		if (p) mIbBackward.setImageResource(R.drawable.player_fast_backward_enabled);
		else mIbBackward.setImageResource(R.drawable.player_fast_backward_disabled);
	};

	private void putPicture(Bitmap bm) {
		String dir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/";
		File l_dir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		if (!l_dir.exists()) l_dir.mkdirs();
		String ftime= StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis())
				.replaceAll("/","-").replaceAll(":","").replaceAll(" ", "_");
		String fn="dr_pic_"+ftime+".jpg";
		final String pfp=dir+fn;
		try {
			FileOutputStream fos=new FileOutputStream(pfp);
			BufferedOutputStream bos=new BufferedOutputStream(fos,4096*256);
			bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
			bos.flush();
			bos.close();
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					Toast toast = Toast.makeText(mContext,
							"静止画を保存しました。ファイル＝"+pfp, Toast.LENGTH_SHORT);
					toast.show();
				}
			});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanMediaFile(dir+fn);
	};
	
	private Bitmap mThumnailBitmap=null;
	private void stopVideoPlayer() {
		if (isVideoPlayerStatusPlaying()) {
			mMediaPlayer.pause();
			setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
			stopVideoThread();
		} 
	};
	
	private void setPlayBtnEnabled(boolean p) {
		if(p) {
			mIbPlay.setEnabled(true);
			mIbPlay.setImageResource(R.drawable.player_play_enabled);
		} else {
			mIbPlay.setEnabled(false);
			mIbPlay.setImageResource(R.drawable.player_play_disabled);
		}
	};

	private void setPlayBtnPause(boolean p) {
		if(p) {
			mIbPlay.setEnabled(true);
			mIbPlay.setImageResource(R.drawable.player_play_pause);
		} else {
			mIbPlay.setEnabled(false);
			mIbPlay.setImageResource(R.drawable.player_play_pause);
		}
	};

	private void setNextPrevBtnStatus() {
//		if ((mCurrentSelectedPos+1)<mFileList.size()) {
//			mIbNextFile.setEnabled(true);
//			mIbNextFile.setImageResource(R.drawable.next_file_enabled);
//		} else {
//			mIbNextFile.setEnabled(false);
//			mIbNextFile.setImageResource(R.drawable.next_file_disabled);
//		}
//		if (mCurrentSelectedPos>0) {
//			mIbPrevFile.setEnabled(true);
//			mIbPrevFile.setImageResource(R.drawable.prev_file_enabled);
//		} else {
//			mIbPrevFile.setEnabled(false);
//			mIbPrevFile.setImageResource(R.drawable.prev_file_disabled);
//		}
	};
	
	private MediaPlayer mMediaPlayer=null;
	private ThreadCtrl mTcPlayer=null;
	private void prepareVideo(final boolean start) {
		mLog.addDebugMsg(1,"I","prepareVideo entered, Player status="+getVideoPlayerStatus());
		if (isVideoPlayerStatusPlaying()) return;
		mTcPlayer.setEnabled();
		mSurfaceView.setVisibility(SurfaceView.VISIBLE);
		setCaptureBtnEnabled(true);
		try {
			mTvTitle.setText(mMainFile.getPath());

			mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener(){
				@Override
				public void onBufferingUpdate(MediaPlayer mp, int percent) {
					mLog.addDebugMsg(1,"I", "onBufferingUpdate percent:" + percent);
				}
			});
			mMediaPlayer.setOnErrorListener(new OnErrorListener(){
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					mLog.addDebugMsg(1,"I","onErrorListener called, what="+what+", extra="+extra);
					stopVideoThread();
					stopMediaPlayer();
					return true;
				}
			});
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener(){
				@Override
				public void onCompletion(MediaPlayer mp) {
					mLog.addDebugMsg(1,"I","onCompletion called");
					setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
					mMediaPlayer.pause();
					stopVideoThread();
				}
			});
			mMediaPlayer.setOnPreparedListener(new OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp) {
					mLog.addDebugMsg(1,"I","onPrepared called");
					MediaMetadataRetriever mr=new MediaMetadataRetriever();
                    ParcelFileDescriptor pfd= null;
                    try {
                        pfd = mContext.getContentResolver().openFileDescriptor(mMainFile.getUri(), "r");
                        mr.setDataSource(pfd.getFileDescriptor());
                        String br_str=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                        BigDecimal br=new BigDecimal("0.00");
                        if (br_str!=null)  {
                            BigDecimal br_a=new BigDecimal(br_str);
                            BigDecimal br_b=new BigDecimal(1000*1000);
                            br=br_a.divide(br_b,0, BigDecimal.ROUND_HALF_UP);
                        }

                        String wh=" "+mMediaPlayer.getVideoWidth()+" x "+mMediaPlayer.getVideoHeight()+" "+br+"MBPS";
                        mTvTitle.setText(mMainFile.getPath());

                        mSbPlayPosition.setMax(mMediaPlayer.getDuration());
                        mTvEndPosition.setText(getTimePosition(mMediaPlayer.getDuration()));
                        if (mGp.settingsVideoPlaybackKeepAspectRatio) {
                            int surfaceView_Width = mSurfaceView.getWidth();
                            int surfaceView_Height = mSurfaceView.getHeight();
                            float video_Width = mMediaPlayer.getVideoWidth();
                            float video_Height = mMediaPlayer.getVideoHeight();
                            float ratio_width = surfaceView_Width/video_Width;
                            float ratio_height = surfaceView_Height/video_Height;
                            float aspectratio = video_Width/video_Height;
                            android.view.ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
                            if (ratio_width > ratio_height){
                                layoutParams.width = (int) (surfaceView_Height * aspectratio);
                                layoutParams.height = surfaceView_Height;
                            }else{
                                layoutParams.width = surfaceView_Width;
                                layoutParams.height = (int) (surfaceView_Width / aspectratio);
                            }
                            mSurfaceView.setLayoutParams(layoutParams);
                        } else {
//					    mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                            mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                        }

                        setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
                        startVideoThread();
                        setNextPrevBtnStatus();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
				}
			});
			mMediaPlayer.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener(){
				@Override
				public void onVideoSizeChanged(MediaPlayer mp, int video_width, int video_height) {
					mLog.addDebugMsg(1,"I","onVideoSizeChanged called, width="+video_width+", height="+video_height);
				}
			});
			
			mMediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener(){
				@Override
				public void onSeekComplete(MediaPlayer mp) {
//					Log.v("","seek completed, pos="+mp.getCurrentPosition());
					synchronized(mTcMoveFrame) {
						mTcMoveFrame.notify();
					}
				}
				
			});

            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(mMainFile.getUri(), "r");
			mMediaPlayer.setDataSource(pfd.getFileDescriptor());
			mMediaPlayer.setDisplay(mSurfaceHolder);
//			mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
			mMediaPlayer.prepareAsync();
			
		} catch (IllegalArgumentException e) {
			mCommonDlg.showCommonDialog(false, "E", "IllegalArgumentException","Path="+mMainFile.getPath(), null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		} catch (SecurityException e) {
			mCommonDlg.showCommonDialog(false, "E", "SecurityException", "Path="+mMainFile.getPath(), null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		} catch (IllegalStateException e) {
			mCommonDlg.showCommonDialog(false, "E", "IllegalStateException","Path="+mMainFile.getPath(), null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		} catch (IOException e) {
			mCommonDlg.showCommonDialog(false, "E", "IOException","Path="+mMainFile.getPath(), null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		}
	};
	
	private String getTimePosition(int cp) {
		int mm=cp/1000/60;
		int ss=(cp-(mm*1000*60))/1000;
		return String.format("%02d",mm)+":"+ String.format("%02d",ss);
	};

	private Thread mPlayerThread=null;
	
	private void stopVideoThread() {
		mTcPlayer.setDisabled();
		synchronized(mTcPlayer) {
			mTcPlayer.notify();
		}
		try {
			if (mPlayerThread!=null) mPlayerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	};
	
	private void startVideoThread() {
//		Thread.dumpStack();
//		setMoveFrameBtnEnabled(mMediaPlayer.getCurrentPosition(), mMediaPlayer.getDuration());
		mPlayerThread=new Thread() {
			@Override
			public void run() {
				mMediaPlayer.start();
				final int interval=100;
				while (isVideoPlayerStatusPlaying()) {
					try {
						if (isVideoPlayerStatusPlaying()) {
							mUiHandler.post(new Runnable(){
								@Override
								public void run() {
									if (isVideoPlayerStatusPlaying()) {
										int cp=mMediaPlayer.getCurrentPosition();
//										int cp=mSbPlayPosition.getProgress();
										mSbPlayPosition.setProgress(cp+interval);
										mTvPlayPosition.setText(getTimePosition(cp+interval));
//										Log.v("","cp="+cp);
										setMoveFrameBtnEnabled(cp+interval, mMediaPlayer.getDuration());
									}
								}
							});
						}
						synchronized(mTcPlayer) {
							mTcPlayer.wait(interval);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!mTcPlayer.isEnabled()) {
						mLog.addDebugMsg(1,"I", "startVideoThread cancelled");
						break;
					} else {
//						mSbPlayPosition.setProgress(mSbPlayPosition.getMax());
					}
				}
				mLog.addDebugMsg(1,"I", "startVideoThread expired");
				mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						setPlayBtnEnabled(true);
					}
				});
			}
		};
		mPlayerThread.setName("Player");
		mPlayerThread.start();		
	};

	private void stopMediaPlayer() {
		setVideoPlayerStatus(VIDEO_STATUS_STOPPED);
		try {
			mMediaPlayer.stop();
			mMediaPlayer.reset();
//			mMediaPlayer.release();
		} catch(IllegalStateException e) {
		}
	};

    private void scanMediaFile(String fp) {
    	String[] paths = new String[] {fp};
    	MediaScannerConnection.scanFile(getApplicationContext(), paths, null, mOnScanCompletedListener);
    };
    
	private OnScanCompletedListener mOnScanCompletedListener=new OnScanCompletedListener(){
		@Override
		public void onScanCompleted(String path, Uri uri) {
			mLog.addDebugMsg(1,"I", "Scan completed path="+path+", uri="+uri);
		}
	};

	private void setVideoPlayerStatus(int p) {
		mVideoPlayerStatus=p;
	};
	private int getVideoPlayerStatus() {
		return mVideoPlayerStatus;
	};
	private boolean isVideoPlayerStatusStopped() {
		return mVideoPlayerStatus == VIDEO_STATUS_STOPPED;
	};
	private boolean isVideoPlayerStatusPlaying() {
		return mVideoPlayerStatus == VIDEO_STATUS_PLAYING;
	};
	private boolean isVideoPlayerStatusPausing() {
		return mVideoPlayerStatus == VIDEO_STATUS_PAUSING;
	};

	@SuppressLint("DefaultLocale")
	private static String isMediaFile(String fp) {
		String mt=null;
		String fid="";
		if (fp.lastIndexOf(".")>0) {
			fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
			fid=fid.toLowerCase();
		}
		mt= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		if (mt==null) return "";
		else return mt;
	};
}

