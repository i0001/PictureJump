package example.android.picturejump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import example.android.bluetooth.PictureSendJob;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

// �摜���M���Activity
public class PictureSendActivity extends Activity {

	// �M�������[�\���p���N�G�X�g�R�[�h
	private static final int REQUEST_CODE_GALLARY = 0;

	// �J������ʕ\���p���N�G�X�g�R�[�h
	private static final int REQUEST_CODE_CAMERA = 1;

	// �f�o�C�X���o��ʕ\���p���N�G�X�g�R�[�h
	private static final int REQUEST_CODE_DEVICE = 2;

	// ���M�f�o�C�X
	private BluetoothDevice device;

	// ���M�摜
	private Bitmap picture;

	// �񓯊����s�p�X���b�h�v�[��
	private ExecutorService executorService;

	// �i���_�C�A���O
	private ProgressDialog progressDialog;

	// onCreate���\�b�h(��ʏ����\���C�x���g)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// �񓯊����s�p�̃X���b�h�v�[������
		executorService = Executors.newCachedThreadPool();

		// ���C�A�E�g�ݒ�t�@�C���w��
		setContentView(R.layout.picture_send);

		// �u���M��I���v�{�^���Ƀ��X�i�[�ݒ�
		ImageButton ibtnSelectDevice = (ImageButton) findViewById(R.id.ibtn_select_device);
		ibtnSelectDevice.setOnClickListener(new OnClickListener() {
			// onClick���\�b�h(�N���b�N�C�x���g)
			@Override
			public void onClick(View v) {
				// ���M�f�o�C�X�I����ʌĂяo��
				Intent intent = new Intent(PictureSendActivity.this,
						DeviceSelectActivity.class);
				startActivityForResult(intent, REQUEST_CODE_DEVICE);
			}
		});

		// �u�M�������[�v�{�^���Ƀ��X�i�[�ݒ�
		ImageButton ibtnGallery = (ImageButton) findViewById(R.id.ibtn_gallery);
		ibtnGallery.setOnClickListener(new OnClickListener() {
			// onClick���\�b�h(�N���b�N�C�x���g)
			@Override
			public void onClick(View v) {
				// �C���e���g����
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				// �C���e���g�^�C�v���摜�Őݒ�
				intent.setType("image/*");
				// �M�������[�N��
				startActivityForResult(Intent.createChooser(intent, AppUtil
						.getString(PictureSendActivity.this,
								R.string.picture_select_title)),
						REQUEST_CODE_GALLARY);
			}
		});

		// �u�J�����v�{�^���Ƀ��X�i�[�ݒ�
		ImageButton ibtnCamera = (ImageButton) findViewById(R.id.ibtn_camera);
		ibtnCamera.setOnClickListener(new OnClickListener() {
			// onClick���\�b�h(�N���b�N�C�x���g)
			@Override
			public void onClick(View v) {
				// �C���e���g����
				Intent intent = new Intent(PictureSendActivity.this,
						CameraActivity.class);
				// �J������ʋN��
				startActivityForResult(intent, REQUEST_CODE_CAMERA);
			}
		});

		// �u��]�v�{�^���Ƀ��X�i�[�ݒ�
		ImageButton ibtnPictureRotate = (ImageButton) findViewById(R.id.ibtn_picture_rotate);
		ibtnPictureRotate.setOnClickListener(new OnClickListener() {
			// onClick���\�b�h(�N���b�N�C�x���g)
			@Override
			public void onClick(View v) {
				// �摜��]����
				setPicture(AppUtil.rotateBitmap(picture));
			}
		});

		// �u�N���A�v�{�^���Ƀ��X�i�[�ݒ�
		Button btnClear = (Button) findViewById(R.id.btn_clear);
		btnClear.setOnClickListener(new OnClickListener() {
			// onClick���\�b�h(�N���b�N�C�x���g)
			@Override
			public void onClick(View v) {
				// �f�o�C�X�A�摜�\�����N���A
				setDevice(null);
				setPicture(null);
			}
		});

		// �u���M�v�{�^���Ƀ��X�i�[�ݒ�
		Button btnSend = (Button) findViewById(R.id.btn_send);
		btnSend.setOnClickListener(new OnClickListener() {
			// onClick���\�b�h(�N���b�N�C�x���g)
			@Override
			public void onClick(View v) {
				// �摜���M����
				send();
			}
		});

