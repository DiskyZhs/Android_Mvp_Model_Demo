package cn.com.egova.securities_police.mvp;

/**
 * Created by ZhangHaoSong on 2016/10/09.
 */

public interface BaseView<T> {
    //用于传递给View Presenter来操作(这么设计是有时候把Fragment作为View时)
    void setPresenter(T presenter);
}