package org.jingle.simulator.webbit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.jingle.simulator.SimRequest;
import org.jingle.simulator.SimResponse;
import org.jingle.simulator.util.ReqRespConvertor;
import org.jingle.simulator.util.SimUtils;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

public class WebbitSimRequest implements SimRequest {
	private static final String TOP_LINE_FORMAT = "%s %s %s";
	private static final String HEADER_LINE_FORMAT = "%s: %s";
	private static final String AUTHENTICATION_LINE_FORMAT = "Authentication: %s,%s";
	private HttpRequest request; 
	private HttpResponse response;
	private String topLine;
	private String[] authentications = new String[] {"", ""};
	private String body;
	private ReqRespConvertor convertor;
	
	public WebbitSimRequest(HttpRequest request, HttpResponse response, ReqRespConvertor convertor) throws IOException {
		this.request = request;
		this.response = response;
		this.convertor = convertor;
		String method = request.method();
		String uri = request.uri();
		String protocol = "HTTP/1.1";
		this.topLine = SimUtils.formatString(TOP_LINE_FORMAT, method, SimUtils.decodeURL(uri), protocol);
		genAuthentications();
		genBody();
	}
	
	protected WebbitSimRequest() {
		
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.topLine).append("\n");
		for (Map.Entry<String, String> entry: request.allHeaders()) {
			sb.append(this.getHeaderLine(entry.getKey())).append("\n");
		}
		sb.append("\n");
		if (body != null) {
			sb.append(body);
		}
		sb.append("\n");
		return sb.toString();
	}
	
	protected void genAuthentications() {
		try {
			String val = request.header("Authorization");
			if (val != null) {
				if (val.startsWith("Basic ")) {
					String base64Str = val.substring(6);
					String str = new String(Base64.getDecoder().decode(base64Str), "utf-8");
					String[] parts = str.split(":");
					authentications[0] = parts[0];
					authentications[1] = parts[1];
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void genBody() throws IOException {
		this.body = convertor.rawRequestToBody(request);
	}
	
	public String getTopLine() {
		return this.topLine;
	}
	
	public String getHeaderLine(String header) {
		List<String> values = request.headers(header);
		StringBuffer value = new StringBuffer();
		if (values != null) {
			for (int i = 1; i <= values.size(); i++) {
				value.append(values.get(i - 1));
				if (i != values.size())
					value.append(",");
			}
		}
		return SimUtils.formatString(HEADER_LINE_FORMAT, header, value.toString());
	}
	
	public String getAutnenticationLine() {
		return SimUtils.formatString(AUTHENTICATION_LINE_FORMAT, authentications);
	}
	
	public String getBody() {
		return this.body;
	}
	
	public void fillResponse(SimResponse resp) throws IOException {
		for (Map.Entry<String, Object> entry : resp.getHeaders().entrySet()) {
			response.header(entry.getKey(), entry.getValue().toString());
		}
		convertor.fillRawResponse(response, resp);
		response.status(resp.getCode());
		response.end();
	}

	@Override
	public List<String> getAllHeaderNames() {
		List<String> ret = new ArrayList<>(); 
		for (Map.Entry<String, String> entry : request.allHeaders()) {
			ret.add(entry.getKey());
		}
		return ret;
	}
}
