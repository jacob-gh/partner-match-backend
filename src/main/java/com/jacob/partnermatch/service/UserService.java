package com.jacob.partnermatch.service;

import com.jacob.partnermatch.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 *
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);


    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签查询用户 内存查询
     * @param tags
     * @return
     */
    List<User> searchUserByTags(List<String> tags);
    /**
     * 根据标签查询用户 SQL查询
     * @param tags
     * @return
     */
    List<User> searchUserByTagsBySql(List<String> tags);

    /**
     * 更新用户
     * @param user 新用户信息
     * @param request 当前请求
     * @return
     */
    Integer upadeteUser(User user,HttpServletRequest request);

    /**
     * 获取当前登录的用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 判断当前用户是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);
    boolean isAdmin(User user);

    List<User> matchUsers(long num, User user);

}
