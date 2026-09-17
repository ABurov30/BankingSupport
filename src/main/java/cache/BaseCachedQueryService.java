package cache;

import com.github.benmanes.caffeine.cache.Cache;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseCachedQueryService<T> {
  private final Cache<String, T> cache;

  public void invalidateL1(String key) {
    cache.invalidate(key);
  }

  protected T getFromL1(String key, Function<String, T> loader) {
    return cache.get(key, loader);
  }
}
