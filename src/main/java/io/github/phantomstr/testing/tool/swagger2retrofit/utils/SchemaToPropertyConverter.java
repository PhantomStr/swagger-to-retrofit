package io.github.phantomstr.testing.tool.swagger2retrofit.utils;

import io.swagger.oas.models.media.BooleanSchema;
import io.swagger.oas.models.media.IntegerSchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.media.StringSchema;
import v2.io.swagger.models.properties.BooleanProperty;
import v2.io.swagger.models.properties.IntegerProperty;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.StringProperty;

public class SchemaToPropertyConverter {

    public static <P extends Property> P convert(Schema schema, boolean required) {
        Property prop = null;
        if (schema instanceof StringSchema) {
            prop = new StringProperty();
        }
        if (schema instanceof IntegerSchema) {
            prop = new IntegerProperty();
        }
        if (schema instanceof BooleanSchema) {
            prop = new BooleanProperty();
        }
        if (prop == null) {
            throw new RuntimeException("can't convert " + schema.getClass().getCanonicalName() + " to Property class");
        }

        prop.setRequired(required);

        //noinspection unchecked
        return (P) prop;
    }

}
