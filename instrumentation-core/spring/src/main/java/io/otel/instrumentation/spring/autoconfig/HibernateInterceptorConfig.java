package io.otel.instrumentation.spring.autoconfig;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;

/**
 * TO DO: 
 * @author root
 *
 */
public class HibernateInterceptorConfig {
  
  class HibernateInterceptor extends EmptyInterceptor{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void afterTransactionCompletion(Transaction tx) {
    }

    @Override
    public void beforeTransactionCompletion(Transaction tx) {
    }
  }
  

}
