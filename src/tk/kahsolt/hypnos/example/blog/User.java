package tk.kahsolt.hypnos.example.blog;

import tk.kahsolt.hypnos.Hypnos;
import tk.kahsolt.hypnos.model.*;

import java.util.ArrayList;

// 1.继承Model设计一个模型
@Table(comment = "就是用户表啊")
public class User extends Model {

    // 约定的内定字段(Hypnos会注入一个模型实例作管理者，方便以伪静态的方式调用findXXX()类函数)
    @Manager
    public static Model objects;            // 一个静态实例代表整个模型(伪管理者模式)

    // 定义数据库表的字段
    @Column(unique = true, notNull = true)
    public String username;
    @Column(length = 64, notNull = true, defaultValue = "<NEED_CHANGE>")
    public String password;
    @Column(length = -8, defaultValue = "male")
    public String gender;
    @Column(defaultValue = "0")
    public Short age;
    @Column(defaultValue = "0.0", comment = "用户余额")
    public Double money;

    // 定义构造函数，通常会比较方便save()
    public User() { }   // 必须要有一个无参构造函数让Hypnos调用
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // 自定义业务方法
    public void register() {
        this.save();
    }
    public void updateInfo() {
        this.save();
    }
    public void leaveout() {
        this.remove();
    }
    public static User login(String username, String password) {
        Object user = User.objects.get(String.format("username = '%s' AND password = '%s'", username, password));
        return (User)user;
    }
    public boolean verify() {   // 不取得用户信息的login
        return User.login(username, password) != null;
    }
    public void post(String article) {
        Message message = new Message();
        message.poster = this.getId();
        message.content = article;
        message.save();
    }
    public ArrayList<Message> listPost() {
        ArrayList<Object> objects = Message.objects.findEqual("poster", getId());
        ArrayList<Message> messages = new ArrayList<>();   // 如有必要就转换一下类型吧 :(
        for (Object o : objects) messages.add((Message)o);
        return messages;
    }
    public void recharge(Double money) {
        this.money += money;
        this.save();
    }
    public void spend(Double money) {
        this.money -= money;
        this.save();
    }
    public boolean isAllowed() {
        return this.age >= 18;
    }

    // 重载一个toString()会比较好看
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", id=" + getId() +
                ", create_time=" + getCreateTime() +
                ", update_time=" + getUpdateTime() +
                '}';
    }

}

