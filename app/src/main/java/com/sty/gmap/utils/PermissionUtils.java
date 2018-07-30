package com.sty.gmap.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.sty.gmap.BuildConfig;

/**
 * @Author tian
 * @Date 17/1/18
 */
public class PermissionUtils {

    public static final int REQUEST_PERMISSIONS_CODE = 23654;

    /**
     * 检测有没有权限
     * @param activity
     * @param permissions
     * @return
     */
    public static boolean checkSelfPermission(Activity activity, String... permissions){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            boolean checkSelfPermissionResult = false;

            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(activity,permissions[i])
                        != PackageManager.PERMISSION_GRANTED){
                    checkSelfPermissionResult = true;
                    break;
                }
            }
            return !checkSelfPermissionResult;
        }else{
            return true;
        }
    }

    /**
     * 请求权限
     * @param activity
     * @param permissions
     * @return 返回true代表已经有这个权限了，不需要请求，false代表会请求
     */
    public static boolean requestPermissions(Activity activity, String... permissions){

        if (checkSelfPermission(activity,permissions)){

            return true;
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            activity.requestPermissions(permissions,REQUEST_PERMISSIONS_CODE);
            return false;
        }else{
            return true;
        }
    }

    public static void showPermissionDialog(final Activity context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("请手动打开权限");
        builder.setMessage("点确认后将跳转到应用详情界面,请在「权限管理」中打开权限");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                //跳转到应用详情
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", BuildConfig.APPLICATION_ID, null));
                try {
                    context.startActivity(intent);
                }catch (Exception e){
                    Log.e("questPermission", "onClickConfirm startActivity error ");
                }
            }
        });
        builder.setNegativeButton("重试", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();

    }

    /**
     * 请求这个权限返回结果
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public static void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults,final HandlerPermissionsCallback callback){
        if (requestCode == REQUEST_PERMISSIONS_CODE){
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (callback!=null){
                    callback.onSuccess(permissions);
                }

            } else {

                if (callback!=null){
                    callback.onFailure(permissions);
                }

            }
        }
    }


    public interface HandlerPermissionsCallback{

        void onSuccess(String[] permissions);

        void onFailure(String[] permissions);

    }




}
