package de.luhmer.owncloud.accountimporter.helper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by david on 28.06.17.
 */

public class NextcloudRequest implements Serializable {

    private static final long serialVersionUID = 215521212534236L; //assign a long value

    public String method;
    public Map<String, List<String>> header = new HashMap<>();
    public Map<String, String> parameter = new HashMap<>();
    public String requestBody;
    public String url;
    public String token;
    public String accountName;

    private NextcloudRequest() {

    }

    public static class Builder {
        private NextcloudRequest ncr;

        public Builder() {
            ncr = new NextcloudRequest();
        }

        public NextcloudRequest build() {
            return ncr;
        }

        public Builder setMethod(String method) {
            ncr.method = method;
            return this;
        }

        public Builder setHeader(Map<String, List<String>> header) {
            ncr.header = header;
            return this;
        }

        public Builder setParameter(HashMap<String, String> parameter) {
            ncr.parameter = parameter;
            return this;
        }

        public Builder setRequestBody(String requestBody) {
            ncr.requestBody = requestBody;
            return this;
        }

        public Builder setUrl(String url) {
            ncr.url = url;
            return this;
        }

        public Builder setToken(String token) {
            ncr.token = token;
            return this;
        }

        public Builder setAccountName(String accountName) {
            ncr.accountName = accountName;
            return this;
        }
    }



}