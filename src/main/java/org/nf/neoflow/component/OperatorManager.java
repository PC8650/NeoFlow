package org.nf.neoflow.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.annotation.ProcessMethod;
import org.nf.neoflow.annotation.ProcessOperator;
import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.dto.execute.ExecuteForm;
import org.nf.neoflow.exception.NeoFlowConfigException;
import org.nf.neoflow.exception.NeoProcessAnnotationException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 程序启动时扫描 {@link ProcessOperator ProcessOperator} 标注的类 、{@link ProcessOperator ProcessOperator} 标注的方法。
 * 对注解属性进行空值和重名校验，将对应行为存入map
 * @author PC8650
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OperatorManager {

    private final NeoFlowConfig config;

    private final Map<String,Map<String, Function<ExecuteForm, ExecuteForm>>> operatorMap = new HashMap<>();

    private Integer ProcessOperatorCount = 0;

    private Integer ProcessMethodCount = 0;

    /**
     * 执行流程
     * @param form 实例表单
     * @return 执行结果
     */
    public ExecuteForm operate(ExecuteForm form) {
        String process = form.getProcessName();
        String method = form.getOperationMethod();

        if (StringUtils.isBlank(method)) {
            return form;
        }

        if (!operatorMap.containsKey(process)) {
           throw new NeoProcessAnnotationException(String.format("未找到%s对应的@ProcessOperator", process));
        }
        if (!operatorMap.get(process).containsKey(method)) {
           throw new NeoProcessAnnotationException(String.format("未找到%s对应的@ProcessMethod", method));
        }

        return operatorMap.get(process).get(method).apply(form);
    }

    /**
     * 扫描指定路径下的@ProcessOperator
     */
    @PostConstruct
    private void scan() throws Exception{
        if (config.getIndependence()) {
            return;
        }
        log.info("开始扫描流程注解");
        String scanPackage = config.getScanPackage();
        if (scanPackage.isBlank()) {
            throw new NeoFlowConfigException("没有配置包扫描路径");
        }
        String path = scanPackage.replace(".", "/");
        log.info("流程扫描目录：{}",path);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);
        File directory;
        URL resource;
        while (resources.hasMoreElements()) {
             resource= resources.nextElement();
             directory= new File(resource.getPath());
             scanDirectory(directory,scanPackage);
        }
        log.info("扫描完成，{}有效@ProcessOperator，{}有效@ProcessMethod", ProcessOperatorCount, ProcessMethodCount);
    }

    /**
     * 扫描目录
     * @param directory 目录
     * @param packageName 包名
     * @throws Exception 异常
     */
    private void scanDirectory(File directory, String packageName) throws Exception {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 子目录则递归扫描，更新包路径和文件系统路径
                        String subPackageName = packageName + "." + file.getName();
                        scanDirectory(file, subPackageName);
                    } else if (file.getName().endsWith(".class")) {
                        // 类文件，判断注解，被标注@ProcessOperator则进行后续处理
                        String className = packageName.concat(".").concat(file.getName().substring(0, file.getName().length() - 6));
                        Class<?> clazz = Class.forName(className);
                        ProcessOperator operator = clazz.getDeclaredAnnotation(ProcessOperator.class);
                        if (operator != null) {
                            DealWithOperator(clazz, className, operator);
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理@ProcessOperator 和 @ProcessMethod
     * @param clazz
     * @param className
     * @param operator
     */
    private void DealWithOperator(Class<?> clazz, String className, ProcessOperator operator) throws Exception {
        //判断name是否为空，是否重复
        String operatorName = operator.name();
        if (StringUtils.isBlank(operatorName)) {
            throw new NeoProcessAnnotationException(String.format("%s 上的 @ProcessOperator 未定义 name", className));
        }else if (operatorMap.containsKey(operatorName)) {
            throw new NeoProcessAnnotationException(String.format("@ProcessOperator(name = \"%s\")  重复定义", operatorName));
        }

        //获取Operator的方法，处理@ProcessMethod
        Map<String, Function<ExecuteForm, ExecuteForm>> operatorMethodMap = new HashMap<>();
        Object instance = clazz.getDeclaredConstructor().newInstance();
        int pmc = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            ProcessMethod pm = method.getDeclaredAnnotation(ProcessMethod.class);
            if (pm == null) {
                continue;
            } else if (StringUtils.isBlank(pm.name())) {
                throw new NeoProcessAnnotationException(String.format("%s 方法 %s 上的@ProcessMethod 未定义 name", className, pm.name()));
            } else if (operatorMethodMap.containsKey(pm.name())) {
                throw new NeoProcessAnnotationException(String.format("%s 中的 @ProcessMethod(name = \"%s\")  重复定义", className, pm.name()));
            }
            //创建Function保存方法调用行为
            Function<ExecuteForm, ExecuteForm> function = (ExecuteForm x) ->  {
                try {
                    method.setAccessible(true);
                    return (ExecuteForm) method.invoke(instance, x);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            operatorMethodMap.put(pm.name(),function);
            pmc ++;
        }

        operatorMap.put(operatorName,operatorMethodMap);
        ProcessOperatorCount ++;
        ProcessMethodCount += pmc;

    }

}
