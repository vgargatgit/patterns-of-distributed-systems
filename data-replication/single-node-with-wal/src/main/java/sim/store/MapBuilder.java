package sim.store;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny helper to avoid verbose map construction at call sites.
 */
public final class MapBuilder {
    private MapBuilder() {}

    public static Map<String, String> of(String k1, String v1) {
        Map<String, String> map = new LinkedHashMap<>(1);
        map.put(k1, v1);
        return map;
    }

    public static Map<String, String> of(String k1, String v1, String k2, String v2) {
        Map<String, String> map = new LinkedHashMap<>(2);
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static Map<String, String> of(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> map = new LinkedHashMap<>(3);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

        public static Map<String, String> of(String k1, String v1, String k2, String v2, 
            String k3, String v3,
            String k4, String v4) {
        Map<String, String> map = new LinkedHashMap<>(3);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }
}
