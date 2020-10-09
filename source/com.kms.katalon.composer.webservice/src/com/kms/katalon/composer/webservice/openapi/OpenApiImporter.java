package com.kms.katalon.composer.webservice.openapi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.google.common.base.Preconditions;
import com.kms.katalon.composer.webservice.constants.HttpMethod;
import com.kms.katalon.composer.webservice.constants.OpenApiConstants;
import com.kms.katalon.controller.EntityNameController;
import com.kms.katalon.core.util.internal.JsonUtil;
import com.kms.katalon.entity.folder.FolderEntity;
import com.kms.katalon.entity.webservice.FormDataBodyParameter;
import com.kms.katalon.entity.webservice.UrlEncodedBodyParameter;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.servers.Server;
import io.swagger.oas.models.servers.ServerVariables;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;

public final class OpenApiImporter {

    private static final OpenApiImporter INSTANCE = new OpenApiImporter();

    public static OpenApiImporter getInstance() {
        return INSTANCE;
    }

    private enum In {
        BODY, PARAM
    };

    public OpenApiProjectImportResult importServices(String projectFilePath, FolderEntity rootFolder) throws Exception {
        OpenApiProjectImportResult projectImportResult;

        Preconditions.checkNotNull(projectFilePath, "OpenAPI 3.0 project file path must not be null or empty.");
        File projectFile = new File(projectFilePath);
        Preconditions.checkArgument(projectFile.exists(), "OpenAPI 3.0 project file does not exist.");
        Preconditions.checkNotNull(rootFolder, "Root folder must not be null.");

        OpenAPIParser parser = new OpenAPIParser();
        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        options.setResolve(true);
        SwaggerParseResult result = parser.readLocation(projectFilePath, null, options);

        OpenAPI openAPI = result.getOpenAPI();
        FolderEntity projectImportFolder = getProjectImportFolder(getProjectName(openAPI), rootFolder);
        projectImportResult = new OpenApiProjectImportResult(projectImportFolder, getBasePath(openAPI));

        Paths paths = openAPI.getPaths();
        for (String pathKey : paths.keySet()) {
            PathItem path = paths.get(pathKey);
            String resourceName = path.getSummary() != null ? path.getSummary() : pathKey;
            OpenApiRestResourceImportResult resourceImportResult = projectImportResult
                    .newResource(toValidFileName(resourceName), pathKey);
            parseRequests(resourceImportResult, path);
        }

        return projectImportResult;
    }

    private String getProjectName(OpenAPI openAPI) {
        String name = null;
        Info info = openAPI.getInfo();
        if (info != null) {
            String title = info.getTitle();
            if (title != null) {
                name = title;
            }
        }
        return name;
    }

    private String getBasePath(OpenAPI openAPI) {
        List<Server> servers = openAPI.getServers();
        String url = "/";
        if (servers != null && !servers.isEmpty()) {
            Server server = servers.get(0);
            url = server.getUrl();
            if (server.getVariables() != null) {
                ServerVariables serverVariables = server.getVariables();
                for (String variable : serverVariables.keySet()) {
                    String defaultValue = serverVariables.get(variable).getDefault();
                    if (defaultValue == null) {
                        continue;
                    }
                    String variableTemplate = "{" + variable + "}";
                    if (url.contains(variableTemplate)) {
                        url = url.replace(variableTemplate, defaultValue);
                    }
                }
            }
        }
        return url;
    }

    private FolderEntity getProjectImportFolder(String name, FolderEntity parentFolder) throws Exception {
        if (StringUtils.isBlank(name)) {
            name = "Imported from OpenAPI 3.0";
        }
        name = toValidFileName(name);
        name = EntityNameController.getInstance().getAvailableName(name, parentFolder, true);
        FolderEntity folder = new FolderEntity();
        folder.setName(name);
        folder.setParentFolder(parentFolder);
        folder.setProject(parentFolder.getProject());
        folder.setFolderType(parentFolder.getFolderType());
        folder.setDescription("folder");
        return folder;
    }

