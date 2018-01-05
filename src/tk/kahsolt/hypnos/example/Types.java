package tk.kahsolt.hypnos.example;

import tk.kahsolt.hypnos.Hypnos;
import tk.kahsolt.hypnos.model.*;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.UUID;

public class Types extends Model {

    @Manager
    public static Model objects;

    @FieldEntry
    public Byte byte_r;
    @FieldEntry
    public byte byte_v;
    @FieldEntry
    public Short short_r;
    @FieldEntry
    public short short_v;
    @FieldEntry
    public Integer int_r;
    @FieldEntry
    public int int_v;
    @FieldEntry
    public Long long_r;
    @FieldEntry
    public long long_v;
    @FieldEntry
    public Float float_r;
    @FieldEntry
    public float float_v;
    @FieldEntry
    public Double double_r;
    @FieldEntry
    public double double_v;

    @FieldEntry(length = 64)
    public String string;
    @FieldEntry(length = -32)
    public String nstring;
    @FieldEntry(length = 0)
    public String text;
    @FieldEntry
    public UUID uuid;

    @FieldEntry
    public java.util.Date datetime;
    @FieldEntry
    public java.sql.Date date;
    @FieldEntry
    public Time time;
    @FieldEntry(initSetCurrent = true)
    public Timestamp timestamp;

    @FieldEntry
    public Object object;


    public static void main(String[] args) {
        Hypnos hypnos;
        if(args.length==1) hypnos = new Hypnos(args[0]); // jdbc:mysql://localhost:3306/test?user=root&password=&useUnicode=true&characterEncoding=UTF-8
        else hypnos = new Hypnos();
        hypnos.register(Types.class);
        hypnos.start();

        Types t = new Types();
        t.byte_r = 125;
        t.byte_v = -125;
        t.short_r = null;
        t.short_v = -1;
        t.int_r = 364554;
        t.int_v = -64653;
        t.long_r = 45643123543L;
        t.long_v = -45643123543L;
        t.float_r = 156123.54f;
        t.float_v = -156123.54f;
        t.double_r = 15615465123.54654;
        t.double_v = -15612354643.54646;
        t.string = "Hello world!";
        t.nstring = "Bye world!";
        t.text = "Hello world! Bye world! Hello world! Bye world! Hello world! Bye world!";
        t.uuid = UUID.randomUUID();
        t.datetime = new java.util.Date(165054L);
        t.datetime.setTime(198462L);
        t.date = new Date(123456L);
        t.time = new Time(897654L);
        t.timestamp = new Timestamp(1234564697L);
        t.object=null;

        // 新存一个记录
        t.save();
        System.out.println("Count1=" + Types.objects.count());
        System.out.println("[t] = " + t);
        // save增量更新不产生SQL动作(因为自对象创建以来没有任何修改)
        t.save();
        // save增量更新产生SQL动作
        t.nstring = "New String";
        t.object = "Also a string";
        t.save();
        // 若无修改也可以用push强制全字段更新
        t.push();

        // tt和t是同一个数据记录
        Types tt = (Types) Types.objects.get("id", t.getId());
        System.out.println("[tt] = " + tt);
        // 用tt指针删了这个记录(数据库记录删除，本地数据还在)
        tt.remove();
        System.out.println("Count2=" + Types.objects.count());

        // 此时保存t将产生SQL动作但不会起作用(删除优先级较高，记录已不存在)
        t.byte_r = null;
        t.save();
        t.push();
        System.out.println("Count3=" + Types.objects.count());

        // 但tt的数据还在本地，可以再次save()/push()产生数据相同的记录(但是id是新的)
        tt.push();
        System.out.println("[tt] = " + tt);
        System.out.println("Count4=" + Types.objects.count());

        hypnos.stop();
    }

    @Override
    public String toString() {
        return "Types{" +
                "id=" + getId()+
                ", byte_r=" + byte_r +
                ", byte_v=" + byte_v +
                ", short_r=" + short_r +
                ", short_v=" + short_v +
                ", int_r=" + int_r +
                ", int_v=" + int_v +
                ", long_r=" + long_r +
                ", long_v=" + long_v +
                ", float_r=" + float_r +
                ", float_v=" + float_v +
                ", double_r=" + double_r +
                ", double_v=" + double_v +
                ", string='" + string + '\'' +
                ", nstring='" + nstring + '\'' +
                ", text='" + text + '\'' +
                ", uuid=" + uuid +
                ", datetime=" + datetime +
                ", date=" + date +
                ", time=" + time +
                ", timestamp=" + timestamp +
                ", object=" + object +
                ", create_time=" + getCreateTime() +
                ", update_time=" + getUpdateTime() +
                '}';
    }
}
