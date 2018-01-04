# Hypnos
---

    Hyperlight-weight SQL wrapper(or pseudo-ORM framework) for SQLite3
    Originally aimed to help with Minecraft plugin development

### General
  - Dependencies
    - [SQLBuilder](https://github.com/Kahsolt/SQLBuilder)
    - jdbc driver(sqlite/mysql)
    - log4j
  - Features
    - Auto-generated and managed PK(with AI) and Timestamps like which in rails(Active Record)
    - Auto data type mapping between class fields and DB columns
    - Write your service logic fluently using functional method inherited from base model
    - SQLBuilder can help with complex queries, and auto-adapt to your dialect
    - Support differential UPDATE
  - Examples
    - Code: tk/kahsolt/hypnos/example/blog/Demo.java
    - Output: run `java -jar hypnos.jar`
  - IDE Builds
    - Intellij artifact - JAR

### Tutorial

#### Step One: Design Your Model and Services
  1. 模型类-数据表：自定义模型继承自model.Model类，并可用model.Table加以注解并配置
  2. 表字段-数据列：需要映射数据库列的字段必须加以model.Column注解，用法重点参考下列文档
      - model.Column   字段配置注解
      - model.TypeMap  字段-列数据类型映射表（可参考example.Types）
  3. 模型集合管理器：每个模型类建议定义一个名为objects的字段作为该模型集合的管理器，由Hypnos自动注入
  4. 无参构造函数：模型类必须有一个无参构造函数，Hypnos会使用它
  5. 业务逻辑：基类Model提供了一些基本的增删改查方法，可以借助这些方法快速书写自定义业务
    
    下例是两个模型示例，其中注意Message有外键参考了User

```java
public class Message extends Model {

    @Manager
    public static Model objects;    // 管理器也不是必须的(若没有的话会产生个WARN)，但通常推荐有

    @Column(referencesTo = User.class, isDeleteCascade = true, isUpdateCascade = true)
    public Integer poster;
    @Column(length = 0)
    public String content;

    // 这个表我们写干净一点， 用以展示最基础的模型所必备的内容
}

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
    public User() { }                       // 必须要有一个无参构造函数让Hypnos调用
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // 自定义业务方法
    public static User login(String username, String password) {
        Object user = User.objects.get(String.format("username = '%s' AND password = '%s'", username, password));
        return (User)user;
    }
    public boolean verify() {              // 不取得用户信息的login
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
    
    // ...... 以下省略，完整请参考tk/kahsolt/hypnos/example/blog/User.java
}
```

#### Step Two: Config and Start Hypnos Engine
  1. 流程：准备数据库dbUri-构造Hypnos对象-注册你的模型-启动Hypnos引擎
  2. 可以使用的几个根对象及其方法概览：
      - hypnos实例方法
        - register()    向引擎注册自定义的模型
        - start()       启动引擎
        - stop()        关闭引擎
      - SQLEngine类静态成员
        - sqlBuilder    SQLBuilder类对象，方便生成SQL语句
      - Model类静态成员
        - dbEngine      SQLEngine类对象，即所有模型类共享的数据库连接
          - beigin()/commit()                   用于手动事务管理
          - execute()/acquire()/fetch()/query() 用于执行sql语句并取得结果
      - 自定义模型类静态成员
        - dbEngine      与Model.dbEngine指向同一个对象
        - objects       本质是该类的一个普通对象，作为方便管理该类模型的集合的管理器（但无法被save()/remove()）
```java
import tk.kahsolt.hypnos.Hypnos;

Hypnos hypnos = new Hypnos();   // 默认使用当前路径下的sqlite3.db数据库，可传入自定义的dbUri(MySQL/Sqlite协议)
                                // 如：new Hypnos("jdbc:mysql://localhost:3306/test?user=root&password=");   
hypnos.register(User.class);
hypnos.register(Message.class); // 若表之间有外键约束则请手动维护注册顺序 :(
hypnos.start();
    // ... 在这之间尽情使用你的模型吧 :)
hypnos.stop();
```

#### Step Three: Time for Business
  以我们约定的模型管理器objects为例，通常它的定义形如：
```java
public static Model objects;    // 实际上Model也可换为该模型类的名字如User
```
  可间接视作抽象类Model即基模型的实例，它只拥有Model类所定义的最基本方法，主要有以下三类：
  1. 模型集合操作
     - all()     返回该表所有模型
     - get()     查找满足指定条件的模型，若有多个则返回修改时间最新的那个
     - findXXX() 一组方法，筛选出满足指定条件的模型（可参考example.Filters）
     - count()   对全表模型计数或按指定条件计数
     - update()  按指定条件更新全表记录的某一列
     - delete()  按指定条件删除全表的某些模型
  2. 模型操作
     - save()    将这个模型存入数据库(自动判断插入或更新)，更新使用增量更新
     - push()    同save()，但更新是强制的全字段更新【确保理解了exmaple.Types中的实例再谨慎使用！】
     - remove()  从数据库删除这个模型
     - exists()  判断该模型是否在数据库中现在有/曾经有记录(本质即是否分配了主键id)
  3. 内定字段访问器
     - getId()           查看该模型的数据库自增主键的编号
     - getCreateTime()   查看该模型的创建时间
     - getUpdateTime()   查看该模型的最后修改时间
  
  以教程开头定义的表User和Message为例，以下是一些模型使用的例子：
  
```java
/*
 *  Part A
 *    继承自Model的原生方法
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

// 强制再压库，现在可手动查看数据库，User表应有一条记录
// 而注意由于设置了外键级联删除，因此Message表空
// 因此请小心设置外键规则，并谨慎使用push()
loginUser.push();
System.out.println("User counts = " + User.objects.count());
System.out.println("Message counts = " + Message.objects.count());
```
以下是该用例对应的的输出信息：
```
User{username='kahsolt', password='1379', id=1, create_time=2018-01-04 17:52:05.0, update_time=2018-01-04 17:52:05.0}
User{username='luper', password='shenmegui', id=2, create_time=2018-01-04 17:52:06.0, update_time=2018-01-04 17:52:06.0}
1
User{username='kahsolt', password='1379', id=1, create_time=2018-01-04 17:52:05.0, update_time=2018-01-04 17:52:05.0}
User{username='kahsolt', password='1379', id=1, create_time=2018-01-04 17:52:05.0, update_time=2018-01-04 17:52:05.0}
0
登陆成功并取得了用户信息
User{username='Kahsolt', password='p@ssw0rd', id=3, create_time=2018-01-04 17:52:07.0, update_time=2018-01-04 17:52:07.0}
Kahsolt发表了：这是个示例文章，我发表的。
未满18岁不得xxxxxxx
用户存在状态：false
User counts = 1
Message counts = 0
```