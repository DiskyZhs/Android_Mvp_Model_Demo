package cn.com.egova.securities_police.ui.Login;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import cn.com.egova.securities_police.SecurityApplication;
import cn.com.egova.securities_police.model.entity.InsuranceCompany;
import cn.com.egova.securities_police.model.entity.LoginError;
import cn.com.egova.securities_police.model.entity.PlateType;
import cn.com.egova.securities_police.model.entity.ThirdLoginReply;
import cn.com.egova.securities_police.model.entity.User;
import cn.com.egova.securities_police.model.entity.UserType;
import cn.com.egova.securities_police.model.http.CustomAsyncHttpHandler;
import cn.com.egova.securities_police.model.http.CustomJsonHttpHanlder;
import cn.com.egova.securities_police.model.http.TrafficAccidentDealHttpClient;
import cn.com.egova.securities_police.model.util.LogUtil;
import cn.com.egova.securities_police.model.util.SharedPreferenceUtil;
import cn.com.egova.securities_police.model.util.ToastUtil;
import cn.com.egova.securities_police.ui.activities.MainActivity;
import cn.com.egova.securities_police.ui.activities.PasswordForgotActivity;
import cn.com.egova.securities_police.ui.activities.RegisterActivity;
import cn.com.egova.securities_police.ui.widget.RemoteDealEnsurenPopupWindow;

/**
 * Created by ZhangHaoSong on 2016/10/09 0009.
 */

public class LoginPresenter implements LoginContract.Presenter {
    //TAG
    public static final String TAG = "LoginPresenter";

    //Activity View
    private LoginActivity mRootView;

    //友盟
    private UMShareAPI mShareAPI;
    public final static String THIRD_LOGIN_TYPE_QQ = "policeqq";
    public final static String THIRD_LOGIN_TYPE_WX = "policeweixin";
    public final static String THIRD_LOGIN_TYPE_SINA = "sina";
    //第三方登陆跳转参数
    public final String INTENT_THIRD_USER_ID = "userId";

    //存储部分登陆信息
    private String mAccessToken;
    public String userName = "";
    public String passWord = "";
    private User mUser;

    /**
     * 跳转数据传输
     */
    public final static String INTENT_BUNDLE_KEY_USER = "user";

    /**
     * persenter初始化就是获取到View来操作View <br>
     * 同时将presenter传递给View<br>
     * 建立presenter与View的联系<br>
     *
     * @param rootView
     */
    public LoginPresenter(LoginContract.View rootView) {
        //获取View
        if (rootView instanceof LoginActivity)
            mRootView = (LoginActivity) rootView;
        //关联View和Presenter
        mRootView.setPresenter(this);
    }

    /**
     * 用户名密码输入
     */
    @Override
    public void userNameLogin() {
        //检验输入
        if (mRootView.getUserName().length() == 0) {
            ToastUtil.showText(mRootView, "用户名未输入", Toast.LENGTH_SHORT);
            return;
        }
        if (mRootView.getPassword().length() == 0) {
            ToastUtil.showText(mRootView, "密码未输入", Toast.LENGTH_SHORT);
            return;
        }

        //为获取到AccessToken或者userName以及Password不对时
        if (mAccessToken == null || (!userName.equals(mRootView.getUserName())) || (!passWord.equals(mRootView.getPassword()))) {
            userName = mRootView.getUserName();
            passWord = mRootView.getPassword();
            LogUtil.e(TAG, "to get mAccessToken");
            //去请求AccessToken(包含DeviceToken)
            TrafficAccidentDealHttpClient.requestAccessToken(userName, passWord, ((SecurityApplication) mRootView.getApplication()).device_token, new RequestAccessTokenResponseHandler(mRootView));
        } else {
            LogUtil.e(TAG, "to get user mAccessToken =" + mAccessToken);
            //存储用户名和密码
            SharedPreferenceUtil.saveUserLoginInfo(mRootView.getUserName(), mRootView.getPassword(), mRootView);
            //去请求用户信息
            TrafficAccidentDealHttpClient.getUser(mAccessToken, new GetUserResponseHandler(mRootView));
        }
        mRootView.showLoading();
    }

    /**
     * 友盟QQ登陆
     */
    @Override
    public void qqLogin() {
        SHARE_MEDIA platformQQ = SHARE_MEDIA.QQ;
        mShareAPI.doOauthVerify(mRootView, platformQQ, new UMThirdAuthListener(mRootView, THIRD_LOGIN_TYPE_QQ));
    }

    /**
     * 友盟微信登陆
     */
    @Override
    public void wxLogin() {
        SHARE_MEDIA platformQQ = SHARE_MEDIA.WEIXIN;
        mShareAPI.doOauthVerify(mRootView, platformQQ, new UMThirdAuthListener(mRootView, THIRD_LOGIN_TYPE_WX));
    }

