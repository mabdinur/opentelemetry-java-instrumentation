package io.opentelemetry.instrumentation.spring.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Aspect
@Configuration
public class LoggingHandler {
    
//  @Pointcut("execution(* noboot.foodfinder.*.*(..))")
//  private void selectAll() {}
//  @Before("selectAll()")
//  public void beforeAdvice(){
//     System.out.println("Going to setup student profile.");
//  }  
  
     @Before("@annotation(noboot.foodfinder.aop.Loggable)")
     public void beforeLogging(JoinPoint joinPoint){
         System.out.println("Before running loggingAdvice on method=");

    }

    @After("@annotation(noboot.foodfinder.aop.Loggable)")
    public void afterLogging(JoinPoint joinPoint){
        System.out.println("After running loggingAdvice on method=");
    }
}