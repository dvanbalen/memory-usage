package org.infinispan.interceptors;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.libra.Libra;
import org.jboss.libra.LibraException;

public class MemoryUsageInterceptor extends BaseCustomInterceptor implements MemoryUsageInterceptorMBean {
    
    protected Map<String, AtomicLong> usageMap = new HashMap<String, AtomicLong>();
    private Boolean useAgent = false;
    private static String JMX_TYPE = "MemoryUsage";
    private static long cacheNo = 0;

    private static final Log log = LogFactory.getLog(MemoryUsageInterceptor.class);
    private static boolean debug = log.isDebugEnabled();
    private static boolean trace = log.isTraceEnabled();
    
    public MemoryUsageInterceptor() {
        String jmxDomain = "org.infinispan.memoryusage";
        
        try {
            System.out.println("Registering MBean in ctor.");
            //jmxDomain = embeddedCacheManager.getCacheManagerConfiguration().globalJmxStatistics().domain();
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName id = new ObjectName(jmxDomain+"."+(++cacheNo)+":type="+JMX_TYPE);
            server.registerMBean(this, id);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    // Map.put(key,value) :: oldValue
    public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {

        Object retval = invokeNextInterceptor(ctx, command);
        log.info("PUTKEYVALUE value is '" + command.getValue()
                + "' and of type '" + command.getValue().getClass().getName()
                + "'.");
        if (command.isSuccessful()) {
            updateMemoryUsage(command.getValue());
        }
        return retval;
    }
    
    public String getGlobalJmxDomain() {
        if(embeddedCacheManager != null)
            return embeddedCacheManager.getCacheManagerConfiguration().globalJmxStatistics().domain();
        return "Unknown";
    }

    public String getMemoryUsage() {
        return getMemoryUsageAsString();
    }

    public void recordObjectSize() {
        if(useAgent != Boolean.TRUE)
            reset();
        useAgent = Boolean.TRUE;
    }

    public void recordObjectCount() {
        if(useAgent != Boolean.FALSE)
            reset();
        useAgent = Boolean.FALSE;
    }

    public void reset() {
        usageMap.clear();
    }
    
    public void updateMemoryUsage(Object obj) {
        String objType = obj.getClass().getName();
        
        if(!usageMap.containsKey(objType)) {
            usageMap.put(objType, new AtomicLong(0));
        }
        if(useAgent) {
            Long size;
            try {
                size = Libra.getDeepObjectSize(obj);
                usageMap.get(objType).getAndAdd(size);
            } catch (LibraException e) {
                log.warn("Error getting object size.", e);
                if(debug)
                    e.printStackTrace();
            }
        } else {
            usageMap.get(objType).getAndIncrement();
        }
    }
    
    public String getMemoryUsageAsString() {
        return usageMap.toString();
    }

}
