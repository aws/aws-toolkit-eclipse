package ${packageName};

import java.util.Map;

public class ServerlessInput {

    private String resource;
    private String path;
    private String httpMethod;
    private Map<String, String> headers;
    private Map<String, String> queryStringParameters;
    private Map<String, String> pathParameters;
    private Map<String, String> stageVariables;
    private String body;

    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }
    public ServerlessInput withResource(String resource) {
        setResource(resource);
        return this;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public ServerlessInput withPath(String path) {
        setPath(path);
        return this;
    }
    public String getHttpMethod() {
        return httpMethod;
    }
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    public ServerlessInput withHttpMethod(String httpMethod) {
        setHttpMethod(httpMethod);
        return this;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    public ServerlessInput withHeaders(Map<String, String> headers) {
        setHeaders(headers);
        return this;
    }
    public Map<String, String> getQueryStringParameters() {
        return queryStringParameters;
    }
    public void setQueryStringParameters(Map<String, String> queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
    }
    public ServerlessInput withQueryStringParameters(Map<String, String> queryStringParameters) {
        setQueryStringParameters(queryStringParameters);
        return this;
    }
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }
    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }
    public ServerlessInput withPathParameters(Map<String, String> pathParameters) {
        setPathParameters(pathParameters);
        return this;
    }
    public Map<String, String> getStageVariables() {
        return stageVariables;
    }
    public void setStageVariables(Map<String, String> stageVariables) {
        this.stageVariables = stageVariables;
    }
    public ServerlessInput withStageVariables(Map<String, String> stageVariables) {
        setStageVariables(stageVariables);
        return this;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public ServerlessInput withBody(String body) {
        setBody(body);
        return this;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
        result = prime * result
                + ((httpMethod == null) ? 0 : httpMethod.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result
                + ((pathParameters == null) ? 0 : pathParameters.hashCode());
        result = prime
                * result
                + ((queryStringParameters == null) ? 0 : queryStringParameters
                        .hashCode());
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
        result = prime * result
                + ((stageVariables == null) ? 0 : stageVariables.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServerlessInput other = (ServerlessInput) obj;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        if (headers == null) {
            if (other.headers != null)
                return false;
        } else if (!headers.equals(other.headers))
            return false;
        if (httpMethod == null) {
            if (other.httpMethod != null)
                return false;
        } else if (!httpMethod.equals(other.httpMethod))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (pathParameters == null) {
            if (other.pathParameters != null)
                return false;
        } else if (!pathParameters.equals(other.pathParameters))
            return false;
        if (queryStringParameters == null) {
            if (other.queryStringParameters != null)
                return false;
        } else if (!queryStringParameters.equals(other.queryStringParameters))
            return false;
        if (resource == null) {
            if (other.resource != null)
                return false;
        } else if (!resource.equals(other.resource))
            return false;
        if (stageVariables == null) {
            if (other.stageVariables != null)
                return false;
        } else if (!stageVariables.equals(other.stageVariables))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "ServerlessInput [resource=" + resource + ", path=" + path
                + ", httpMethod=" + httpMethod + ", headers=" + headers
                + ", queryStringParameters=" + queryStringParameters
                + ", pathParameters=" + pathParameters + ", stageVariables="
                + stageVariables + ", body=" + body + "]";
    }
}