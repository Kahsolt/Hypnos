package tk.kahsolt.hypnos.model;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /*
     *  <此注解是必要的>
     *    需要注册到数据库的字段必须注释以model.Column，且建议为public修饰
     */

    /*
     *  类字段类型为String时，所对应数据库列的最大长度，默认VARCHAR(128)
     *    1.正数表示使用VARCHAR(length)
     *    2.负数表示使用CHAR(-length)
     *    3.0表示使用TEXT
     */
    int length() default 128;

    /*
     *  设置为自增主键PK+AI(默认字段id使用)，自定义model不要使用！
     */
    boolean identity() default false;

    /*
     *  设置为惟一键UNIQUE；通常数据库会自动建立非聚簇索引
     */
    boolean unique() default false;

    /*
     *  设置默认值，若是数值则会自动转换类型
     */
    String defaultValue() default "";

    /*
     *  设置为非空(NOT NULL)
     *    注意：若非空设置为false，且字段无默认值则会生成可空列
     */
    boolean notNull() default false;

    /*
     *  设置时间戳默认值为当前时间(DEFAULT CURRENT_TIMESTAMP)
     *  设置更新时时间戳为当前时间(ON UPDATE CURRENT_TIMESTAMP)
     *    注意：该类字段类型必须兼容数据库的TIMESTAMP类型
     */
    boolean initSetCurrent() default false;
    boolean updateSetCurrent() default false;

    /*
     *  设置外键参考表，指向目标表的id列
     *    注意：该类字段类型必须兼容数据库的INTEGER类型
     *    默认值：Model.class表示无外键参照
     */
    Class<? extends Model> referencesTo() default Model.class;

    /*
     *  设置外键的修改策略，配合referencesTo()时起作用
     *    默认值：false表示拒绝删改即RESTRICTED
     */
    boolean isUpdateCascade() default false;
    boolean isDeleteCascade() default false;

    /*
     *  设置列注释
     */
    String comment() default "";

}

