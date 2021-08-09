package com.phantomstr.testing.tool.swagger2retrofit.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.customProperties.ValidationSchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

import java.io.IOException;

import static com.github.fge.jackson.JsonLoader.fromString;

public class GenerateSchemas {

    public static JsonSchema generateSchema(SchemaFactoryWrapper visitor, ObjectMapper mapper, Class<?> classToGenerate) throws JsonMappingException {
        mapper.acceptJsonFormatVisitor(mapper.constructType(classToGenerate), visitor);
        return visitor.finalSchema();
    }

    public static JsonSchema generateSchema(SchemaFactoryWrapper visitor, Class<?> classToGenerate) throws JsonMappingException {
        return generateSchema(visitor, new ObjectMapper(), classToGenerate);
    }

    public static JsonSchema generateSchema(Class<?> classToGenerate) throws JsonProcessingException {
        CustomSchemaFactoryWrapper visitor = new CustomSchemaFactoryWrapper();
        JsonSchema schema = generateSchema(visitor, classToGenerate);

        // Need to clone global list of dependencies for this schema
        if (schema instanceof CustomSchemaFactoryWrapper.CustomAnySchema) {
            ((CustomSchemaFactoryWrapper.CustomAnySchema) schema).definitions = visitor.globalDefinitionClasses;
        } else if (schema instanceof CustomSchemaFactoryWrapper.CustomObjectSchema) {
            ((CustomSchemaFactoryWrapper.CustomObjectSchema) schema).definitions = visitor.globalDefinitionClasses;
        }

        return schema;
    }


    public <POJO> JsonNode generateSchemaNode(ValidationSchemaFactoryWrapper visitor, Class<POJO> pojoClass) throws IOException {
        JsonSchema schema = GenerateSchemas.generateSchema(visitor, pojoClass);
        String jsonSchemaStr = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        return fromString(jsonSchemaStr);
    }

}
