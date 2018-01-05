package tk.kahsolt.hypnos.example;

import tk.kahsolt.hypnos.Hypnos;
import tk.kahsolt.hypnos.model.*;
import tk.kahsolt.hypnos.model.Manager;

import java.util.ArrayList;
import java.util.Random;

public class Filters extends Model {

    @Manager
    public static Model objects;

    @FieldEntry
    public int x;
    @FieldEntry
    public double y;
    @FieldEntry
    public String s;

    public Filters() { }
    public Filters(int x, double y, String s) {
        this.x = x;
        this.y = y;
        this.s = s;
    }

    public static void main(String[] args) {
        Hypnos hypnos;
        if(args.length==1) hypnos = new Hypnos(args[0]); // jdbc:mysql://localhost:3306/test?user=root&password=&useUnicode=true&characterEncoding=UTF-8
        else hypnos = new Hypnos();
        hypnos.register(Filters.class);
        hypnos.start();

        Random random = new Random();
        Filters.beginUpdate();   // for butch INSERT should not auto-commit
        for (int i = 0; i < 100; i++) {
            String x;
            if(random.nextDouble()>=0.2) x = "1345asd";
            else if(random.nextDouble()>=0.4) x = "sdgfnch";
            else if(random.nextDouble()>=0.6) x = null;
            else if(random.nextDouble()>=0.8) x = "sbdx5z";
            else x = "654as68";
            Filters filters = new Filters(random.nextInt(), random.nextDouble(), x);
            filters.save();
        }
        Filters.endUpdate();

        System.out.println(Filters.objects.all().size());
        System.out.println(Filters.objects.count());    // 显然比all().size()快
        System.out.println(Filters.objects.count("x > 5"));
        System.out.println(Filters.objects.findEqual("x",5).size());
        System.out.println(Filters.objects.findNotEqual("x",5).size());
        System.out.println(Filters.objects.findGreater("x",5).size());
        System.out.println(Filters.objects.findGreaterEqual("x",5).size());
        System.out.println(Filters.objects.findLess("y",0.5).size());
        System.out.println(Filters.objects.findLessEqual("y",0.5).size());
        System.out.println(Filters.objects.findBetween("x",5, 50000).size());
        System.out.println(Filters.objects.findLike("s","as").size());
        System.out.println(Filters.objects.findNull("s").size());
        System.out.println(Filters.objects.findNotNull("s").size());
        System.out.println(Filters.objects.find("(x > 5 OR y < 0.5) AND s IS NOT NULL").size());

        Filters.objects.delete("1=1");  // del all using a tautology
        hypnos.stop();
    }

}
