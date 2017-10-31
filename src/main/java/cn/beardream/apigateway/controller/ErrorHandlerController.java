package cn.beardream.apigateway.controller;

import cn.beardream.core_common.model.ResponseBody;
import cn.beardream.core_common.utils.ResponseBodyUtil;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2017/10/31
 * zuul出现异常时返回统一格式
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
@RestController
public class ErrorHandlerController implements ErrorController {


    @Override
    public String getErrorPath() {
        return "/error";
    }

    @RequestMapping("/error")
    public ResponseBody error(){
        return ResponseBodyUtil.error("服务暂时不可用...");
    }
}
