package ${packageName};

import java.util.Map;

public class ${serverlessInputClassName} {

    private String resource;
    private String path;
    private String httpMethod;
    private Map<String, String> headers;
    private Map<String, String> queryStringParameters;
    private Map<String, String> pathParameters;
    private Map<String, String> stageVariables;
    private String body;
    private RequestContext requestContext;
    private Boolean isBase64Encoded;

    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }
    public ${serverlessInputClassName} withResource(String resource) {
        setResource(resource);
        return this;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public ${serverlessInputClassName} withPath(String path) {
        setPath(path);
        return this;
    }
    public String getHttpMethod() {
        return httpMethod;
    }
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    public ${serverlessInputClassName} withHttpMethod(String httpMethod) {
        setHttpMethod(httpMethod);
        return this;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    public ${serverlessInputClassName} withHeaders(Map<String, String> headers) {
        setHeaders(headers);
        return this;
    }
    public Map<String, String> getQueryStringParameters() {
        return queryStringParameters;
    }
    public void setQueryStringParameters(Map<String, String> queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
    }
    public ${serverlessInputClassName} withQueryStringParameters(Map<String, String> queryStringParameters) {
        setQueryStringParameters(queryStringParameters);
        return this;
    }
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }
    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }
    public ${serverlessInputClassName} withPathParameters(Map<String, String> pathParameters) {
        setPathParameters(pathParameters);
        return this;
    }
    public Map<String, String> getStageVariables() {
        return stageVariables;
    }
    public void setStageVariables(Map<String, String> stageVariables) {
        this.stageVariables = stageVariables;
    }
    public ${serverlessInputClassName} withStageVariables(Map<String, String> stageVariables) {
        setStageVariables(stageVariables);
        return this;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public ${serverlessInputClassName} withBody(String body) {
        setBody(body);
        return this;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public Boolean getIsBase64Encoded() {
        return isBase64Encoded;
    }
    public void setIsBase64Encoded(Boolean isBase64Encoded) {
        this.isBase64Encoded = isBase64Encoded;
    }

    public static class RequestContext {
        private String accountId;
        private String resourceId;
        private String stage;
        private String requestId;
        private Map<String, String> identity;
        private String resourcePath;
        private String httpMethod;
        private String apiId;

        public String getAccountId() {
            return accountId;
        }
        public String getResourceId() {
            return resourceId;
        }
        public String getStage() {
            return stage;
        }
        public String getRequestId() {
            return requestId;
        }
        public Map<String, String> getIdentity() {
            return identity;
        }
        public String getResourcePath() {
            return resourcePath;
        }
        public String getHttpMethod() {
            return httpMethod;
        }
        public String getApiId() {
            return apiId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }
        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }
        public void setStage(String stage) {
            this.stage = stage;
        }
        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
        public void setIdentity(Map<String, String> identity) {
            this.identity = identity;
        }
        public void setResourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
        }
        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }
        public void setApiId(String apiId) {
            this.apiId = apiId;
        }
    }
}