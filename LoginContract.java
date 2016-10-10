package cn.com.egova.securities_police.ui.Login;

import android.content.Intent;

import cn.com.egova.securities_police.mvp.BasePresenter;
import cn.com.egova.securities_police.mvp.BaseView;
import cn.com.egova.securities_police.ui.widget.RemoteDealEnsurenPopupWindow;

/**
 * Created by ZhangHaoSong on 2016/10/09 0009.
 */

public class LoginContract {

    interface View extends BaseView<Presenter> {
        /**
         * 显示Loading对话框
         */
        void showLoading();

        void dismissLoading();

        /**
         * 设置密码
         * @param password
         */
        void setPassword(String password);

        /**
         * 设置用户名
         * @param userName
         */
        void setUserName(String userName);

        /**
         * 设置用户名Edit的光标位置
         * @param index
         */
        void setUserNameSelection(int index);

        void setPasswordSelection(int index);

        String getUserName();

        String getPassword();

        RemoteDealEnsurenPopupWindow getThirdEnsureWindow();

        void setThirdEnsureWindow(RemoteDealEnsurenPopupWindow thirdEnsureWindow);

        /**
         * 显示第三方登录授权结果对话框
         */
        void showThirdEnsureWindow();

        void dismissThirdEnsureWindow();
    }

    interface Presenter extends BasePresenter {
        /**
         * 利用用户名登录
         */
        void userNameLogin();

        /**
         * qq第三方登录
         */
        void qqLogin();

        void wxLogin();

        void sinaLogin();

        /**
         * 忘记密码
         */
        void forgetPassword();

        /**
         * 注册新用户
         */
        void register();

        /**
         * 初始化UserName以及Password（账户密码存储）
         */
        void initLoginInfo();

        /**
         * 配置友盟第三方登录
         */
        void initUmengLogin();

        void umengAuthActivityResult(int requestCode, int resultCode, Intent data);
    }
}
