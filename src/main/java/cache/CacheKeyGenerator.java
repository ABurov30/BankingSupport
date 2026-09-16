package cache;

import cache.enums.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CacheKeyGenerator {
  private static final String VERSION = "v1";

  public static String generateKey(CachePrefix prefix, CachePath path, Object... parts) {
    var suffix = Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining(":"));

    var prev = VERSION + ":" + prefix + ":" + path;
    return parts.length == 0 ? prev : prev + ":" + suffix;
  }
}