		// �uMAIL�v�{�^���Ƀ��X�i�[�ݒ�
		Button btnMailSend = (Button) findViewById(R.id.btn_send_mail);
		btnMailSend.setOnClickListener(new OnClickListener() {
			// onClick���\�b�h(�N���b�N�C�x���g)
			@Override
			public void onClick(View v) {

			// �C���e���g�ɃA�N�V�����y�ё��M�����Z�b�g
			Uri uri = Uri.parse("mailto:");
			Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
			intent.putExtra("sms_body", "�ʐ^�����[");

			// �摜��Y�t
			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(getExternalSdCardPath()));

		// ���[���N��
		startActivity(intent);

			}
			});


		// �{�^���̎g�p�\��Ԃ��X�V
		updateButtonStatus();
	}

	// onDestroy���\�b�h(��ʔj���C�x���g)
	@Override
	protected void onDestroy() {
		// �X���b�h�v�[���I��
		executorService.shutdownNow();
		super.onDestroy();
	}

	// send���\�b�h(�摜���M����)
	private void send() {

		// �i���_�C�A���O��\��
		showProgress();

		// �摜���M������ʃX���b�h�Ŏ��s
		executorService.execute(new PictureSendJob(device, picture) {

			// handleSendStarted���\�b�h(�摜���M�J�n�C�x���g)
			@Override
			protected void handleSendStarted(BluetoothDevice device) {
				// UI�X���b�h���s
				runOnUiThread(new Runnable() {
					// run���\�b�h(���s����)
					@Override
					public void run() {
						// ���M�J�n���ɐi�����b�Z�[�W�ύX
						updateProgressMessage(AppUtil.getString(
								PictureSendActivity.this,
								R.string.picture_send_progress_sending));
					}
				});
			}

			// handleSendFinish���\�b�h(�摜���M�I���C�x���g)
			@Override
			protected void handleSendFinish() {
				// UI�X���b�h���s
				runOnUiThread(new Runnable() {
					// run���\�b�h(���s����)
					@Override
					public void run() {
						// �i���_�C�A���O��\��
						hideProgress();
						// ���b�Z�[�W�\��
						AppUtil.showToast(PictureSendActivity.this,
								R.string.picture_send_succeed);
					}
				});
			}

			// handleProgress���\�b�h(�摜���M�i���ύX�C�x���g)
			@Override
			protected void handleProgress(final int total, final int progress) {
				// UI�X���b�h���s
				runOnUiThread(new Runnable() {
					// run���\�b�h(���s����)
					@Override
					public void run() {
						// �i���\���؂�ւ�
						int value = Math.round(progress * 100 / total);
						updateProgressValue(value);
					}
				});
			}

			// handleException���\�b�h(�摜���M��O�����C�x���g)
			@Override
			protected void handleException(IOException e) {
				// UI�X���b�h���s
				runOnUiThread(new Runnable() {
					// run���\�b�h(���s����)
					@Override
					public void run() {
						// �i���_�C�A���O��\��
						hideProgress();
						// �G���[���b�Z�[�W�\��
						AppUtil.showToast(PictureSendActivity.this,
								R.string.picture_send_failed);
					}
				});

			}
		});
	}

	// onActivityResult���\�b�h(��ʍĕ\�����C�x���g)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// �������ʂ�OK�̏ꍇ�A�����I��
		if (resultCode != RESULT_OK) {
			return;
		}

		// �f�o�C�X���o�̌��ʂ̏ꍇ
		if (requestCode == REQUEST_CODE_DEVICE) {
			// ���o�����f�o�C�X�擾
			device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			setDevice(device);

			// �M�������[�摜�擾���ʂ̏ꍇ
		} else if (requestCode == REQUEST_CODE_GALLARY) {

			try {
				// �I���摜��URI����摜�X�g���[���擾
				InputStream in = getContentResolver().openInputStream(
						data.getData());
				// �摜�\��
				setPicture(BitmapFactory.decodeStream(in));
			} catch (FileNotFoundException e) {
				Log.e(getClass().getSimpleName(), "get image failed.", e);
			}

			// �J�����摜�擾���ʂ̏ꍇ
		} else if (requestCode == REQUEST_CODE_CAMERA) {
			// �ꎞ�ۑ��摜�\��
			setPicture(BitmapFactory.decodeFile(AppUtil.getPictureTempPath()));
			// �ꎞ�ۑ��摜�폜
			AppUtil.deletePicture();
		}
	}

	// setDevice���\�b�h(�f�o�C�X�\������)
	private void setDevice(BluetoothDevice device) {
		this.device = device;
		TextView textView = (TextView) findViewById(R.id.tv_selected_device);
		textView.setText(device != null ? device.getName() : "");

		updateButtonStatus();
	}

	// setPicture���\�b�h(�摜�\������)
	private void setPicture(Bitmap picture) {
		this.picture = picture;
		ImageView imageView = (ImageView) findViewById(R.id.iv_selected_picture);
		imageView.setImageBitmap(AppUtil.resizePicture(picture, 180, 180));

		updateButtonStatus();
	}

	// updateButtonStatus���\�b�h(�{�^���̎g�p�\��ԍX�V����)
	private void updateButtonStatus() {

		View send = findViewById(R.id.btn_send);
		send.setEnabled(picture != null && device != null);

		View rotate = findViewById(R.id.ibtn_picture_rotate);
		rotate.setEnabled(picture != null);
	}

	// showProgress���\�b�h(�i���_�C�A���O�\������)
	private void showProgress() {
		// �i���_�C�A���O����
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(AppUtil.getString(this,
				R.string.picture_send_progress_title));
		progressDialog.setMessage(AppUtil.getString(this,
				R.string.picture_send_progress_waiting));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMax(100);
		progressDialog.setCancelable(false);
		// �_�C�A���O�\��
		progressDialog.show();
	}

	// hideProgress���\�b�h(�i���_�C�A���O��\������)
	private void hideProgress() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	// updateProgressMessage���\�b�h(�i�����b�Z�[�W�X�V����)
	private void updateProgressMessage(String message) {
		if (progressDialog != null) {
			progressDialog.setMessage(message);
		}
	}

	// updateProgressValue���\�b�h(�i���l���X�V����)
	private void updateProgressValue(int value) {
		if (progressDialog != null) {
			progressDialog.setProgress(value);
		}
	}

	/**
	 * �O��SD�J�[�h�̃p�X���擾����
	 *
	 * @return external SD card path, or null
	 */
	public static String getExternalSdCardPath(){
	    HashSet<String> paths = new HashSet<String>();
	    Scanner scanner = null;
	    try{
	        // �V�X�e���ݒ�t�@�C����ǂݍ���
	        File vold_fstab = new File("/system/etc/vold.fstab");
	        scanner = new Scanner(new FileInputStream(vold_fstab));
	        while(scanner.hasNextLine()){
	            String line = scanner.nextLine();
	            // dev_mount�܂���fuse_mount�Ŏn�܂�s
	            if(line.startsWith("dev_mount") || line.startsWith("fuse_mount")){
	                // ���p�X�y�[�X�ł͂Ȃ��^�u�ŋ�؂��Ă���@�������炵��
	                // ���p�X�y�[�X��؂�R�߁ipath�j���擾
	                String path = line.replaceAll("\t", " ").split(" ")[2];
	                paths.add(path);
	            }
	        }
	    }catch(FileNotFoundException e){
	        e.printStackTrace();
	        return null;
	    }finally{
	        if(scanner != null){
	            scanner.close();
	        }
	    }
	    // Environment.getExternalStorageDirectory() ������SD��Ԃ��ꍇ�͏��O
	    if(!Environment.isExternalStorageRemovable()){
	        paths.remove(Environment.getExternalStorageDirectory().getPath());
	    }
	    // �}�E���g����Ă���SD�J�[�h�̃p�X��ǉ�
	    List<String> mountSdCardPaths = new ArrayList<String>();
	    for(String path : paths){
	        if(isMounted(path)){
	            mountSdCardPaths.add(path);
	        }
	    }
	    // �}�E���g����Ă���SD�J�[�h�̃p�X
	    String mountSdCardPath = null;
	    if(mountSdCardPaths.size() > 0){
	        mountSdCardPath = mountSdCardPaths.get(0);
	    }
	    return mountSdCardPath;

	}

	/**
	 * �p�X���}�E���g����Ă���SD�J�[�h�̃p�X���`�F�b�N����
	 *
	 * @param path SD card path
	 * @return true if path is the mounted SD card's path, otherwise false
	 */
	public static boolean isMounted(String path){
	    boolean isMounted = false;
	    Scanner scanner = null;
	    try{
	        // �}�E���g�|�C���g���擾����
	        File mounts = new File("/proc/mounts");
	        scanner = new Scanner(new FileInputStream(mounts));
	        while(scanner.hasNextLine()){
	            if(scanner.nextLine().contains(path)){
	                isMounted = true;
	                break;
	            }
	        }
	    }catch(FileNotFoundException e){
	        e.printStackTrace();
	        return false;
	    }finally{
	        if(scanner != null){
	            scanner.close();
	        }
	    }
	    return isMounted;
	}
}