    /**
     * 友盟sina登陆
     */
    @Override
    public void sinaLogin() {
        SHARE_MEDIA platformQQ = SHARE_MEDIA.SINA;
        mShareAPI.doOauthVerify(mRootView, platformQQ, new UMThirdAuthListener(mRootView, THIRD_LOGIN_TYPE_SINA));
    }

    /**
     * 忘记密码，重置密码
     */
    @Override
    public void forgetPassword() {
        mRootView.startActivity(new Intent(mRootView, PasswordForgotActivity.class));
    }

    /**
     * 注册新用户
     */
    @Override
    public void register() {
        mRootView.startActivity(new Intent(mRootView, RegisterActivity.class));
    }

    /**
     * 初始化登陆的信息(账号密码)
     */
    @Override
    public void initLoginInfo() {
        if (SharedPreferenceUtil.isUserNameSaved(mRootView)) {
            mRootView.setUserName(SharedPreferenceUtil.getUserName(mRootView));
            mRootView.setUserNameSelection(SharedPreferenceUtil.getUserName(mRootView).length());
        }
        if (SharedPreferenceUtil.isUserNameSaved(mRootView))
            mRootView.setPassword(SharedPreferenceUtil.getPassword(mRootView));
    }

    /**
     * UM第三方登初始化
     */
    @Override
    public void initUmengLogin() {
        //友盟请求权限
        askUmengPermission();
        //获取友盟API
        mShareAPI = UMShareAPI.get(mRootView);
    }

    /**
     * UM第三方登陆授权页面返回（写于onActivityResult()生命周期）
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void umengAuthActivityResult(int requestCode, int resultCode, Intent data) {
        mShareAPI.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 使用于OnResume，用于恢复现场
     */
    @Override
    public void start() {
        //Nothing to do
    }

