package grpcfutureadapter;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CompletableFuture;

public final class GrpcFutureAdapter {

  private GrpcFutureAdapter() {}

  public static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> source) {

    CompletableFuture<T> result = new CompletableFuture<>();

    Futures.addCallback(
        source,
        new FutureCallback<>() {
          @Override
          public void onSuccess(T value) {
            result.complete(value);
          }

          @Override
          public void onFailure(Throwable throwable) {
            result.completeExceptionally(throwable);
          }
        },
        MoreExecutors.directExecutor());

    return result;
  }
}
