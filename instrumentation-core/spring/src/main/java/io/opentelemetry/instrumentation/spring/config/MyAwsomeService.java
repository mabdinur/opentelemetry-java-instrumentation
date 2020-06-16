package io.opentelemetry.instrumentation.spring.config;

import org.springframework.stereotype.Service;

@Service
public class MyAwsomeService {

    @Loggable
    public void myAwesomemethod(String someParam) throws Exception {
        System.out.println(someParam);
    }
}
