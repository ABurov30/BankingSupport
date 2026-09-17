package cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public final class CaffeineCacheServiceFactory {
  private CaffeineCacheServiceFactory() {}

  public static <T, P extends BaseCacheProperties> Cache<String, T> create(P cacheProperties) {
    return Caffeine.newBuilder()
        .maximumSize(cacheProperties.getL1MaxSize())
        .expireAfterWrite(cacheProperties.getL1Ttl())
        .build();
  }
}
