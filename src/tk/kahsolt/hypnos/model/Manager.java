package tk.kahsolt.hypnos.model;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Manager {

    /*
     *  用来修饰模型管理器（由Hypnos注入该模型的一个实例），请定义为：
     *    @Manager
     *    public static Model objects;
     *  或者：
     *    @Manager
     *    public static <该模型类名> <字段名>;    // 字段名例如manager
     */

}
