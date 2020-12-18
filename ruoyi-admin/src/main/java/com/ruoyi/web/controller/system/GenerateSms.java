package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.uuid.IdUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Api("生成验证码接口")
@Controller
public class GenerateSms {
    @Autowired
    private RedisCache redisCache;

    private Logger logger = LoggerFactory.getLogger(getClass());


    @ApiOperation("生成验证码")
    @ApiImplicitParam(name = "mobile",value = "手机号码",required = true,dataType = "String",paramType = "query")
    @PostMapping("/sms/code")
    @ResponseBody
    public AjaxResult sms(@RequestBody LoginBody loginBody) {

        String mobile=loginBody.getMobile();
        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = Constants.SMS_CAPTCHA_CODE_KEY + uuid;

        int code = (int) Math.ceil(Math.random() * 9000 + 1000);
        Map<String, Object> map = new HashMap<>(16);
        map.put("mobile", mobile);
        map.put("code", code);

        redisCache.setCacheObject(verifyKey, map, Constants.SMS_EXPIRATION, TimeUnit.MINUTES);
//        session.setAttribute("smsCode", map);

        logger.info(" 为 {} 设置短信验证码：{}", mobile, code);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("uuid", uuid);
        return ajax;
    }

}
