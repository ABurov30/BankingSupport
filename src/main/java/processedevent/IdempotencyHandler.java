package processedevent;

import java.lang.annotation.Annotation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import processedevent.annotation.EventKey;

public interface IdempotencyHandler {

  default <T extends ProcessedEvent> boolean isAlreadyProcessed(
      String eventKey, BaseProcessedEventRepository<T> repository) {
    return repository.existsByEventKey(eventKey);
  }

  default <T extends ProcessedEvent> void markAsProcessed(
      T processedEvent, BaseProcessedEventRepository<T> repository) {
    repository.save(processedEvent);
  }

  default String extractEventKey(ProceedingJoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Annotation[][] annotations = signature.getMethod().getParameterAnnotations();

    for (int i = 0; i < annotations.length; i++) {
      for (Annotation annotation : annotations[i]) {
        if (annotation instanceof EventKey) {
          return (String) args[i];
        }
      }
    }

    throw new IllegalArgumentException("Event key parameter not found");
  }
}
