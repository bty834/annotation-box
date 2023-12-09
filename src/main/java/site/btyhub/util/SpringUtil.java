package site.btyhub.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("all")
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringUtil.applicationContext == null) {
            SpringUtil.applicationContext = applicationContext;
        }
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }


    public static String getBeanNameByObject(Object obj){
        String[] beanNamesForType = getApplicationContext().getBeanNamesForType(obj.getClass());
        for (String s : beanNamesForType) {
            if(getBean(s)==obj){
                return s;
            }
        }
        throw new RuntimeException("beanName not found");
    }
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

}
