package ru.inno.itpd.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document("plugins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PluginDocument {
    @Id
    private String name;                  // = _id
    private PluginType type;              // задаётся при создании, дальше не меняется
    private boolean enabled;
    private Map<String, Object> options;  // settings плагина
    private int position;                 // порядок в цепочке
}