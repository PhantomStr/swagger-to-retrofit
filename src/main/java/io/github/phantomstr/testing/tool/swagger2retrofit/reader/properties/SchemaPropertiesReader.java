package io.github.phantomstr.testing.tool.swagger2retrofit.reader.properties;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import io.swagger.oas.models.Components;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import v2.io.swagger.models.properties.AbstractProperty;
import v2.io.swagger.models.properties.ArrayProperty;
import v2.io.swagger.models.properties.BooleanProperty;
import v2.io.swagger.models.properties.IntegerProperty;
import v2.io.swagger.models.properties.LongProperty;
import v2.io.swagger.models.properties.ObjectProperty;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.RefProperty;
import v2.io.swagger.models.properties.StringProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.apache.commons.lang3.StringUtils.substringBetween;

@Slf4j
public class SchemaPropertiesReader {

    ClassMapping classMapping;

    public SchemaPropertiesReader(ClassMapping classMapping) {
        this.classMapping = classMapping;
    }

    //read high-level properties from scheme
    public Map<String, Property> readProperties(Components components, Schema<?> schema, String schemaName) {
        return getProperties(components, schema);
    }

    public Consumer<String> readRefProperties(Components components, Map<String, Property> properties) {
        return ref -> {
            String subSchemaName = substringBetween(ref, "#/components/", "/");
            switch (subSchemaName) {
                case "schemas":
                    Schema subSchema = components.getSchemas().get(stripStart(ref, "#/components/schemas"));
                    properties.putAll(getProperties(components, subSchema));
                    break;
                case "requestBodies":
                case "responses":
                case "parameters":
                default:
                    throw new RuntimeException("can't process reference for" + ref);
            }
        };
    }

    /**
     * Read without inner structure.
     */
    private Property toProperty(Schema<?> propertySchema, String propertyName, boolean required) {
        //1) ref
        if (propertySchema == null) {
            ObjectProperty objectProperty = new ObjectProperty();
            objectProperty.setName(propertyName);
            objectProperty.setRequired(required);
            return objectProperty;
        }
        String ref = propertySchema.get$ref();
        if (ref != null) {
            return new RefProperty(ref);
        }

        //2) type
        String type = propertySchema.getType();
        if (type != null) {
            switch (type) {
                case "integer":
                    IntegerProperty integerProperty = new IntegerProperty();
                    integerProperty.setRequired(required);
                    integerProperty.setDescription(propertySchema.getDescription());
                    integerProperty.setExample(propertySchema.getExample());
                    integerProperty.setMinimum(propertySchema.getMinimum());
                    integerProperty.setMaximum(propertySchema.getMaximum());
                    return integerProperty;
                case "number":
                    LongProperty longProperty = new LongProperty();
                    longProperty.setRequired(required);
                    longProperty.setDescription(propertySchema.getDescription());
                    longProperty.setExample(propertySchema.getExample());
                    longProperty.setMinimum(propertySchema.getMinimum());
                    longProperty.setMaximum(propertySchema.getMaximum());
                    return longProperty;
                case "boolean":
                    BooleanProperty booleanProperty = new BooleanProperty();
                    booleanProperty.setRequired(required);
                    booleanProperty.setDescription(propertySchema.getDescription());
                    booleanProperty.setExample(propertySchema.getExample());
                    return booleanProperty;
                case "string":
                    StringProperty stringProperty = new StringProperty();
                    stringProperty.setRequired(required);
                    stringProperty.setFormat(propertySchema.getFormat());
                    stringProperty.setDescription(propertySchema.getDescription());
                    stringProperty.setExample(propertySchema.getExample());
                    return stringProperty;
                case "array":
                    Schema itemSchema = ((ArraySchema) propertySchema).getItems();
                    ArrayProperty arrayProperty = new ArrayProperty(toProperty(itemSchema, propertyName, required));
                    arrayProperty.setRequired(required);
                    arrayProperty.setItems(toProperty(itemSchema, propertyName, required));
                    return arrayProperty;
                //3) local described
                case "object":
                    InnerClassProperty innerClassProperty = new InnerClassProperty();
                    innerClassProperty.setRequired(required);
                    innerClassProperty.setDescription(propertySchema.getDescription());
                    innerClassProperty.setExample(propertySchema.getExample());
                    innerClassProperty.setName(propertyName);
                    innerClassProperty.setSchema(propertySchema);
                    innerClassProperty.setType("inner");
                    return innerClassProperty;
            }
        }

        throw new RuntimeException("can't convert schema to property");
    }

    private Map<String, Property> getProperties(Components components, Schema schema) {
        Map<String, Property> properties = new HashMap<>();

        if (schema.getProperties() != null) {
            //noinspection unchecked
            ((Map<String, Schema>) schema.getProperties())
                    .forEach((propName, propSchema) -> {
                        boolean required = schema.getRequired() != null && schema.getRequired().contains(propName);
                        //use to property
                        Property property = toProperty(propSchema, propName, required);
                        properties.put(propName, property);
                    });
        }

        //add properties from other schemas if needed
        if (schema instanceof ComposedSchema) {
            List<Schema> allOf = ((ComposedSchema) schema).getAllOf();
            if (allOf != null) {
                allOf.stream()
                        .map(Schema::get$ref)
                        .forEach(readRefProperties(components, properties));
            }
        }
        if (schema.get$ref() != null) {
            String propName = lowerCase(SimpleClassResolver.getSimpleTypeName(schema.get$ref()));
            boolean required = schema.getRequired() != null && schema.getRequired().contains(propName);
            properties.put(propName, toProperty(schema, propName, required));
        }
        //additional properties
       /* Schema additionalProperties = schema.getAdditionalProperties();
        if (additionalProperties != null) {
            properties.put(uncapitalize(schemaName), toProperty(additionalProperties, schemaName));
        }*/
        return properties;
    }

    @Getter
    @Setter
    public static class InnerClassProperty extends AbstractProperty {

        private Schema<?> schema;
        private String canonicalClassName;

    }

}