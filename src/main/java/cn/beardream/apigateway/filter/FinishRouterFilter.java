package cn.beardream.apigateway.filter;

import cn.beardream.core_common.constant.SysConstant;
import cn.beardream.core_common.model.ResponseBody;
import cn.beardream.core_common.utils.MD5Utils;
import cn.beardream.core_common.utils.ResponseBodyUtil;
import cn.beardream.core_common.utils.TextUtils;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * 2017/10/31
 * 路由完成的过滤器
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Component
public class FinishRouterFilter extends ZuulFilter {

    Logger logger = LoggerFactory.getLogger(FinishRouterFilter.class);

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 11;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String uri = ctx.getRequest().getRequestURI();
        if (TextUtils.strCompare(uri, SysConstant.LOGINURI)){
            return false;
        }
        return true;
    }

    @Override
    public Object run() {
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            HttpServletRequest request = ctx.getRequest();

            HttpServletResponse response = ctx.getResponse();
            Integer code = ctx.getResponseStatusCode();
            String body = ctx.getResponseBody();
            if (!Objects.isNull(body)){
                try {
                    ResponseBody responseBody = JSON.parseObject(body, ResponseBody.class);
                    if (!Objects.isNull(responseBody)){
                        return null;
                    }
                }catch (Exception e){
                    logger.error("json反序列化失败, responseBody：{}", body);
                }
            }
            // 对后端的相应进行包装(如果是401，则包装为401的对象；如果是正常则包装为200的对象)
            if (code == 401){
                errorResponse(ctx, "身份信息过期", 401);
                return null;
            }
            if (code == 200){
                InputStream in = ctx.getResponseDataStream();
                ResponseBody userBody = new ResponseBody();
                Optional.ofNullable(in).ifPresent(i -> {
                    try {
                        userBody.setData(IOUtils.toString(i, "utf-8"));
                    } catch (IOException e) {
                        errorResponse(ctx, "异常:"+e.getLocalizedMessage(), -1);
                        e.printStackTrace();
                    }
                });
                userBody.setMsg("success");
                response.setCharacterEncoding("UTF-8");
                response.setContentType("text/json;charset=UTF-8");
                OutputStream outStream = response.getOutputStream();
                String userBodyStr = JSON.toJSONString(userBody);
                InputStream is = new ByteArrayInputStream(userBodyStr.getBytes(response.getCharacterEncoding()));
                writeResponse(is,outStream);
                return null;
            }else {
                errorResponse(ctx, "服务出错了...", 500);
                return null;
            }

        }catch (Exception e){
            ReflectionUtils.rethrowRuntimeException(e);
        }
        return null;
    }

    private void writeResponse(InputStream zin, OutputStream out) throws Exception {
        byte[] bytes = new byte[1024];
        int bytesRead = -1;
        while ((bytesRead = zin.read(bytes)) != -1) {
            out.write(bytes, 0, bytesRead);
        }
    }

    private Object errorResponse(RequestContext context, String msg, Integer code){
        context.setSendZuulResponse(false);
        context.setResponseBody(JSON.toJSONString(ResponseBodyUtil.unauthorized(msg)));
        context.setResponseStatusCode(code);
        return null;
    }
}
