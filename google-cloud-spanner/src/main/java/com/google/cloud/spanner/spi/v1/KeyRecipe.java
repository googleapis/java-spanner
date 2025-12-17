package com.google.cloud.spanner.spi.v1;

import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.spanner.v1.KeyRange;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class KeyRecipe {

  private enum Kind {
    TAG,
    VALUE
  }

  private static final class Part {
    private final Kind kind;
    private final int tag; // if kind == TAG
    private final com.google.spanner.v1.Type type; // if kind == VALUE
    private final com.google.spanner.v1.KeyRecipe.Part.Order order; // if kind == VALUE
    private final com.google.spanner.v1.KeyRecipe.Part.NullOrder nullOrder; // if kind == VALUE
    private final String identifier; // if kind == VALUE

    private Part(
        Kind kind,
        int tag,
        com.google.spanner.v1.Type type,
        com.google.spanner.v1.KeyRecipe.Part.Order order,
        com.google.spanner.v1.KeyRecipe.Part.NullOrder nullOrder,
        String identifier) {
      this.kind = kind;
      this.tag = tag;
      this.type = type;
      this.order = order;
      this.nullOrder = nullOrder;
      this.identifier = identifier;
    }

    static Part fromProto(com.google.spanner.v1.KeyRecipe.Part partProto) {
      if (partProto.getTag() > 0) {
        return new Part(Kind.TAG, partProto.getTag(), null, null, null, null);
      } else {
        if (!partProto.hasType()) {
          throw new IllegalArgumentException(
              "KeyRecipe.Part representing a value must have a type.");
        }
        if (partProto.getOrder() != com.google.spanner.v1.KeyRecipe.Part.Order.ASCENDING
            && partProto.getOrder() != com.google.spanner.v1.KeyRecipe.Part.Order.DESCENDING) {
          throw new IllegalArgumentException(
              "KeyRecipe.Part order must be ASCENDING or DESCENDING.");
        }
        if (partProto.getNullOrder() != com.google.spanner.v1.KeyRecipe.Part.NullOrder.NULLS_FIRST
            && partProto.getNullOrder() != com.google.spanner.v1.KeyRecipe.Part.NullOrder.NULLS_LAST
            && partProto.getNullOrder()
                != com.google.spanner.v1.KeyRecipe.Part.NullOrder.NOT_NULL) {
          throw new IllegalArgumentException(
              "KeyRecipe.Part null_order must be NULLS_FIRST or NULLS_LAST.");
        }
        String identifier = partProto.getIdentifier().isEmpty() ? partProto.getIdentifier() : null;
        return new Part(
            Kind.VALUE,
            0, // tag is not used for VALUE kind in this simplified constructor
            partProto.getType(),
            partProto.getOrder(),
            partProto.getNullOrder(),
            identifier);
      }
    }
  }

  private final List<Part> parts;
  private final int numValueParts;

  private KeyRecipe(List<Part> parts, int numValueParts) {
    this.parts = parts;
    this.numValueParts = numValueParts;
  }

  public static KeyRecipe create(com.google.spanner.v1.KeyRecipe in) {
    List<Part> partsList = new ArrayList<>();
    int valuePartsCount = 0;
    for (com.google.spanner.v1.KeyRecipe.Part partProto : in.getPartList()) {
      Part part = Part.fromProto(partProto);
      partsList.add(part);
      if (part.kind == Kind.VALUE) {
        valuePartsCount++;
      }
    }
    if (partsList.isEmpty()) {
      throw new IllegalArgumentException("KeyRecipe must have at least one part.");
    }
    return new KeyRecipe(partsList, valuePartsCount);
  }

  private static void encodeNull(Part part, ByteArrayOutputStream out) {
    switch (part.nullOrder) {
      case NULLS_FIRST:
        SsFormat.appendNullOrderedFirst(out);
        break;
      case NULLS_LAST:
        SsFormat.appendNullOrderedLast(out);
        break;
      case NOT_NULL:
        throw new IllegalArgumentException("Key part cannot be NULL");
      default:
        throw new IllegalArgumentException("Unknown null order: " + part.nullOrder);
    }
  }

  private static void encodeNotNull(Part part, ByteArrayOutputStream out) {
    switch (part.nullOrder) {
      case NULLS_FIRST:
        SsFormat.appendNotNullMarkerNullOrderedFirst(out);
        break;
      case NULLS_LAST:
        SsFormat.appendNotNullMarkerNullOrderedLast(out);
        break;
      case NOT_NULL:
        // No marker needed for NOT_NULL
        break;
      default:
        throw new IllegalArgumentException("Unknown null order: " + part.nullOrder);
    }
  }

  private static void encodeSingleValuePart(Part part, Value value, ByteArrayOutputStream out) {
    if (value.getKindCase() == Value.KindCase.NULL_VALUE) {
      encodeNull(part, out);
      return;
    }
    encodeNotNull(part, out);

    boolean isAscending = (part.order == com.google.spanner.v1.KeyRecipe.Part.Order.ASCENDING);

    switch (part.type.getCode()) {
      case BOOL:
        if (value.getKindCase() != Value.KindCase.BOOL_VALUE) {
          throw new IllegalArgumentException("Type mismatch for BOOL.");
        }
        if (isAscending) {
          SsFormat.appendUnsignedIntIncreasing(out, value.getBoolValue() ? 1 : 0);
        } else {
          SsFormat.appendUnsignedIntDecreasing(out, value.getBoolValue() ? 1 : 0);
        }
        break;
      case INT64:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for INT64, expecting decimal string.");
        }
        long intVal;
        try {
          intVal = Long.parseLong(value.getStringValue());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid INT64 string: " + value.getStringValue(), e);
        }
        if (isAscending) {
          SsFormat.appendIntIncreasing(out, intVal);
        } else {
          SsFormat.appendIntDecreasing(out, intVal);
        }
        break;
      case FLOAT64:
        if (value.getKindCase() != Value.KindCase.NUMBER_VALUE) {
          throw new IllegalArgumentException("Type mismatch for FLOAT64.");
        }
        if (isAscending) {
          SsFormat.appendDoubleIncreasing(out, value.getNumberValue());
        } else {
          SsFormat.appendDoubleDecreasing(out, value.getNumberValue());
        }
        break;
      case STRING:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for STRING.");
        }
        if (isAscending) {
          SsFormat.appendStringIncreasing(out, value.getStringValue());
        } else {
          SsFormat.appendStringDecreasing(out, value.getStringValue());
        }
        break;
      case BYTES:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for BYTES, expecting base64 string.");
        }
        byte[] bytesDecoded;
        try {
          bytesDecoded = Base64.getDecoder().decode(value.getStringValue());
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid base64 for BYTES type.", e);
        }
        if (isAscending) {
          SsFormat.appendBytesIncreasing(out, bytesDecoded);
        } else {
          SsFormat.appendBytesDecreasing(out, bytesDecoded);
        }
        break;
      case TIMESTAMP:
      case DATE:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for TIMESTAMP/DATE, expecting string.");
        }
        if (isAscending) {
          SsFormat.appendStringIncreasing(out, value.getStringValue());
        } else {
          SsFormat.appendStringDecreasing(out, value.getStringValue());
        }
        break;
      case NUMERIC:
      case ENUM:
      case TYPE_CODE_UNSPECIFIED:
      case ARRAY:
      case STRUCT:
      case PROTO:
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException(
            "Unsupported type code for ssformat encoding: " + part.type.getCode());
    }
  }

  private ByteString encodeKeyInternal(ValueGetter valueFinder, boolean requireFullKey) {
    ByteArrayOutputStream ssKey = new ByteArrayOutputStream();
    int valueIdx = 0;
    for (Part part : parts) {
      switch (part.kind) {
        case TAG:
          SsFormat.appendCompositeTag(ssKey, part.tag);
          break;
        case VALUE:
          Value value = valueFinder.get(valueIdx, part.identifier);
          if (value == null) {
            if (requireFullKey) {
              throw new IllegalArgumentException("Missing value for part " + part.identifier);
            } else {
              // If not requiring full key, stop when a value is missing (e.g. for prefix ranges)
              return ByteString.copyFrom(ssKey.toByteArray());
            }
          }
          encodeSingleValuePart(part, value, ssKey);
          valueIdx++;
          break;
      }
    }
    return ByteString.copyFrom(ssKey.toByteArray());
  }

  private ByteString encodeListValueInternal(ListValue listValue, boolean requireFullKey) {
    return encodeKeyInternal(
        (index, identifier) -> {
          if (index < 0 || index >= listValue.getValuesCount()) {
            return null;
          }
          return listValue.getValues(index);
        },
        requireFullKey);
  }

  public ByteString keyToSS(ListValue in) {
    Objects.requireNonNull(in, "Input ListValue cannot be null");
    return this.encodeListValueInternal(in, true);
  }

  public ByteString keyRangeToSSRangeStart(final KeyRange in) {
    Objects.requireNonNull(in, "Input KeyRange cannot be null");

    switch (in.getStartKeyTypeCase()) {
      case START_CLOSED:
        return this.encodeListValueInternal(in.getStartClosed(), false);
      case START_OPEN:
        ByteString startSs = this.encodeListValueInternal(in.getStartOpen(), false);
        return startSs;
      case STARTKEYTYPE_NOT_SET:
      default:
        throw new IllegalArgumentException("KeyRange must have a start key type.");
    }
  }

  public ByteString keyRangeToSSRangeLimit(final KeyRange in) {
    Objects.requireNonNull(in, "Input KeyRange cannot be null");

    switch (in.getEndKeyTypeCase()) {
      case END_CLOSED:
        ByteString limitSs = this.encodeListValueInternal(in.getEndClosed(), false);
        return SsFormat.makePrefixSuccessor(limitSs);
      case END_OPEN:
        return this.encodeListValueInternal(in.getEndOpen(), false);
      case ENDKEYTYPE_NOT_SET:
      default:
        throw new IllegalArgumentException("KeyRange must have an end key type.");
    }
  }

  @FunctionalInterface
  private interface ValueGetter {
    Value get(final int index, final String identifier);
  }
}
