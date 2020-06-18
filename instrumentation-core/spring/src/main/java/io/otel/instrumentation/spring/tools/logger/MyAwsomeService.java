package io.otel.instrumentation.spring.tools.logger;

import org.springframework.stereotype.Service;
import io.otel.instrumentation.spring.tools.annotations.TracedClass;

@Service
@TracedClass
public class MyAwsomeService {
  
    //@TracedMethod
    public void myAwesomemethod(String someParam) throws Exception {
        System.out.println(someParam);
    }
    
    public void myAwesomemethodClassTraced(String someParam) throws Exception {
      System.out.println(someParam);
  }
}
