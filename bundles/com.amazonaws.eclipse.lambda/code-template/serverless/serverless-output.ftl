package ${packageName};

import java.util.Map;

public class ${serverlessOutputClassName} {
    private Integer statusCode;
    private Map<String, String> headers;
    private String body;

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public ${serverlessOutputClassName} withStatusCode(Integer statusCode) {
        setStatusCode(statusCode);
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public ${serverlessOutputClassName} withHeaders(Map<String, String> headers) {
        setHeaders(headers);
        return this;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ${serverlessOutputClassName} withBody(String body) {
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
                + ((statusCode == null) ? 0 : statusCode.hashCode());
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
        ${serverlessOutputClassName} other = (${serverlessOutputClassName}) obj;
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
        if (statusCode == null) {
            if (other.statusCode != null)
                return false;
        } else if (!statusCode.equals(other.statusCode))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "${serverlessOutputClassName} [statusCode=" + statusCode + ", headers="
                + headers + ", body=" + body + "]";
    }
}