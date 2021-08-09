package com.phantomstr.testing.tool.swagger2retrofit.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.FormatVisitorFactory;
import com.fasterxml.jackson.module.jsonSchema.factories.JsonSchemaFactory;
import com.fasterxml.jackson.module.jsonSchema.factories.ObjectVisitor;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.VisitorContext;
import com.fasterxml.jackson.module.jsonSchema.factories.WrapperFactory;
import com.fasterxml.jackson.module.jsonSchema.types.AnySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ReferenceSchema;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author rmistry
 * https://gist.github.com/rrmistry/2246c959d1c9cc45894ecf55305c61fd
 */
public class CustomSchemaFactoryWrapper extends SchemaFactoryWrapper {

    public Map<String, JsonSchema> globalDefinitionClasses;

    public CustomSchemaFactoryWrapper() {
        this(null);
    }

    public CustomSchemaFactoryWrapper(SerializerProvider p) {
        super(p);
        this.schemaProvider = new CustomJsonSchemaFactory();
        this.visitorFactory = new CustomFormatVisitorFactory(this);
        this.globalDefinitionClasses = new LinkedHashMap<>();
    }

    public static CustomSchemaFactoryWrapper getNewWrapper(CustomSchemaFactoryWrapper visitor) {
        CustomSchemaFactoryWrapper wrapper = new CustomSchemaFactoryWrapper();
        wrapper.globalDefinitionClasses = visitor.globalDefinitionClasses;
        return wrapper;
    }

    public static List<Class<?>> getAllJsonSubTypeClasses(Class<?> classToTest) {
        JsonSubTypes[] subTypesAnnotations = classToTest.getAnnotationsByType(JsonSubTypes.class);
        List<Class<?>> output = new LinkedList<>();
        for (JsonSubTypes subTypeAnnotation : subTypesAnnotations) {
            for (JsonSubTypes.Type subType : subTypeAnnotation.value()) {
                output.add(subType.value());
            }
        }
        return output;
    }

    public static String javaTypeToUrn(Class<?> classObj) {
        return "urn:jsonschema:" + classObj.getCanonicalName().replace('.', ':').replace('$', ':');
    }

    public static boolean isModel(JavaType type) {
        return type.getRawClass() != String.class
                && !isBoxedPrimitive(type.getRawClass())
                && !type.isPrimitive()
                && !type.isMapLikeType()
                && !type.isCollectionLikeType()
                && !type.isEnumType();
    }

    public static boolean isBoxedPrimitive(Class<?> type) {
        return type == Boolean.class
                || type == Byte.class
                || type == Long.class
                || type == Integer.class
                || type == Short.class
                || type == Float.class
                || type == Double.class;
    }

    public static class CustomObjectSchema extends ObjectSchema {

        @JsonProperty
        public Map<String, JsonSchema> definitions;

    }

    public static class CustomAnySchema extends AnySchema {

        @JsonProperty
        public Map<String, JsonSchema> definitions;

    }

    public static class CustomJsonSchemaFactory extends JsonSchemaFactory {

        @Override
        public AnySchema anySchema() {
            return new CustomAnySchema();
        }

        @Override
        public ObjectSchema objectSchema() {
            return new CustomObjectSchema();
        }

    }

    public static class CustomWrapperFactory extends WrapperFactory {

        private final CustomSchemaFactoryWrapper schemaFactory;

        public CustomWrapperFactory(CustomSchemaFactoryWrapper sf) {
            schemaFactory = sf;
        }

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider) {
            CustomSchemaFactoryWrapper wrapper = getNewWrapper(schemaFactory);
            wrapper.setProvider(provider);
            return wrapper;
        }

        @Override
        public SchemaFactoryWrapper getWrapper(SerializerProvider provider, VisitorContext rvc) {
            CustomSchemaFactoryWrapper wrapper = getNewWrapper(schemaFactory);
            wrapper.setProvider(provider);
            wrapper.setVisitorContext(rvc);
            return wrapper;
        }

    }

    private static class CustomObjectVisitor extends ObjectVisitor {

        private final CustomSchemaFactoryWrapper schemaFactory;

        public CustomObjectVisitor(SerializerProvider provider,
                                   ObjectSchema schema,
                                   WrapperFactory wrapperFactory,
                                   CustomSchemaFactoryWrapper sf) {
            super(provider, schema, wrapperFactory);
            schemaFactory = sf;
        }

        @Override
        public void optionalProperty(BeanProperty prop) throws JsonMappingException {
            JavaType type = prop.getType();
            if (isModel(type)) {
                ObjectSchema propSchema = new ObjectSchema();
                Class<?> rawClass = type.getRawClass();
                String refId = CustomSchemaFactoryWrapper.javaTypeToUrn(rawClass);

                Set<Object> inheritingClasses = new HashSet<>();

                for (Class<?> subClass : getAllJsonSubTypeClasses(rawClass)) {
                    inheritingClasses.add(new ReferenceSchema(javaTypeToUrn(subClass)));
                    SchemaFactoryWrapper visitor = getNewWrapper(schemaFactory);
                    try {
                        JsonSchema subClassSchema = GenerateSchemas.generateSchema(visitor, subClass);
                        if (subClassSchema instanceof ObjectSchema) {
                            ((ObjectSchema) subClassSchema).rejectAdditionalProperties();
                        }
                        schemaFactory.globalDefinitionClasses.put(javaTypeToUrn(subClass), subClassSchema);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw JsonMappingException.from(visitor.getProvider(), "Exception occured", ex);
                    }
                }

                if (inheritingClasses.size() > 0) {
                    propSchema.setOneOf(inheritingClasses);
                } else {
                    propSchema.set$ref(refId);
                    SchemaFactoryWrapper visitor = getNewWrapper(schemaFactory);
                    try {
                        JsonSchema subClassSchema = GenerateSchemas.generateSchema(visitor, rawClass);
                        if (subClassSchema.getId() == null) {
                            subClassSchema.setId(javaTypeToUrn(type.getRawClass()));
                        }
                        schemaFactory.globalDefinitionClasses.put(refId, subClassSchema);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw JsonMappingException.from(visitor.getProvider(), "Exception occured", ex);
                    }
                }

                schema.putProperty(prop.getName(), propSchema);
                propSchema.setRequired(rawClass.isAnnotationPresent(JsonInclude.class));
                return;
            }

            schema.putOptionalProperty(prop.getName(), propertySchema(prop));
        }

    }

    public static class CustomFormatVisitorFactory extends FormatVisitorFactory {

        private final CustomSchemaFactoryWrapper schemaFactory;

        public CustomFormatVisitorFactory(CustomSchemaFactoryWrapper schemaFactoryWrapper) {
            super(new CustomWrapperFactory(schemaFactoryWrapper));
            this.schemaFactory = schemaFactoryWrapper;
        }

        @Override
        public JsonObjectFormatVisitor objectFormatVisitor(SerializerProvider provider, ObjectSchema objectSchema, VisitorContext rvc) {
            CustomObjectVisitor v = new CustomObjectVisitor(provider,
                                                            objectSchema,
                                                            new CustomWrapperFactory(schemaFactory),
                                                            schemaFactory);
            v.setVisitorContext(rvc);
            return v;
        }

    }

}
