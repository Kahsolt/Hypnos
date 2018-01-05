package tk.kahsolt.hypnos.example;

import tk.kahsolt.hypnos.Hypnos;
import tk.kahsolt.hypnos.model.Cache;
import tk.kahsolt.hypnos.model.FieldEntry;
import tk.kahsolt.hypnos.model.Manager;
import tk.kahsolt.hypnos.model.Model;
import tk.kahsolt.sqlbuilder.sql.Query;

import java.util.ArrayList;

public class ModelizeMe extends Model {

    @Manager
    public static ModelizeMe manager;    // 自定义管理器字段名
    @Cache
    public static ArrayList<Types> cache;

    @FieldEntry(length = 32)
    public String name;
    @FieldEntry
    public int money;

    public ModelizeMe() { }
    public ModelizeMe(String name, int money) {
        this.name = name;
        this.money = money;
    }

    public ArrayList<ModelizeMe> getPoorUsers() {
        // 1. 生成SQL查询对象，准备值
        Query sql = sqlBuilder.select()  // 参数填"*"或留空，如果填了其他东西最终会被改写为"*"【不允许产生部分模型！】
                .from(this.getClass().getSimpleName())
                .where("money").lt();
        int val = 10;

        // 2. 调用modelize()获取模型，适当做类型转换(啊为什么没有map()之类的函数啊)
        ArrayList<Object> objects = modelize(sql, val);
        ArrayList<ModelizeMe> ModelizeMes = new ArrayList<>();
        for(Object o : objects) {
            ModelizeMes.add((ModelizeMe) o);
        }
        return ModelizeMes;
    }

    public static void main(String[] args) {
        Hypnos hypnos;
        if(args.length==1) hypnos = new Hypnos(args[0]); // jdbc:mysql://localhost:3306/test?user=root&password=&useUnicode=true&characterEncoding=UTF-8
        else hypnos = new Hypnos();
        hypnos.register(ModelizeMe.class);
        hypnos.start();

        ModelizeMe.beginUpdate();
        new ModelizeMe("kahsolt", 2).save();
        new ModelizeMe("luper", 5).save();
        ModelizeMe.endUpdate();

        ArrayList<Object> users = ModelizeMe.manager.all();
        for (Object u : users) {
            System.out.println(u);       // 查看一下全部模型
        }
        ModelizeMe.manager.delete("1=1");

        hypnos.stop();
    }

    @Override
    public String toString() {
        return "ModelizeMe{" +
                "id='" + getId() + '\'' +
                ", name='" + name + '\'' +
                ", money=" + money +
                ", create_time='" + getCreateTime() + '\'' +
                ", update_time='" + getUpdateTime() + '\'' +
                '}';
    }
}
