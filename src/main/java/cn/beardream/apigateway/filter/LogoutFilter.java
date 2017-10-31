package cn.beardream.apigateway.filter;

import cn.beardream.apigateway.utils.TokenUtil;
import cn.beardream.core_common.constant.SysConstant;
import cn.beardream.core_common.model.TokenModel;
import cn.beardream.core_common.utils.ResponseBodyUtil;
import cn.beardream.core_common.utils.TextUtils;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 2017/10/24
 * zuul过滤器，进行登陆权限校验
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Component
public class LogoutFilter extends ZuulFilter {

    @Autowired
    private TokenUtil mTokenUtil;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        String uri = context.getRequest().getRequestURI();
        // 如果是登陆的请求则不需要鉴权
        if (TextUtils.strCompare(uri, SysConstant.LOGOUTURI)){
            return true;
        }
        return false;
    }

    /**
     * 1) 先校验token（若路径为/user/login则不进行校验）
     *  1.1)token不存在，直接返回401
     *  1.2)token存在，通过TokenUtil进行校验
     * @return
     */
    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        context.getResponse().setContentType("text/json;charset=UTF-8");
        HttpServletRequest request = context.getRequest();

        String accessToken = request.getHeader("Authrization");
        if (accessToken == null){
            unauthorized(context);
        }
        // 通过user-server进行校验是否登陆, 若已登陆则刷新token并继续路由，否则返回401
        if (accessToken != null){
            mTokenUtil.delToken(accessToken);
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(200);
            context.setResponseBody(JSON.toJSONString(ResponseBodyUtil.success("注销成功")));
        }
        return null;
    }

    public void unauthorized(RequestContext context){
        context.setSendZuulResponse(false);
        context.setResponseBody(JSON.toJSONString(ResponseBodyUtil.unauthorized("未登陆")));
        context.setResponseStatusCode(401);
    }

}
