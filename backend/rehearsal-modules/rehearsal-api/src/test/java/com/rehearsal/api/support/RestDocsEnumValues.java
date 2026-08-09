package com.rehearsal.api.support;

import com.rehearsal.domain.situation.model.SituationType;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RestDocsEnumValues {

  private RestDocsEnumValues() {}

  public static String situationTypes() {
    return values(SituationType.values(), SituationType::getKey);
  }

  public static <E extends Enum<E>> String names(Class<E> enumType) {
    return values(enumType.getEnumConstants(), Enum::name);
  }

  private static <E extends Enum<E>> String values(E[] values, Function<E, String> mapper) {
    return Arrays.stream(values).map(mapper).collect(Collectors.joining(", ", "[", "]"));
  }
}
