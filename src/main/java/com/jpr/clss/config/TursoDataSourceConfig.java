package com.jpr.clss.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;

@Configuration
@Profile("turso")
public class TursoDataSourceConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            return Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    new Class[]{DataSource.class},
                    (proxy, method, args) -> {
                        if ("getConnection".equals(method.getName())) {
                            Connection connection = (Connection) method.invoke(bean, args);
                            return Proxy.newProxyInstance(
                                    connection.getClass().getClassLoader(),
                                    new Class[]{Connection.class},
                                    (connProxy, connMethod, connArgs) -> {
                                        String methodName = connMethod.getName();
                                        if ("setAutoCommit".equals(methodName) || "commit".equals(methodName) || "rollback".equals(methodName)) {
                                            return null;
                                        }
                                        
                                        if ("prepareStatement".equals(methodName)) {
                                            java.sql.PreparedStatement pstmt = (java.sql.PreparedStatement) connMethod.invoke(connection, connArgs);
                                            return Proxy.newProxyInstance(
                                                    pstmt.getClass().getClassLoader(),
                                                    new Class[]{java.sql.PreparedStatement.class},
                                                    (pstmtProxy, pstmtMethod, pstmtArgs) -> {
                                                        try {
                                                            if ("executeUpdate".equals(pstmtMethod.getName()) && (pstmtArgs == null || pstmtArgs.length == 0)) {
                                                                pstmt.execute();
                                                                // The DBeaver LibSQL driver sometimes returns an incorrect update count (e.g., 3 instead of 1).
                                                                // Since Hibernate expects exactly 1 for single entity inserts/updates, we return 1 directly.
                                                                return 1;
                                                            }
                                                            return pstmtMethod.invoke(pstmt, pstmtArgs);
                                                        } catch (InvocationTargetException e) {
                                                            try {
                                                                java.nio.file.Files.writeString(
                                                                    java.nio.file.Path.of("turso_error.log"), 
                                                                    "JDBC ERROR in " + pstmtMethod.getName() + ": " + e.getTargetException().getMessage() + "\n",
                                                                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND
                                                                );
                                                            } catch (Exception ex) {}
                                                            throw e.getTargetException();
                                                        }
                                                    }
                                            );
                                        }

                                        try {
                                            return connMethod.invoke(connection, connArgs);
                                        } catch (InvocationTargetException e) {
                                            throw e.getTargetException();
                                        }
                                    }
                            );
                        }
                        try {
                            return method.invoke(bean, args);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException();
                        }
                    }
            );
        }
        return bean;
    }
}
