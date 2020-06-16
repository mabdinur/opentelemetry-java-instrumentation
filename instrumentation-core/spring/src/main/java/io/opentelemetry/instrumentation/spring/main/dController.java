package io.opentelemetry.instrumentation.spring.main;

import java.util.logging.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.opentelemetry.instrumentation.spring.config.MyAwsomeService;

@RestController
@RequestMapping(value = "/hi")
public class dController {
 private static final Logger LOG = Logger.getLogger(dController.class.getName());


 @GetMapping
 public String hi() throws Exception {
  
   LOG.info("hi starts");
   (new MyAwsomeService()).myAwesomemethod("FPPPPPPPPPP");
   
   return "HELOOOOOOOOO";
 }

}