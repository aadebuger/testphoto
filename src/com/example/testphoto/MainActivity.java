package com.example.testphoto;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class MainActivity extends Activity {

	private GridView gridview;
	TextView group_text, total_text;
	ListView group_listview;

	private ProgressDialog mProgressDialog;

	private ProgressDialog mDirDialog;
	private ImageLoader mImageLoader;

	private HashMap<String, ArrayList<String>> mGruopMap = new HashMap<String, ArrayList<String>>();
	private ArrayList<ImageBean> imgBeanLists = new ArrayList<ImageBean>();

	// ���е�ͼƬ
	private ArrayList<String> mAllImgs;
	private final static int SCAN_OK = 1;

	private final static int SCAN_FOLDER_OK = 2;
	private RelativeLayout list_layout;
	private DisplayImageOptions options;

	private ListAdapter listAdapter;
	
	private int limit_count ;
	
	Animation toUp, toDown;

	// private GridAdapter gridAdatper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		initData();
		setListener();
	}

	private void setListener() {
		
		total_text.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//addedPath���ظ��ϸ�ҳ��-----����ֻѡ��������
				Intent dataIntent = new Intent();
				Bundle dataBundle = new Bundle();
				dataBundle.putStringArrayList("pic_paths", addedPath);
				dataIntent.putExtras(dataBundle);
				setResult(RESULT_OK, dataIntent);
				MainActivity.this.finish();
			}
		});
		
		group_text.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (list_layout.getVisibility() == View.VISIBLE) {
					list_layout.setVisibility(View.GONE);
					list_layout.startAnimation(toDown);
				} else {
					list_layout.setVisibility(View.VISIBLE);
					list_layout.startAnimation(toUp);
				}
			}
		});

		group_listview
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int position, long arg3) {
						// ���ˢ�¶�Ӧ����ͼ
						if (chooseItem.get(0) == position) {
							// ��������������
							list_layout.setVisibility(View.GONE);
						} else {
							chooseItem.clear();
							chooseItem.add(position);
							listAdapter.notifyDataSetChanged();
							list_layout.setVisibility(View.GONE);

							// ��ȡ��mAllImgs������ʾ��������
							GridAdapter gridAdatper = new GridAdapter();
							gridAdatper.setData(new ArrayList<String>());
							gridview.setAdapter(gridAdatper);
							gridAdatper = null;

							// �õ���ǰ����ˢ��
							if (0 == position) {
								getImages();
							} else {
								// ˢ�µ�ǰ��GridView
								mDirDialog = ProgressDialog.show(MainActivity.this, null, "���ڼ���...");
								nowStrs.clear();
								String fa_path = imgBeanLists.get(position).getFa_filepath();
								nowStrs.addAll(mGruopMap.get(fa_path));
								Log.e("cxm", "fa_path="+fa_path+",nowStrs.size="+nowStrs.size());
								mHandler.sendEmptyMessageDelayed(SCAN_FOLDER_OK, 1000);
//								// ֪ͨHandlerɨ��ͼƬ���
//								getFolderImages(imageBean.getFa_filepath());
							}
						}
					}
				});
		
		gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				if(chooseItem.get(0) == 0 && 0 == position) {
					//����ϵͳ���
					//�ж��Ƿ�����8��ͼƬ
					if(addedPath.size() >= limit_count) {
						Toast.makeText(MainActivity.this, "���ѡ8�ţ���ȡ�����ٵ������", Toast.LENGTH_SHORT).show();
						return;
					}
					
					tempCameraPath = IndexActivity.CAMERA_PATH + "/"
							+ System.currentTimeMillis() + ".jpg";
					Log.e("cxm", "path============"+tempCameraPath);
					PickPhotoUtil.getInstance().takePhoto(
							MainActivity.this, "tempUser", tempCameraPath);
				}
			}
		});
	}
	
	private String tempCameraPath = "";
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK) {
			switch (requestCode) {
			case PickPhotoUtil.PickPhotoCode.PICKPHOTO_TAKE:
				
				File fi = new File("");
				PickPhotoUtil.getInstance().takeResult(this,
						data, fi);
				
				//�����ͼƬ
				ArrayList<String> camepaths = new ArrayList<String>();
				camepaths.add(tempCameraPath);
				Intent dataIntent = new Intent();
				Bundle dataBundle = new Bundle();
				dataBundle.putStringArrayList("pic_paths", camepaths);
				dataIntent.putExtras(dataBundle);
				setResult(RESULT_OK, dataIntent);
				MainActivity.this.finish();
				break;

			default:
				break;
			}
		}
	}

