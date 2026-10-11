package dev.migzb.worldnotes;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class StructureCatalog {
    record Choice(String id,String name,String dimension,String resource) { }
    private static final List<Choice> CHOICES=load();
    private static List<Choice> load() {
        try(var stream=StructureCatalog.class.getResourceAsStream("/assets/worldnotes/structures.json")) {
            if(stream==null)return List.of();
            return Arrays.stream(new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Choice[].class))
                    .filter(choice->StructureCatalog.class.getResource("/"+choice.resource)!=null).toList();
        }catch(Exception exception){org.slf4j.LoggerFactory.getLogger("worldnotes").warn("Unable to read structure catalog",exception);return List.of();}
    }
    static List<Choice> filter(List<Choice> choices,String dimension,String query) {
        String needle=query.strip().toLowerCase(Locale.ROOT);
        return choices.stream().filter(c->c.dimension.equals(dimension))
                .filter(c->(c.name+" "+c.id).toLowerCase(Locale.ROOT).contains(needle)).toList();
    }
    static List<Choice> choices(String dimension,String query){return filter(CHOICES,dimension,query);}
}
