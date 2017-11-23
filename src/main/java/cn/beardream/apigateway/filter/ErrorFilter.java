package cn.beardream.apigateway.filter;

import cn.beardream.core_common.utils.ResponseBodyUtil;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 2017/10/31
 * 处理的是路由中的异常信息
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@Component
public class ErrorFilter extends ZuulFilter {

    Logger logger = LoggerFactory.getLogger(AccessFilter.class);

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        return ctx.getThrowable() != null && !ctx.getBoolean("sendErrorFilter.ran", false);    }

    @Override
    public Object run() {
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            HttpServletRequest request = ctx.getRequest();

            HttpServletResponse response = ctx.getResponse();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            OutputStream outStream = response.getOutputStream();
            String error = JSON.toJSONString(ResponseBodyUtil.error("zuul路由出现异常"));
            InputStream is = new ByteArrayInputStream(error.getBytes(response.getCharacterEncoding()));
            writeResponse(is,outStream);
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
}
