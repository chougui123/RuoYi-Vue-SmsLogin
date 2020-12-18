package com.ruoyi.framework.web.service;

import javax.annotation.Resource;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.smsConfig.SmsCodeAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;

import java.util.Map;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    /**
     * 登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid)
    {
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
            throw new CaptchaException();
        }
        // 用户验证
        Authentication authentication = null;
        try
        {
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new CustomException(e.getMessage());
            }
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        // 生成token
        return tokenService.createToken(loginUser);
    }

    /**
     * 手机号登录验证
     *
     * @param mobile 手机号
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */

    public AjaxResult smsLogin(String mobile, String code, String uuid)
    {

        // 用户验证
        Authentication authentication = null;
        try
        {
            checkSmsCode(mobile,code,uuid);

            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager
                    .authenticate(new SmsCodeAuthenticationToken(mobile));
        }
        catch (Exception e)
        {

                AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_FAIL, e.getMessage()));
                throw new CustomException(e.getMessage());

        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(mobile, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        AjaxResult ajax = AjaxResult.success();

        // 生成token
        String token = tokenService.createToken(loginUser);
        ajax.put(Constants.TOKEN, token);
        return  ajax;
    }

    /**
     * 检查手机号登录
     * @param
     */
    private void checkSmsCode(String mobile,String inputCode, String uuid) {

        String verifyKey = Constants.SMS_CAPTCHA_CODE_KEY + uuid;

        Map<String, Object> smsCode =  redisCache.getCacheObject(verifyKey);
//        redisCache.deleteObject(verifyKey);
        if(StringUtils.isEmpty(inputCode)){
            throw new BadCredentialsException("验证码不能为空");
        }

        if(smsCode == null) {
            throw new BadCredentialsException("验证码失效");
        }

        String applyMobile = (String) smsCode.get("mobile");
        int code = (int) smsCode.get("code");

        if(!applyMobile.equals(mobile)) {
            throw new BadCredentialsException("手机号码不一致");
        }
        if(code != Integer.parseInt(inputCode)) {
            throw new BadCredentialsException("验证码错误");
        }
    }
}
