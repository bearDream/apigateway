package cn.beardream.apigateway.utils;

import cn.beardream.core_common.exception.TokenException;
import cn.beardream.core_common.model.TokenModel;
import cn.beardream.core_common.model.User;
import cn.beardream.core_common.utils.MD5Utils;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 2017/10/27
 * token的验证和生成
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Component
public class TokenUtil {

    @Autowired
    private StringRedisTemplate mStringRedisTemplate;

    Logger log = LoggerFactory.getLogger("TokenUtil.class");

    /**
     * 检查token是否过期，如果未过期则刷新
     * @param token
     * @return
     */
    public TokenModel checkToken(@NotNull String token){
        if (token == null){
            throw new TokenException("检查token出错,token为空");
        }
        boolean isExisted = mStringRedisTemplate.hasKey(token);

        TokenModel tokenModel = new TokenModel();
        if (isExisted){
            Object v = mStringRedisTemplate.opsForValue().get(token);
            TokenModel t = JSON.parseObject(String.valueOf(v), TokenModel.class);
            // token没有过期才可返回
            log.info("expired ===========>   {}", mStringRedisTemplate.getExpire(t.getToken(), TimeUnit.HOURS));
            if (mStringRedisTemplate.getExpire(t.getToken(), TimeUnit.SECONDS) > 0){
                // 顺便刷新token
                refreshToken(t.getToken());
                tokenModel.setToken(t.getToken());
                tokenModel.setExpired(t.getExpired());
                tokenModel.setUserId(t.getUserId());
                return tokenModel;
            }else {
                // 该token过期返回空并删除
                mStringRedisTemplate.delete(token);
                return null;
            }
        }
        // 该token不存在则返回空
        return null;
    }

    public TokenModel setToken(TokenModel model){
        if (model.getUserId() == null || model.getToken() == null){
            throw new TokenException("设置token出错, token或userId为空");
        }
        // 设置过期时间为12小时
        model.setExpired(System.currentTimeMillis() + 43200000L);
        mStringRedisTemplate.opsForValue().set(model.token, JSON.toJSONString(model), 12, TimeUnit.HOURS);
        return model;
    }

    public void delToken(String token){
        // 设置过期时间为12小时
        mStringRedisTemplate.delete(token);
    }

    public TokenModel refreshToken(String token){
        if (token == null){
            throw new TokenException("刷新token出错, token为空");
        }
        String var1 = mStringRedisTemplate.opsForValue().get(token);
        TokenModel tokenModel = JSON.parseObject(var1, TokenModel.class);

        tokenModel.setExpired(System.currentTimeMillis() + 43200000L);
        mStringRedisTemplate.opsForValue().set(token, JSON.toJSONString(tokenModel), 12, TimeUnit.HOURS);
        return tokenModel;
    }

    public String generateToken(User user){
        // 根据 userId + password + 当前时间 + 随机字符串
        StringBuffer token = new StringBuffer();
        token.append(user.getUserId());
        token.append(user.getPassword());
        token.append(System.currentTimeMillis());
        token.append(UUID.randomUUID());
        return MD5Utils.GetMD5Code(token.toString());
    }
}