//	protected ArrayList<String> getFolderImages(final String path_dir) {
//		nowStrs.clear();
//		Log.e("cxm", "foldename=" + path_dir);
//		// ��ʾ������
//		mDirDialog = ProgressDialog.show(this, null, "���ڼ���...");
//
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				getFiles(path_dir);
//
//				// ֪ͨHandlerɨ��ͼƬ���
//				mHandler.sendEmptyMessage(SCAN_FOLDER_OK);
//			}
//		}).start();
//		return null;
//	}

	ArrayList<String> nowStrs = new ArrayList<>();

//	private void getFiles(String filePath) {
//		File root = new File(filePath);
//		File[] files = root.listFiles();
//		for (File file : files) {
//			if (file.isFile()) {
//				nowStrs.add(file.getAbsolutePath());
//			} else {
//				/*
//				 * �ݹ����
//				 */
//				getFiles(file.getAbsolutePath());
//			}
//		}
//	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case SCAN_OK:
				// �رս�����
				mProgressDialog.dismiss();

				// adapter = new GroupAdapter(MainActivity.this, list =
				// subGroupOfImage(mGruopMap), mGroupGridView);
				// mGroupGridView.setAdapter(adapter);
				// ɨ����ɺ󣬸�listview��ֵ------������ͼƬ��ֵ
				imgBeanLists = subGroupOfImage(mGruopMap);
				listAdapter = new ListAdapter();
				listAdapter.setData(imgBeanLists);
				group_listview.setAdapter(listAdapter);
				// ��ȡ��mAllImgs������ʾ��������
				GridAdapter gridAdatper = new GridAdapter();
				gridAdatper.setData(mAllImgs);
				gridview.setAdapter(gridAdatper);
				gridAdatper = null;
				break;
			case SCAN_FOLDER_OK:
				mDirDialog.dismiss();
				// ��ȡ��mAllImgs������ʾ��������
				GridAdapter gridAdatper1 = new GridAdapter();
				gridAdatper1.setData(nowStrs);
				gridview.setAdapter(gridAdatper1);
				gridAdatper1 = null;
				break;
			}
		}

	};

	private void initView() {
		gridview = (GridView) findViewById(R.id.gridview);
		group_text = (TextView) findViewById(R.id.group_text);
		total_text = (TextView) findViewById(R.id.total_text);
		group_listview = (ListView) findViewById(R.id.group_listview);

		list_layout = (RelativeLayout) findViewById(R.id.list_layout);
	}

	private void initData() {
		// ��ʼ�����ݣ�����ͼƬӦ��281������
		chooseItem.add(0);
		// imageLoader����
		DisplayImageOptions imageOptions = new DisplayImageOptions.Builder()
				.cacheInMemory().cacheOnDisc().build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				this).defaultDisplayImageOptions(imageOptions)
				.discCacheFileNameGenerator(new Md5FileNameGenerator())
				.memoryCacheSize(2 * 1024 * 1024)
