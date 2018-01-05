# Hypnos
---

    Hyperlight-weight SQL wrapper(or pseudo-ORM framework) for SQLite3
    Originally aimed to help with Minecraft plugin development

## General
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

## Changelog
  - v0.3
```java
/* 1. 删去了危险的push()方法
 * 2. 新增Cache模式，即增加一个被@Cache注解修饰的字段作为本地缓存
 *    Hypnos启动时会一次性将整个表模型化，读入内存，之后基本就全是
 *    本地操作而节省数据库查询的开销
 *      - 此模式不支持增量更新，也不支持含有自定义SQL条件的查询
 *      - 此模式暂未经过大量测试可能有很多BUG
 *      - 此模式特别适合频繁修改的小表
 *    如下例所示，有个有趣的现象：
 *      启用cache：
 *        生成数据+查询：耗时340左右
 *        已有数据+查询：耗时550左右
 *      不启用cache：
 *        生成数据+查询：耗时1500左右
 *        已有数据+查询：耗时1900左右
 *      生成数据反而比从数据库取现有数据快，说明了瓶颈在于类型映射和转换
 */
public class CachedModel extends Model {

    @Manager
    public static CachedModel objects;
    @Cache
    public static ArrayList<CachedModel> cache;

    @FieldEntry
    public UUID uuid;

    public static void main(String[] args) {
        // 启动Hypnos
                
        CachedModel.beginUpdate();   // 先插入很多数据，等会计时的时候注释掉
        for (int i = 0; i < 1500; i++) {
            new CachedModel(UUID.randomUUID()).save();
        }
        CachedModel.endUpdate();
        
        long start, end;
        start = System.currentTimeMillis();
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            Integer rand = random.nextInt(50);
            CachedModel.objects.findLike("uuid", rand.toString()).size();
        }
        end = System.currentTimeMillis();

        System.out.println(String.format("Time spend: %d",end - start));
        // 停止Hypnos
    }
}
```
  - v0.2
```java
/* 1. 基模型原生筛选器只支持单条件筛选，对于复杂条件不友好
 *    因此开放了modelize()方法，可以更方便开发
 *    因而删除了原来与dbEngine相关的接口
 * 2. Manager注入策略修改为优先查找名为objects的字段
 *    其次查找被注解Manager修饰的字段
 */
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
ArrayList<Object> users = ModelizeMe.manager.all(); // 使用了自定义名字为manager的字段充当模型管理器
```
```java
ModelizeMe{id='1', name='kahsolt', money=2, create_time='2018-01-05 07:18:45.0', update_time='2018-01-05 07:18:45.0'}
ModelizeMe{id='2', name='luper', money=5, create_time='2018-01-05 07:18:45.0', update_time='2018-01-05 07:18:45.0'}
```

  - v0.1
    基本功能OK，参考下文初版Tutorial

## Tutorial

### Step One: Design Your Model and Services
  1. 模型类-数据表：
      - model.Model：      模型基类，自定义模型必须继承自它
      - model.TableEntry： 表配置注解
  2. 表字段-数据列：
      - model.FieldEntry： 字段配置注解
      - model.TypeMap：    字段-列数据类型映射表（可参考example.Types）
  3. 模型集合管理器：
      - model.Manager：    模型管理器字段公约，建议为每个模型类定义一个字段作为该模型集合的管理器
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

### Step Two: Config and Start Hypnos Engine
  1. 流程：准备数据库dbUri-构造Hypnos对象-注册你的模型-启动Hypnos引擎
  2. 可以使用的几个根对象及其方法概览：
      - hypnos实例方法
        - register()：向引擎注册自定义的模型
        - start()：启动引擎
        - stop()：关闭引擎
      - Model类静态成员
        - sqlBuilder：SQLBuilder类对象，方便生成SQL语句
        - beiginUpdate()/endUpdate()：用于批量SQL执行时手动事务管理
      - 自定义模型静态成员(比Model类多一个模型管理器)
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

### Step Three: Time for Business
  以我们约定的模型管理器objects对象为例，通常它的定义形如：
```java
public static Model objects;    // 实际上Model也可换为该模型类的名字如User
```
  可间接视作抽象类Model即基模型的实例，它只拥有Model类所定义的最基本方法，主要有以下四类：
  1. 模型转换器
     - modelize()：提交SQL查询接收模型，下述模型集合操作方法都是基于此方法实现的
  2. 模型集合操作
     - all()：     返回该表所有模型
     - get()：     查找满足指定条件的模型，若有多个则返回修改时间最新的那个
     - findXXX()： 一组方法，筛选出满足指定条件的模型（可参考example.Filters）
     - count()：   对全表模型计数或按指定条件计数
     - update()：  按指定条件更新全表记录的某一列
     - delete()：  按指定条件删除全表的某些模型
  3. 模型操作
     - save()：    将这个模型存入数据库(自动判断插入或更新)，更新使用增量更新
     - push()：    同save()，但更新是强制的全字段更新【确保理解了exmaple.Types中的实例再谨慎使用！】
     - remove()：  从数据库删除这个模型
     - exists()：  判断该模型是否在数据库中现在有/曾经有记录(本质即是否分配了主键id)
  4. 内定字段访问器
     - getId()：           查看该模型的数据库自增主键的编号
     - getCreateTime()：   查看该模型的创建时间
     - getUpdateTime()：   查看该模型的最后修改时间
  
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