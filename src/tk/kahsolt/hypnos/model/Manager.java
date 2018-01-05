package tk.kahsolt.hypnos.model;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Manager {

    /*
     *  [此注解不是必要的]
     *    用来修饰模型管理器（由Hypnos注入一个的模型实例），方便以伪静态的方式调用基模型的方法
     *    模型管理器请定义为：
     *        public static Model objects;
     *      或者
     *        @Manager
     *        public static <该模型类名> <任意字段名>;
     */

}
