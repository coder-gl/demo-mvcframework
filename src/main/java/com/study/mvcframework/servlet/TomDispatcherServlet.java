package com.study.mvcframework.servlet;

import com.study.mvcframework.annotation.TomAutowired;
import com.study.mvcframework.annotation.TomController;
import com.study.mvcframework.annotation.TomRequestMapping;
import com.study.mvcframework.annotation.TomService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class TomDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String LOCATION = "contextConfigLocation";

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String ,Object > ioc = new HashMap<>();

    private Map<String , Method> handlerMapping = new HashMap<>();



    public TomDispatcherServlet(){
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        doLoadConfig(config.getInitParameter(LOCATION));

        doScanner(properties.getProperty("scanPackage"));

        doInstance();

        doAotuwired();

        initHandlerMapping();


        System.out.println("Tom demo mvcframework is init");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if ( !clazz.isAnnotationPresent(TomController.class)) {
                continue;
            }
            String baseUrl = "";
            //获取Controller 的url配置
            if (clazz.isAnnotationPresent(TomRequestMapping.class)) {
                TomRequestMapping requestMapping = clazz.getAnnotation(TomRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(TomRequestMapping.class)) {
                    continue;
                }

                //映射 URL
                TomRequestMapping tomRequestMapping = method.getAnnotation(TomRequestMapping.class);
                String url = ("/"+baseUrl+"/"+tomRequestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
                System.out.println("mapped "+ url + "," +method);
            }
        }
    }

    private void doAotuwired() {
        if (ioc.isEmpty()){
            return;
        }
        //iter 快捷键 增强循环
        for ( Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if ( !field.isAnnotationPresent(TomAutowired.class)){
                    continue;
                }
                TomAutowired tomAutowired = field.getAnnotation(TomAutowired.class);
                String beanName = tomAutowired.value().trim();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);

                try {
                    field.set(entry.getValue() , ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private String lowerFirstCase(String str){
        char [] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doInstance() {
        if (classNames.size() == 0){
            return;
        }
        try{
            for (String className : classNames){
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(TomController.class)){
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName , clazz.newInstance());
                }else if (clazz.isAnnotationPresent(TomService.class)){
                    TomService service = clazz.getAnnotation(TomService.class);
                    String beanName = service.value();
                    if ( !"".equals(beanName.trim())){
                        ioc.put(beanName,clazz.newInstance());
                        continue;
                    }
                    //如果没有手动设置 value
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class<?> i : interfaces){
                        ioc.put(i.getName() , clazz.newInstance());
                    }

                }else{
                    continue;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for(File file : dir.listFiles()){
            if (file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else {
                classNames.add(scanPackage+"."+file.getName().replace(".class","").trim());
            }
        }
    }

    private void doLoadConfig(String location) {
        InputStream fis = null;

        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req,resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (this.handlerMapping.isEmpty()) {
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found !!");
            return;
        }
        //Map<String ,String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);
        Class<?>[] parameterTypes = method.getParameterTypes();
        Map<String ,String[]> parameterMap = req.getParameterMap();
        Object [] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            }else if (parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
                continue;
            }else if (parameterType == String.class) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]","")
                            .replaceAll(",\\s",",");
                    paramValues[i] = value;
                }
            }

        }
        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        try {
            method.invoke(this.ioc.get(beanName) ,paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
