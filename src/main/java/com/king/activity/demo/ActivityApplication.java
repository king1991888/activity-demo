package com.king.activity.demo;


import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author king
 * 2019/1/1
 */
/**
 * activiti6.0版本不兼容springboot2.0版本，因为activiti6.0出来的时候springboot2.0还没出
 * 需要排除SecurityAutoConfiguration这个类的加载
 * */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class ActivityApplication {


    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class,args);
    }



}
