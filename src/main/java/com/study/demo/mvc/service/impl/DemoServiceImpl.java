package com.study.demo.mvc.service.impl;

import com.study.demo.mvc.service.DemoService;
import com.study.mvcframework.annotation.TomService;

@TomService
public class DemoServiceImpl implements DemoService {
    @Override
    public String get(String name) {
        return "My name is "+ name;
    }
}
