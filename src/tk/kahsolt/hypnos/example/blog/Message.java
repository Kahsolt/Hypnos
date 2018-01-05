package tk.kahsolt.hypnos.example.blog;

import tk.kahsolt.hypnos.example.Types;
import tk.kahsolt.hypnos.model.*;

import java.util.ArrayList;

public class Message extends Model {

    @Manager
    public static Model objects;    // 管理器也不是必须的(若没有的话会产生个WARN)，但通常推荐有
    @Cache
    public static ArrayList<Types> cache;

    @FieldEntry(referencesTo = User.class, isDeleteCascade = true, isUpdateCascade = true)
    public Integer poster;
    @FieldEntry(length = 0)
    public String content;

    // 这个表我们写干净一点， 用以展示最基础的模型所必备的内容
}
