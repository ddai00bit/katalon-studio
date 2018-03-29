package com.kms.katalon.core.testobject.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import com.kms.katalon.core.testobject.HttpBodyContent;
import com.kms.katalon.core.testobject.UrlEncodedBodyParameter;

public class HttpUrlEncodedBodyContent implements HttpBodyContent {

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    
    private static final String CHARSET = "UTF-8";
    
    private UrlEncodedFormEntity urlEncodedFormEntity;
    
    public HttpUrlEncodedBodyContent(List<UrlEncodedBodyParameter> parameters) throws UnsupportedEncodingException {
        List<NameValuePair> nameValuePairs = toNameValuePairs(parameters);
        
        urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs);
        urlEncodedFormEntity.setContentType(CONTENT_TYPE);
    }
    
    private List<NameValuePair> toNameValuePairs(List<UrlEncodedBodyParameter> parameters) {
        return parameters.stream()
                .map(p -> new BasicNameValuePair(p.getName(), p.getValue()))
                .collect(Collectors.toList());
    }
    
    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public long getContentLength() {
        return 0;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException, UnsupportedOperationException {
        return urlEncodedFormEntity.getContent();
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        urlEncodedFormEntity.writeTo(outstream);
        
    }
    
    public String getCharset() {
        return CHARSET;
    }
}
