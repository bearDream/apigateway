package cn.beardream.apigateway.filter;

import cn.beardream.apigateway.remote.IUserServerRemote;
import cn.beardream.apigateway.utils.TokenUtil;
import cn.beardream.core_common.constant.SysConstant;
import cn.beardream.core_common.model.ResponseBody;
import cn.beardream.core_common.model.TokenModel;
import cn.beardream.core_common.model.User;
import cn.beardream.core_common.utils.ResponseBodyUtil;
import cn.beardream.core_common.utils.TextUtils;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import javafx.util.Pair;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 2017/10/24
 * zuul过滤器，进行登陆权限校验
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Component
public class FinishLoginFilter extends SendResponseFilter {

    Logger logger = LoggerFactory.getLogger(FinishLoginFilter.class);

    @Autowired
    private TokenUtil mTokenUtil;

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        String uri = context.getRequest().getRequestURI();
        logger.info("uri ==========>>>>>>> {}", uri);
        // 如果是登陆的请求则需要设置token
        if (TextUtils.strCompare(uri, SysConstant.LOGINURI)){
            return true;
        }
        return false;
    }

    /**
     * 用户登陆成功后设置token
     * 若用户登陆失败(系统错误或用户名密码输入错误)则不会设置token
     * @return
     */
    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        context.getResponse().setContentType("text/json;charset=UTF-8");
        HttpServletRequest request = context.getRequest();
        InputStream in = context.getResponseDataStream();
        try {
            String s = IOUtils.toString(in,"utf-8");
            ResponseBody userBody = JSON.parseObject(s, ResponseBody.class);
            // 表明登陆成功, 需要设置token
            if (userBody.getCode().equals(1000)){
                User user = JSON.parseObject(userBody.getData().toString(), User.class);
                String token = mTokenUtil.generateToken(user);
                mTokenUtil.setToken(new TokenModel(token, user.getUserId(), System.currentTimeMillis()));
                successResponse(s, token, context);

                TokenModel deToken = mTokenUtil.checkToken(token);
                logger.info("debug: token ======>>>>{}----  userId:  ---{}", deToken.getToken(), deToken.getUserId());
                return null;
            }
            logger.info("s-============>: {}", s);
            errorResponse("登陆失败，" + userBody.getData(), context);
        } catch (IOException e) {
            logger.error("zuul处理登陆出错:{}", e.getLocalizedMessage());
            errorResponse(e.getLocalizedMessage(), context);
        } catch (Exception e1){
            logger.error("zuul处理登陆出错:{}", e1.getLocalizedMessage());
            errorResponse(e1.getLocalizedMessage(), context);
        }
        return null;
    }

    private Object errorResponse(String e, RequestContext context){
        context.setSendZuulResponse(false);
        context.setResponseBody(JSON.toJSONString(ResponseBodyUtil.error("zuul路由解析出错,原因:"+e)));
        context.setResponseStatusCode(500);
        return null;
    }

    private Object successResponse(String s, String token, RequestContext context){
        context.setSendZuulResponse(false);
        context.setResponseBody(s);
        context.setResponseStatusCode(200);
        HttpServletResponse response = context.getResponse();
        response.addHeader("Authrization", token);
        return null;
    }

}
