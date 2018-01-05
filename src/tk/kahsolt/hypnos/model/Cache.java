package tk.kahsolt.hypnos.model;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {

    /*
     *  用来修饰模型集合缓存（由Hypnos注入全表所有模型的列表），请定义为：
     *    @Cache
     *    public static ArrayList<该模型类名> <字段名>;     // 字段名例如cache
     *
     *    但如无必要请不要直接操作这个列表
     *    而是通过Manager的findXXX()等方法间接使用
     */

}