//				.memoryCache(new WeakMemoryCache())
				.build();
		ImageLoader.getInstance().init(config);
		mImageLoader = ImageLoader.getInstance();

		options = new DisplayImageOptions.Builder().cacheOnDisc()
				.showImageForEmptyUri(R.drawable.friends_sends_pictures_no)
				.showImageOnFail(R.drawable.friends_sends_pictures_no)
				.showStubImage(R.drawable.friends_sends_pictures_no).build();

		mAllImgs = new ArrayList<String>(281);
		addedPath = new ArrayList<String>();
		limit_count = 8-IndexActivity.mPicList.size();
		total_text.setText("0/"+limit_count+"��");
		toUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.act_bottom_to_top);
		toDown = AnimationUtils.loadAnimation(MainActivity.this, R.anim.act_top_to_bottom);
		// listAdapter = new ListAdapter();
		// group_listview.setAdapter(listAdapter);
		//
		// gridAdatper = new GridAdapter();
		// gridview.setAdapter(gridAdatper);
		getImages();
	}

	/**
	 * ����ContentProviderɨ���ֻ��е�ͼƬ���˷��������������߳���
	 */
	private void getImages() {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, "�����ⲿ�洢", Toast.LENGTH_SHORT).show();
			return;
		}

		// ��ʾ������
		mProgressDialog = ProgressDialog.show(this, null, "���ڼ���...");

		new Thread(new Runnable() {

			@Override
			public void run() {
				Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				ContentResolver mContentResolver = MainActivity.this
						.getContentResolver();

				// ֻ��ѯjpeg��png��ͼƬ
				Cursor mCursor = mContentResolver.query(mImageUri, null,
						MediaStore.Images.Media.MIME_TYPE + "=? or "
								+ MediaStore.Images.Media.MIME_TYPE + "=?",
						new String[] { "image/jpeg", "image/png" },
						MediaStore.Images.Media.DATE_MODIFIED);

				while (mCursor.moveToNext()) {
					// ��ȡͼƬ��·��
					String path = mCursor.getString(mCursor
							.getColumnIndex(MediaStore.Images.Media.DATA));

					// ��ȡ��ͼƬ�ĸ�·����
					File pa_file = new File(path).getParentFile();
					String parentName = pa_file.getAbsolutePath();
					if (mAllImgs.size() < 281) {
						mAllImgs.add(path);
					}
					// ���ݸ�·������ͼƬ���뵽mGruopMap��
					if (!mGruopMap.containsKey(parentName)) {
						ArrayList<String> chileList = new ArrayList<String>();
						chileList.add(path);
						mGruopMap.put(parentName, chileList);
					} else {
						mGruopMap.get(parentName).add(path);
					}
				}

				mCursor.close();

				// ֪ͨHandlerɨ��ͼƬ���
				mHandler.sendEmptyMessage(SCAN_OK);

			}
		}).start();

	}

	/**
	 * ��װ�������GridView������Դ����Ϊ����ɨ���ֻ���ʱ��ͼƬ��Ϣ����HashMap�� ������Ҫ����HashMap��������װ��List
	 * 
	 * @param mGruopMap
	 * @return
	 */
	private ArrayList<ImageBean> subGroupOfImage(
			HashMap<String, ArrayList<String>> gruopMap) {
		if (gruopMap.size() == 0) {
			return null;
		}
		ArrayList<ImageBean> list = new ArrayList<ImageBean>();
		Iterator<Map.Entry<String, ArrayList<String>>> it = gruopMap.entrySet()
				.iterator();
		ImageBean ig0 = new ImageBean();
		ig0.setFolderName("����ͼƬ");
		ig0.setImageCounts(0);
		ig0.setTopImagePath("");
		list.add(0, ig0);
		while (it.hasNext()) {
			Map.Entry<String, ArrayList<String>> entry = it.next();
			ImageBean mImageBean = new ImageBean();
			String key = entry.getKey();
			List<String> value = entry.getValue();
			File dir_file = new File(key);
			mImageBean.setFolderName(dir_file.getName());
			mImageBean.setImageCounts(value.size());
			mImageBean.setTopImagePath(value.get(0));// ��ȡ����ĵ�һ��ͼƬ
			mImageBean.setFa_filepath(key);
			list.add(mImageBean);
		}

		return list;

	}

	private ArrayList<String> addedPath = null;

	// gridview��Adapter
	class GridAdapter extends BaseAdapter {
		// �������ֲ�ͬ�Ĳ�����Ӧ��
		final int VIEW_TYPE = 2;
		final int TYPE_1 = 0;
		final int TYPE_2 = 1;
		LayoutInflater inflater;
		private ArrayList<String> gridStrings;/**
		 * �����洢ͼƬ��ѡ�����
		 */
		private HashMap<Integer, Boolean> mSelectMap = new HashMap<Integer, Boolean>();

		public GridAdapter() {
			gridStrings = new ArrayList<>();
			inflater = LayoutInflater.from(MainActivity.this);
		}

		public void setData(ArrayList<String> strs) {
			if (null != strs) {
				gridStrings.clear();
				gridStrings.addAll(strs);
				notifyDataSetChanged();
			}
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return gridStrings.size();
		}

		@Override
		public String getItem(int position) {
			if (chooseItem.get(0) == 0) {
				return gridStrings.get(position - 1);
			} else {
				Log.e("cxm", "position===="+position+",path="+gridStrings.get(position));
				return gridStrings.get(position);
			}
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getItemViewType(int position) {
			if (chooseItem.get(0) == 0) {
				if (position == 0) {
					return TYPE_1;
				} else {
					return TYPE_2;
				}
			} else {
				return TYPE_2;
			}
		}

		@Override
		public int getViewTypeCount() {
			if (chooseItem.get(0) == 0) {
				return VIEW_TYPE;
			} else {
				return 1;
			}
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup arg2) {
			GridHolder gridHolder = null  ;
			PhotoHolder photoHodler = null;
			int type = getItemViewType(position);
			if (convertView == null) {
				switch (type) {
				case TYPE_1:
					// ��ʾ����
					photoHodler = new PhotoHolder();
					convertView = inflater.inflate(R.layout.take_photo, null);
					convertView.setTag(photoHodler);
					break;
				case TYPE_2:
					convertView = inflater.inflate(R.layout.grid_item, null);
					gridHolder = new GridHolder();
					gridHolder.grid_image = (ImageView) convertView
							.findViewById(R.id.grid_image);
					gridHolder.grid_img = (ImageView) convertView
							.findViewById(R.id.grid_img);
					convertView.setTag(gridHolder);
					break;
				default:
					break;
				}
			} else {
				switch (type) {
				case TYPE_1:
					// ��ʾ����
					photoHodler = (PhotoHolder) convertView.getTag();
					break;
				case TYPE_2:
					gridHolder = (GridHolder) convertView.getTag();
					break;
				default:
					break;
				}
			}

			if (type == TYPE_2) {
				// �ж��Ƿ��Ѿ����
				mImageLoader.displayImage("file://" + getItem(position),
						gridHolder.grid_image, options);

//				gridHolder.grid_check
//						.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//							@Override
//							public void onCheckedChanged(CompoundButton arg0,
//									boolean isChecked) {
//								Log.e("cxm", "boo==" + isChecked);
////								if (isSelect) {
////									addedPath.add(getItem(position));
////								} else {
////									// �Ѿ��������ȡ��
////									addedPath.remove(getItem(position));
////								}
//								if(!mSelectMap.containsKey(position) || !mSelectMap.get(position)){
//									addedPath.add(getItem(position));
//								} else {
//									mSelectMap.put(position, isChecked);
//									addedPath.remove(getItem(position));
//								}
//							}
//						});
				
				gridHolder.grid_img.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View view) {
						if(addedPath.contains(getItem(position))) {
							//�Ѿ��������path�ˣ���ɵ�
							addedPath.remove(getItem(position));
							((ImageView)view).setImageResource(R.drawable.friends_sends_pictures_select_icon_unselected);
						} else {
							//�жϴ�С
							if(addedPath.size() < limit_count) {
								addedPath.add(getItem(position));
								((ImageView)view).setImageResource(R.drawable.friends_sends_pictures_select_icon_selected);
								//���ͼƬ����ʾ��������
							}
						}
						mYhandler.sendEmptyMessage(0);
					}
				});

				if (addedPath.contains(getItem(position))) {
					// �Ѿ���ӹ���
					gridHolder.grid_img.setImageResource(R.drawable.friends_sends_pictures_select_icon_selected);
				} else {
					gridHolder.grid_img.setImageResource(R.drawable.friends_sends_pictures_select_icon_unselected);
				}
			}

			return convertView;
		}

		class PhotoHolder {

		}

		class GridHolder {
			ImageView grid_image;
			public ImageView grid_img;
		}

	}
	
	Handler mYhandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				total_text.setText(addedPath.size()+"/"+limit_count+"��");
				break;

			default:
				break;
			}
		}
	};

	private ArrayList<Integer> chooseItem = new ArrayList<>();

	class ListAdapter extends BaseAdapter {
		private ArrayList<ImageBean> beans = null;
		LayoutInflater inflater;

		public ListAdapter() {
			inflater = LayoutInflater.from(MainActivity.this);
			beans = new ArrayList<>();
		}

		public void setData(ArrayList<ImageBean> listBeans) {
			if (listBeans != null) {
				beans.clear();
				beans.addAll(listBeans);
				notifyDataSetChanged();
			}
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return beans.size();
		}

		@Override
		public ImageBean getItem(int arg0) {
			// TODO Auto-generated method stub
			return beans.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup arg2) {
			final ListViewHolder listHoder;
			ImageBean imageBean = beans.get(position);
			if (convertView == null) {
				listHoder = new ListViewHolder();
				convertView = inflater.inflate(R.layout.list_item, null);
				listHoder.myimage_view = (ImageView) convertView
						.findViewById(R.id.myimage_view);
				listHoder.choose_img = (ImageView) convertView
						.findViewById(R.id.choose_img);
				listHoder.folder_text = (TextView) convertView
						.findViewById(R.id.folder_text);
				listHoder.count_text = (TextView) convertView
						.findViewById(R.id.count_text);
				convertView.setTag(listHoder);
			} else {
				listHoder = (ListViewHolder) convertView.getTag();
			}
			int cho_posi = chooseItem.get(0);
			if (position == cho_posi) {
				// �������ʾ
				listHoder.choose_img.setVisibility(View.VISIBLE);
			} else {
				listHoder.choose_img.setVisibility(View.GONE);
			}
			String img_path = "";
			if (position == 0) {
				img_path = beans.get(1).getTopImagePath();
				listHoder.count_text.setVisibility(View.GONE);
			} else {
				img_path = imageBean.getTopImagePath();
				listHoder.count_text.setVisibility(View.VISIBLE);
				listHoder.count_text.setText(imageBean.getImageCounts()+"��");
			}
			listHoder.folder_text.setText(imageBean.getFolderName());
			mImageLoader.displayImage("file://" + img_path,
					listHoder.myimage_view, options);
			return convertView;
		}

		class ListViewHolder {
			ImageView myimage_view;
			ImageView choose_img;
			TextView folder_text, count_text;
		}

	}
}
