package tk.kahsolt.hypnos.model;

import java.lang.annotation.Documented;

@Documented
public @interface Manager {

    /*
     *  [此注解不是必要的]
     *    用来修饰模型管理器，由Hypnos注入一个模型实例，方便以伪静态的方式调用findXXX()类函数
     *    暂时公约此字段应定义为public static Model objects以节省查找时间 :)
     */

}
