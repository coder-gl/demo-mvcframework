package com.study.demo.mvc.controller;

import com.study.demo.mvc.service.DemoService;
import com.study.mvcframework.annotation.TomAutowired;
import com.study.mvcframework.annotation.TomController;
import com.study.mvcframework.annotation.TomRequestMapping;
import com.study.mvcframework.annotation.TomRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@TomController
@TomRequestMapping(value="/demo")
public class DemoController {

    @TomAutowired
    private DemoService demoService;

    @TomRequestMapping(value = "/getModel")
    public void getModel(HttpServletRequest req , HttpServletResponse resp,
                         @TomRequestParam("name")String name){
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
