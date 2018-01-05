/*
 * Author : Kahsolt <kahsolt@qq.com>
 * Date : 2017-12-28
 * License : GPLv3
 * Description : 让我想想这个模块能有什么卵用……
 */

package tk.kahsolt.hypnos.example.blog;

import tk.kahsolt.hypnos.Hypnos;
import tk.kahsolt.hypnos.db.SQLEngine;
import tk.kahsolt.hypnos.example.Filters;
import tk.kahsolt.hypnos.model.Model;

import java.util.ArrayList;

public class Demo {

    public static void main(String[] args) {

        // 2.配置数据库，注册模型，启动引擎
        Hypnos hypnos;
        if(args.length==1) hypnos = new Hypnos(args[0]); // jdbc:mysql://localhost:3306/test?user=root&password=&useUnicode=true&characterEncoding=UTF-8
        else hypnos = new Hypnos();
        hypnos.register(User.class);
        hypnos.register(Message.class); // 若表之间有外键约束则请手动维护注册顺序 :(
        hypnos.start();

        // 3.使用你的模型
        /*
         *  Part A
         *    继承自Model的原生方法（更多例子请参考User类的自定义业务方法）
         */
        // 增
        User user = new User("kahsolt","1379");
        user.save();
        User anoUser = new User();
        anoUser.username = "luper";
        anoUser.password = "shenmegui";
        anoUser.save();

        // 查（get返回一个, findXXX返回一组，all返回全部）
        ArrayList<Object> allUsers = User.objects.all();
        for (Object u : allUsers) {
            System.out.println((User)u);
        }
        ArrayList<Object> users = User.objects.findLike("username", "upe");
        System.out.println(users.size());
        User u = (User)User.objects.get("username", "kahsolt");
        System.out.println(u);
        // 改
        u.age=50;
        u.money+=2.333;
        u.save();
        System.out.println(u);

        // 删
        user.remove();
        User.objects.update("age",100,"username = 'luper'");
        User.objects.delete("password = 'shenmegui'");
        System.out.println(User.objects.count());

        /*
         * Part B
         *   更推荐的做法是不要在类外调用原生方法
         *   应该把业务逻辑封装在对应模型类里，如下所示
         */
        new User("Kahsolt", "p@ssw0rd").register();
        User loginUser = User.login("Kahsolt", "p@ssw0rd");

        if(loginUser!=null) System.out.println("登陆成功并取得了用户信息");
        else System.out.println("登录失败，请检查用户名密码");
        System.out.println(loginUser);

        loginUser.recharge(2.333);
        loginUser.spend(6.666);

        loginUser.post("这是个示例文章，我发表的。");
        ArrayList<Message> posts = loginUser.listPost();
        for (Message p : posts) {
            System.out.println(loginUser.username + "发表了：" + p.content);
        }

        loginUser.age=12;
        loginUser.updateInfo();
        if(!loginUser.isAllowed()) System.out.println("未满18岁不得xxxxxxx");
        else System.out.println("OK，你可以做一些不可描述的事了");

        loginUser.leaveout();
        System.out.println("用户存在状态：" + loginUser.exists());

        // 这些操作对模型管理器是无效的
        User.objects.save();
        User.objects.remove();

        // 4.关闭引擎
        hypnos.stop();
    }

}