package dao;

import org.greenrobot.greendao.generator.DaoGenerator;
import org.greenrobot.greendao.generator.Entity;
import org.greenrobot.greendao.generator.Schema;

import java.io.IOException;

public class Dao2Generator {

    public static void main(String[] args) throws IOException, Exception {

        Schema schema = new Schema(8, "com.foobnix.dao2");

        Entity note = schema.addEntity("FileMeta");

        note.addStringProperty("path").primaryKey().indexAsc("path_asc", true).indexDesc("path_desc", true);
        note.addStringProperty("title").indexAsc("title_asc", false).indexDesc("title_desc", false);
        note.addStringProperty("author").indexAsc("author_asc", false).indexDesc("author_desc", false);
        note.addStringProperty("sequence");
        note.addStringProperty("genre");
        note.addStringProperty("child");
        note.addStringProperty("annotation");
        note.addIntProperty("sIndex");

        note.addIntProperty("cusType");

        note.addStringProperty("ext");
        note.addLongProperty("size").indexAsc("size_asc", false).indexDesc("size_desc", false);
        note.addLongProperty("date").indexAsc("date_asc", false).indexDesc("date_desc", false);

        note.addStringProperty("dateTxt");
        note.addStringProperty("sizeTxt");
        note.addStringProperty("pathTxt").indexAsc("pathTxt_asc", false).indexDesc("pathTxt_desc", false);

        note.addBooleanProperty("isStar");
        note.addLongProperty("isStarTime");

        note.addBooleanProperty("isRecent");
        note.addLongProperty("isRecentTime");
        note.addFloatProperty("isRecentProgress");
        note.addBooleanProperty("isSearchBook");
        note.addStringProperty("lang");
        note.addStringProperty("tag");
        note.addIntProperty("pages");
        note.addStringProperty("keyword");
        note.addIntProperty("year");
        note.addIntProperty("state");
        note.addStringProperty("publisher");
        note.addStringProperty("isbn");
        note.addStringProperty("parentPath").indexAsc("parentPath_asc", false).indexDesc("parentPath_desc", false);



        Entity dict = schema.addEntity("DictMeta");
        dict.addStringProperty("key").primaryKey();
        dict.addStringProperty("value");

        new DaoGenerator().generateAll(schema, "/home/data/git/LibreraReader/app/src/main/java");




    }

}