    /**
     * 请求友盟的第三方登录所需要的权限
     */
    private void askUmengPermission() {
        String[] mPermissionList = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CALL_PHONE, Manifest.permission.READ_LOGS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SET_DEBUG_APP, Manifest.permission.SYSTEM_ALERT_WINDOW, Manifest.permission.GET_ACCOUNTS};
        ActivityCompat.requestPermissions(mRootView, mPermissionList, 100);
    }

    /**
     * 用户名登录请求AccessToken回调
     */
    private class RequestAccessTokenResponseHandler extends CustomJsonHttpHanlder {
        private Context context;

        public RequestAccessTokenResponseHandler(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            super.onSuccess(statusCode, headers, response);
            LogUtil.e(TAG, "response = " + response.toString());
            //获取AccessToken
            try {
                mAccessToken = response.getString("access_token");
                LogUtil.e(TAG, "RequestAccessTokenResponseHandler access_token = " + mAccessToken);
                //去请求用户信息
                TrafficAccidentDealHttpClient.getUser(mAccessToken, new GetUserResponseHandler(context));
                //存储用户名和密码
                SharedPreferenceUtil.saveUserLoginInfo(mRootView.getUserName(), mRootView.getPassword(), mRootView);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            super.onFailure(statusCode, headers, responseString, throwable);
            LogUtil.e(TAG, "RequestAccessTokenResponseHandler1 onFailure throwable msg = " + responseString);
            ToastUtil.showText(context, "登录失败，请再确认网络通畅后再次进行登陆," + throwable.getMessage(), Toast.LENGTH_SHORT);
            mRootView.dismissLoading();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            super.onFailure(statusCode, headers, throwable, errorResponse);
            if (null != errorResponse) {
                LogUtil.e(TAG, "RequestAccessTokenResponseHandler onFailure responseString = " + errorResponse.toString());
                if (new Gson().fromJson(errorResponse.toString(), LoginError.class).getError_description().equals(LoginError.ERROR_USERNAME_PASSWORD)) {
                    ToastUtil.showText(context, "用户名或密码错误", Toast.LENGTH_SHORT);
                    mRootView.dismissLoading();
                    return;
                }
            }
            LogUtil.e(TAG, "RequestAccessTokenResponseHandler2 onFailure throwable msg = " + throwable.getMessage());
            ToastUtil.showText(context, "登录失败，请再确认网络通畅后再次进行登陆," + throwable.getMessage(), Toast.LENGTH_SHORT);
            mRootView.dismissLoading();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
            super.onFailure(statusCode, headers, throwable, errorResponse);
            if (null != errorResponse)
                LogUtil.e(TAG, "RequestAccessTokenResponseHandler3 onFailure responseString = " + errorResponse.toString());
            LogUtil.e(TAG, "RequestAccessTokenResponseHandler3 onFailure throwable msg = " + throwable.getMessage());
            ToastUtil.showText(context, "登录失败，请再确认网络通畅后再次进行登陆," + throwable.getMessage(), Toast.LENGTH_SHORT);
            mRootView.dismissLoading();
        }
    }


    private class GetUserResponseHandler extends CustomAsyncHttpHandler {
        private Context context;

        public GetUserResponseHandler(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public void onSuccess(int i, Header[] headers, byte[] bytes) {
            //序列化
            LogUtil.e(TAG, "GetUserResponseHandler onSuccess response = " + new String(bytes));
            //判断是否登陆成功
            mUser = new Gson().fromJson(new String(bytes), User.class);
            mUser.setmAccessToken(mAccessToken);
            LogUtil.e(TAG, "mUser = " + mUser.toString());
            //去获取保险公司信息
            if (InsuranceCompany.getInstanceList().size() == 0)
                TrafficAccidentDealHttpClient.getInsurances(new GetInsurancesResponseHandler(mRootView));
            else {
                //跳转
                if (PlateType.getPlateTypeList().size() == 0) {
                    TrafficAccidentDealHttpClient.getPlateType(new GetPlateTypeResponseHandler(mRootView));
                } else {
                    //跳转
                    mRootView.dismissLoading();
                    //跳转到主页(把用户作为)
                    Intent intent = new Intent(mRootView, MainActivity.class);
                    Bundle b = new Bundle();
                    b.putParcelable(INTENT_BUNDLE_KEY_USER, mUser);
                    intent.putExtras(b);
                    mRootView.startActivity(intent);
                    //关闭当前Activity
                    mRootView.finish();
                }
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
            LogUtil.e(TAG, "GetUserResponseHandler onFailure responseString = " + throwable.getMessage());
            ToastUtil.showText(context, "获取用户信息失败，请再确认网络通畅后再次进行登录," + throwable.getMessage(), Toast.LENGTH_SHORT);
            mRootView.dismissLoading();
        }
    }

    /**
     * 获取保险公司Http请求结果的回调
     */
    private class GetInsurancesResponseHandler extends CustomJsonHttpHanlder {
        private Context context;

        public GetInsurancesResponseHandler(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            super.onSuccess(statusCode, headers, response);
            mRootView.dismissLoading();
            //获取保险公司信息
            try {
                JSONArray companyResult = response.getJSONArray("result");
                LogUtil.e("GetInsurancesResponseHandler", " GetInsurancesResponseHandler onSuccess JSONObject reply=" + companyResult.toString());
                Gson gson = new Gson();
                ArrayList<InsuranceCompany> mInsuranceCompanyList = new ArrayList<InsuranceCompany>();
                mInsuranceCompanyList = gson.fromJson(companyResult.toString(), new TypeToken<ArrayList<InsuranceCompany>>() {
                }.getType());
                //将赋值给单例
                if (InsuranceCompany.getInstanceList().size() == 0) {
                    for (InsuranceCompany item : mInsuranceCompanyList)
                        InsuranceCompany.getInstanceList().add(item);
                }
                //获取车辆类型信息
                if (PlateType.getPlateTypeList().size() == 0) {
                    TrafficAccidentDealHttpClient.getPlateType(new GetPlateTypeResponseHandler(context));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            super.onFailure(statusCode, headers, responseString, throwable);
            mRootView.dismissLoading();
            ToastUtil.showText(context, "获取保险公司信息失败，请再确认网络通畅后再次进行登录", Toast.LENGTH_SHORT);
            //finish();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            super.onFailure(statusCode, headers, throwable, errorResponse);
            mRootView.dismissLoading();
            ToastUtil.showText(context, "获取保险公司信息失败，请再确认网络通畅后再次进行登录", Toast.LENGTH_SHORT);
            //finish();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
            super.onFailure(statusCode, headers, throwable, errorResponse);
            mRootView.dismissLoading();
            ToastUtil.showText(context, "获取保险公司信息失败，请再确认网络通畅后再次进行登录", Toast.LENGTH_SHORT);
            //finish();
        }
    }

    /**
     * 获取车辆类型Http请求结果的回调
     */
    private class GetPlateTypeResponseHandler extends CustomAsyncHttpHandler {
        private Context context;

        public GetPlateTypeResponseHandler(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public void onSuccess(int i, Header[] headers, byte[] bytes) {
            super.onSuccess(i, headers, bytes);
            LogUtil.e(TAG, "GetPlateTypeResponseHandler onSuccess  reply=" + new String(bytes));
            Gson gson = new Gson();
            ArrayList<PlateType.PlateTypeInfo> mPlateTypeList = new ArrayList<PlateType.PlateTypeInfo>();
            mPlateTypeList = gson.fromJson(new String(bytes), new TypeToken<ArrayList<PlateType.PlateTypeInfo>>() {
            }.getType());
            //将赋值给单例
            if (PlateType.getPlateTypeList().size() == 0) {
                for (PlateType.PlateTypeInfo item : mPlateTypeList)
                    PlateType.getPlateTypeList().add(item);
            }
            if (PlateType.getPlateTypeList().size() != 0) {
                //跳转
                mRootView.dismissLoading();
                //跳转到主页(把用户作为)
                Intent intent = new Intent(context, MainActivity.class);
                Bundle b = new Bundle();
                b.putParcelable(INTENT_BUNDLE_KEY_USER, mUser);
                intent.putExtras(b);
                context.startActivity(intent);
                //关闭当前Activity
                ((LoginActivity) context).finish();
            } else {
                ToastUtil.showText(context, "获取车辆类型信息失败，请再确认网络通畅后再次进行登录", Toast.LENGTH_SHORT);
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
            super.onFailure(i, headers, bytes, throwable);
            mRootView.dismissLoading();
            LogUtil.e(TAG, "GetPlateTypeResponseHandler onFailure  reply=" + throwable.getMessage());
            ToastUtil.showText(context, "获取车辆类型信息失败，请再确认网络通畅后再次进行登录", Toast.LENGTH_SHORT);
            //finish();
        }
    }

    /**
     * 友盟第三方登录回调
     */
    private class UMThirdAuthListener implements UMAuthListener {

        private Context context;
        private String loginType;

        public UMThirdAuthListener(Context context, String loginType) {
            this.loginType = loginType;
            this.context = context;
        }

        @Override
        public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
            mRootView.showLoading();
            //去获取AccessToken
            TrafficAccidentDealHttpClient.getAccessTokenFromThird(map.get("openid"), loginType, map.get("access_token"), UserType.USER_TYPE_DEFAULT, new GetAccessTokenFromThirdResponseHandler(context, loginType));

        }

        @Override
        public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
            ToastUtil.showText(context.getApplicationContext(), "授权错误，请稍后重试", Toast.LENGTH_SHORT);
        }

        @Override
        public void onCancel(SHARE_MEDIA share_media, int i) {
            ToastUtil.showText(context.getApplicationContext(), "授权错误，请稍后重试", Toast.LENGTH_SHORT);
        }
    }

    /**
     * 第三方登录获取AccessToken
     */
    private class GetAccessTokenFromThirdResponseHandler extends CustomAsyncHttpHandler {

        private Context context;
        private String loginType;

        public GetAccessTokenFromThirdResponseHandler(Context context, String loginType) {
            super(context);
            this.loginType = loginType;
            this.context = context;
        }

        @Override
        public void onSuccess(int i, Header[] headers, byte[] bytes) {
            LogUtil.e(TAG, "onSuccess response = " + new String(bytes));
            final ThirdLoginReply mLoginReply = new Gson().fromJson(new String(bytes), ThirdLoginReply.class);
            LogUtil.e(TAG, "getAccessTokenFromThird onSuccess mLoginReply =" + mLoginReply.toString());
            //赋值AccessToken
            mAccessToken = mLoginReply.access_token;
            mRootView.dismissLoading();
            if (mLoginReply.isNewUser) { //还未注册
                //显示确认对话框
                if (mRootView.getThirdEnsureWindow() == null) {
                    mRootView.setThirdEnsureWindow(new RemoteDealEnsurenPopupWindow(context));
                    mRootView.getThirdEnsureWindow().setTitle("授权成功");
                    mRootView.getThirdEnsureWindow().setMessage("该账户之前未注册，请完成账户信息");
                    mRootView.getThirdEnsureWindow().setEnsureOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //跳转到注册界面
                            //需要复制UserId
                            Intent intent = new Intent(context, RegisterActivity.class);
                            intent.putExtra(INTENT_THIRD_USER_ID, mLoginReply.user.id);
                            context.startActivity(intent);
                            mRootView.dismissThirdEnsureWindow();
                        }
                    });
                }
                mRootView.showThirdEnsureWindow();
            } else {//已经注册
                //去请求用户信息
                mRootView.dismissLoading();
                TrafficAccidentDealHttpClient.getUser(mAccessToken, new GetUserResponseHandler(context));
            }
        }

        @Override
        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
            if (bytes != null)
                LogUtil.e(TAG, "response = " + new String(bytes));
            else
                LogUtil.e(TAG, "response = " + throwable.toString());
            mRootView.dismissLoading();
            ToastUtil.showText(context.getApplicationContext(), "第三方登录失败，请稍后重试", Toast.LENGTH_SHORT);
        }
    }

}
