package tk.kahsolt.hypnos.example;

import tk.kahsolt.hypnos.Hypnos;
import tk.kahsolt.hypnos.model.Cache;
import tk.kahsolt.hypnos.model.FieldEntry;
import tk.kahsolt.hypnos.model.Manager;
import tk.kahsolt.hypnos.model.Model;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class CachedModel extends Model {

    @Manager
    public static CachedModel objects;
    @Cache
    public static ArrayList<CachedModel> cache;

    @FieldEntry
    public UUID uuid;

    public CachedModel() { }
    public CachedModel(UUID uuid) { this.uuid = uuid; }

    public static void main(String[] args) {
        Hypnos hypnos;
        if(args.length==1) hypnos = new Hypnos(args[0]); // jdbc:mysql://localhost:3306/test?user=root&password=&useUnicode=true&characterEncoding=UTF-8
        else hypnos = new Hypnos();
        hypnos.register(CachedModel.class);
        hypnos.start();

        CachedModel.beginUpdate();   // for butch INSERT should not auto-commit
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

        hypnos.stop();
    }

}
