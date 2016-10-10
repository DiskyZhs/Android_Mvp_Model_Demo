package cn.com.egova.securities_police.ui.Login;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import cn.com.egova.securities_police.R;
import cn.com.egova.securities_police.ui.BaseActivity;
import cn.com.egova.securities_police.ui.widget.CustomImageEditText;
import cn.com.egova.securities_police.ui.widget.ProgressDialog;
import cn.com.egova.securities_police.ui.widget.RemoteDealEnsurenPopupWindow;

public class LoginActivity extends BaseActivity implements View.OnClickListener, LoginContract.View {
    public static final String TAG = "LoginActivity";
    //UI
    private TextView mRegisterText;
    private TextView mPasswordForgetText;
    private Button mLoginBtn;
    private CustomImageEditText mPhoneNoEdit;
    private CustomImageEditText mPasswordEdit;
    private ProgressDialog mProgressDialog;
    private ImageView mQqLoginImg;
    private ImageView mWcLoginImg;
    private ImageView mAilpayLoginImg;
    private View mContianer;
    private RemoteDealEnsurenPopupWindow mThirdLoginEnsureWindow;

    //MVP Presenter
    private LoginContract.Presenter mLoginPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);
        //申明Presenter
        new LoginPresenter(this);
        //申明View
        initView();
        //初始化Umeng登录
        mLoginPresenter.initUmengLogin();
    }

    public void initView() {
        //控件优化
        mContianer = findViewById(R.id.login_activity_container);
        mRegisterText = (TextView) findViewById(R.id.login_activity_register_text);
        mRegisterText.setOnClickListener(this);
        mPasswordForgetText = (TextView) findViewById(R.id.login_activity_password_forget_text);
        mPasswordForgetText.setOnClickListener(this);
        mLoginBtn = (Button) findViewById(R.id.login_activity_login_btn);
        mLoginBtn.setOnClickListener(this);
        mPhoneNoEdit = (CustomImageEditText) findViewById(R.id.login_activity_phone_number_edit);
        mPasswordEdit = (CustomImageEditText) findViewById(R.id.login_activity_password_edit);
        mPhoneNoEdit.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
        mPasswordEdit.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPhoneNoEdit.getIcon().setImageResource(R.mipmap.edit_icon_phone_num);
        mPasswordEdit.getIcon().setImageResource(R.mipmap.edit_icon_password);
        mProgressDialog = new ProgressDialog(this);

        //第三方登录
        mQqLoginImg = (ImageView) findViewById(R.id.login_activity_qq_icon);
        mWcLoginImg = (ImageView) findViewById(R.id.login_activity_wc_icon);
        mAilpayLoginImg = (ImageView) findViewById(R.id.login_activity_alipay_icon);
        mQqLoginImg.setOnClickListener(this);
        mWcLoginImg.setOnClickListener(this);
        mAilpayLoginImg.setOnClickListener(this);

        //初始化登录信息
        mLoginPresenter.initLoginInfo();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_activity_register_text:
                mLoginPresenter.register();
                break;
            case R.id.login_activity_password_forget_text:
                mLoginPresenter.forgetPassword();
                break;
            case R.id.login_activity_login_btn:
                mLoginPresenter.userNameLogin();
                break;
            case R.id.login_activity_qq_icon:
                //友盟QQ登陆
                mLoginPresenter.qqLogin();
                break;
            case R.id.login_activity_wc_icon:
                //跳转到微信授权界面
                mLoginPresenter.wxLogin();
                break;
            case R.id.login_activity_alipay_icon:
                //新浪微博登陆
                mLoginPresenter.sinaLogin();
                break;
            default:
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //友盟QQ授权登陆
        mLoginPresenter.umengAuthActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoginPresenter.start();
    }

    @Override
    public void showLoading() {
        mProgressDialog.show();
    }

    @Override
    public void dismissLoading() {
        mProgressDialog.dismiss();
    }

    @Override
    public void setPassword(String password) {
        mPasswordEdit.getEditText().setText(password);
    }

    @Override
    public void setUserName(String userName) {
        mPhoneNoEdit.getEditText().setText(userName);
    }

    @Override
    public void setUserNameSelection(int index) {
        mPhoneNoEdit.getEditText().setSelection(index);
    }

    @Override
    public void setPasswordSelection(int index) {
        mPasswordEdit.getEditText().setSelection(index);
    }

    @Override
    public String getUserName() {
        return String.valueOf(mPhoneNoEdit.getEditText().getText());
    }

    @Override
    public String getPassword() {
        return String.valueOf(mPasswordEdit.getEditText().getText());
    }

    @Override
    public RemoteDealEnsurenPopupWindow getThirdEnsureWindow() {
        return mThirdLoginEnsureWindow;
    }

    @Override
    public void setThirdEnsureWindow(RemoteDealEnsurenPopupWindow thirdEnsureWindow) {
        mThirdLoginEnsureWindow = thirdEnsureWindow;
    }

    @Override
    public void showThirdEnsureWindow() {
        if (mThirdLoginEnsureWindow != null) {
            mThirdLoginEnsureWindow.showPopupWindow(mContianer);
        }
    }

    @Override
    public void dismissThirdEnsureWindow() {
        if (mThirdLoginEnsureWindow != null) {
            mThirdLoginEnsureWindow.dismiss();
        }
    }

    @Override
    public void setPresenter(LoginContract.Presenter presenter) {
        mLoginPresenter = presenter;
    }

}

