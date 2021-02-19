package com.redhat.developers.msa.hola;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.deltaspike.core.spi.config.ConfigSource;

public class Configuration implements ConfigSource {

    @Override
    public boolean isScannable() {
        return true;
    }

    @Override
    public String getPropertyValue(String key) {
        return getProperties().get(key);
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<>();
        try {
            Properties p = new Properties();
            String file = System.getProperty("conf");
            System.out.println(file);
            p.load(new FileReader(file));
            for (String key : p.stringPropertyNames()) {
                String value = p.getProperty(key);
                map.put(key, value);
            }
        } catch (Exception e) {
            // No issues here
        }
        return map;
    }

    @Override
    public int getOrdinal() {
        return 0;
    }

    @Override
    public String getConfigName() {
        return "ConfigFile";
    }
}
