package cache;

import cache.enums.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CacheKeyGenerator {
  private static final String VERSION = "v1";
  private static final String SEPARATOR = ":";

  public static String generateKey(CachePrefix prefix, CachePath path, Object... parts) {
    var suffix = Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining(SEPARATOR));

    var prev = VERSION + SEPARATOR + prefix + SEPARATOR + path;
    return parts.length == 0 ? prev : prev + SEPARATOR + suffix;
  }

  public static Map<String, Object> parseKey(String key) {
    String[] parts = key.split(SEPARATOR, -1);

    if (parts.length < 3) {
      throw new IllegalArgumentException("Invalid cache key: " + key);
    }

    return Map.of(
        "version", parts[0],
        "prefix", CachePrefix.valueOf(parts[1]),
        "path", CachePath.valueOf(parts[2]),
        "args", Arrays.stream(parts).skip(3).toList());
  }
}
