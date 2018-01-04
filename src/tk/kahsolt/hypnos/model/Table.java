package tk.kahsolt.hypnos.model;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

    /*
     *  [此注解不是必要的]
     *    用来制定模型类建表时的一些参数，不是必须的
     */

    /*
     *  设置数据库引擎(MySQL有效)
     */
    String engine() default "InnoDB";

    /*
     *  设全表默认字符集(MySQL有效)
     */
    String charset() default "utf8";

    /*
     *  设置表注释(MySQL有效)
     */
    String comment() default "";
}