    private void parseRequests(OpenApiRestResourceImportResult resourceImportResult, PathItem pathItem) {
        if (resourceImportResult == null || pathItem == null) {
            return;
        }
        if (pathItem.getGet() != null) {
            parseRequest(resourceImportResult, HttpMethod.GET, pathItem.getGet());
        }
        if (pathItem.getPost() != null) {
            parseRequest(resourceImportResult, HttpMethod.POST, pathItem.getPost());
        }
        if (pathItem.getDelete() != null) {
            parseRequest(resourceImportResult, HttpMethod.DELETE, pathItem.getDelete());
        }
        if (pathItem.getPut() != null) {
            parseRequest(resourceImportResult, HttpMethod.PUT, pathItem.getPut());
        }
        if (pathItem.getOptions() != null) {
            parseRequest(resourceImportResult, HttpMethod.OPTIONS, pathItem.getOptions());
        }
        if (pathItem.getTrace() != null) {
            parseRequest(resourceImportResult, HttpMethod.TRACE, pathItem.getTrace());
        }
        if (pathItem.getHead() != null) {
            parseRequest(resourceImportResult, HttpMethod.HEAD, pathItem.getHead());
        }
    }

    private void parseRequest(OpenApiRestResourceImportResult resourceImportResult, HttpMethod httpMethod,
            Operation operation) {
        if (operation == null) {
            return;
        }
        String name;
        if (!StringUtils.isBlank(operation.getOperationId())) {
            name = operation.getOperationId();
        } else {
            name = httpMethod.toString();
        }
        OpenApiRestRequestImportResult request = resourceImportResult.newRequest(toValidFileName(name));
        request.setHttpMethod(httpMethod.toString());
        addParameters(request, operation.getParameters());

        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            Content content = operation.getRequestBody().getContent();
            for (String mediaTypeName : content.keySet()) {
                MediaType mediaType = content.get(mediaTypeName);
                Schema<?> schema = mediaType.getSchema();
                switch (mediaTypeName) {
                case OpenApiRestRequestImportResult.FORM_URLENCODED_CONTENT_TYPE:
                    request.setUrlEncodedBodyParameters(parseUrlEncodedRequestBody(schema));
                    request.setMediaType(mediaTypeName);
                    break;
                case OpenApiRestRequestImportResult.MULTIPART_FORM_DATA_CONTENT_TYPE:
                    request.setFormDataBodyParameters(parseFormDataRequestBody(schema));
                    request.setMediaType(mediaTypeName);
                    break;
                case OpenApiRestRequestImportResult.APPLICATION_JSON_CONTENT_TYPE:
                    String jsonBodyContent;
                    if (mediaType.getExample() != null) {
                        jsonBodyContent = parseExample(mediaType.getExample());
                    } else if (schema.getExample() != null) {
                        jsonBodyContent = parseExample(schema.getExample());
                    } else {
                        jsonBodyContent = JsonUtil.toJson(parseJsonObject(schema));
                    }
                    request.setRequestBodyContent(jsonBodyContent);
                    request.setMediaType(mediaTypeName);
                    break;
                case OpenApiRestRequestImportResult.MULTIPLE_CONTENT_TYPE:
                    String bodyContent;
                    if (schema.getExample() != null) {
                        bodyContent = parseExample(schema.getExample());
                    } else {
                        bodyContent = JsonUtil.toJson(parseJsonValue(schema, In.BODY));
                    }
                    request.setRequestBodyContent(bodyContent);
                    request.setMediaType(mediaTypeName);
                    break;
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private List<UrlEncodedBodyParameter> parseUrlEncodedRequestBody(Schema<?> schema) {
        Map<String, Schema> propertyMap = schema.getProperties();
        List<UrlEncodedBodyParameter> params = new ArrayList<>();
        for (Map.Entry<String, Schema> entry : propertyMap.entrySet()) {
            UrlEncodedBodyParameter param = new UrlEncodedBodyParameter();
            param.setName(entry.getKey());
            String value = parseJsonValue(entry.getValue(), In.BODY).toString();
            param.setValue(value);
            params.add(param);
        }
        return params;
    }

    @SuppressWarnings("rawtypes")
    private List<FormDataBodyParameter> parseFormDataRequestBody(Schema<?> schema) {
        Map<String, Schema> propertyMap = schema.getProperties();
        List<FormDataBodyParameter> params = new ArrayList<>();
        for (Map.Entry<String, Schema> entry : propertyMap.entrySet()) {
            FormDataBodyParameter param = new FormDataBodyParameter();
            param.setName(entry.getKey());
            param.setValue(JsonUtil.toJson(parseJsonValue(entry.getValue(), In.BODY)));
            String format = entry.getValue().getFormat();
            if (format != null) {
                if (entry.getValue().getType().equals("string") && format.equals("binary") || format.equals("byte")) {
                    param.setType(FormDataBodyParameter.PARAM_TYPE_FILE);
                } else {
                    param.setType(FormDataBodyParameter.PARAM_TYPE_TEXT);
                }
            }
            params.add(param);
        }
        return params;
    }

    private void addParameters(OpenApiRestResourceImportNode holder, List<Parameter> restParameters) {
        if (restParameters == null) {
            return;
        }
        for (Parameter param : restParameters) {
            String in = param.getIn();
            String name = param.getName();
            String value = null;
            String type = null;
            if (param.getSchema() != null) {
                value = parseJsonValue(param.getSchema(), In.PARAM).toString();
                type = param.getSchema().getType();
            }

            type = type != null ? "<" + type + "> " : "";
            String required = param.getRequired() ? "{Required} " : "";
            String description = type + required + param.getDescription();

            if (in.equals(OpenApiConstants.PATH_PARAMETER_TYPE)) {
                holder.addParameter(name, null, description, OpenApiRestParameter.Style.TEMPLATE);
            } else if (in.equals(OpenApiConstants.QUERY_PARAMETER_TYPE)) {
                holder.addParameter(name, value, description, OpenApiRestParameter.Style.QUERY);
            } else if (in.equals(OpenApiConstants.HEADER_PARAMETER_TYPE)) {
                holder.addParameter(name, value, description, OpenApiRestParameter.Style.HEADER);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Object> parseJsonObject(Schema<?> schema) {
        Map<String, Schema> propertyMap = schema.getProperties();
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Schema> entry : propertyMap.entrySet()) {
            String name = entry.getKey();
            Object value = parseJsonValue(entry.getValue(), In.BODY);
            if (value != null) {
                result.put(name, value);
            }
        }
        return result;
    }

    private Object parseJsonValue(Schema<?> schema, In in) {
        Object displayValue = null;
        if (schema.getExample() != null) {
            return schema.getExample();
        }
        if (schema.getEnum() != null) {
            return schema.getEnum().get(0);
        }
        switch (schema.getType()) {
        case OpenApiConstants.INTEGER_DATA_TYPE:
            displayValue = OpenApiConstants.INT_SAMPLE_VALUE;
            break;
        case OpenApiConstants.NUMBER_DATA_TYPE:
            displayValue = OpenApiConstants.NUMBER_SAMPLE_VALUE;
            break;
        case OpenApiConstants.STRING_DATA_TYPE:
            displayValue = OpenApiConstants.STRING_SAMPLE_VALUE;
            break;
        case OpenApiConstants.BOOLEAN_DATA_TYPE:
            displayValue = OpenApiConstants.BOOLEAN_SAMPLE_VALUE;
            break;
        case OpenApiConstants.ARRAY_DATA_TYPE:
            Schema<?> items = ((ArraySchema) schema).getItems();
            Object item = parseJsonValue(items, in);
            if (in == In.PARAM) {
                displayValue = item;
            } else {
                List<Object> arr = new ArrayList<>();
                arr.add(item);
                displayValue = arr;
            }
            break;
        case OpenApiConstants.OBJECT_DATA_TYPE:
            displayValue = parseJsonObject(schema);
            break;
        }
        return displayValue;
    }

    private String parseExample(Object ex) {
        JSONObject json = new JSONObject(ex.toString());
        return json.toString(2);
    }

    private String toValidFileName(String fileName) {
        return fileName.trim().replaceAll("[^A-Za-z-0-9_().\\- ]+", "_");
    }
}