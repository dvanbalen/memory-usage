package org.infinispan.interceptors;

public interface MemoryUsageInterceptorMBean {
    String getGlobalJmxDomain();
    String getMemoryUsage();
    void recordObjectSize();
    void recordObjectCount();
    void reset();
}
