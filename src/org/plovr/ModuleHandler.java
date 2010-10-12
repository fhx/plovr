package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class ModuleHandler extends AbstractGetHandler {

  ModuleHandler(CompilationServer server) {
    super(server, true /* usesRestfulPath */);
  }

  /**
   * Pattern that matches the path to the REST URI for this handler.
   * The \\w+ will match the config id and the (.*) will match the
   * module name.
   */
  private static final Pattern URI_MODULE_PATTERN = Pattern.compile(
      "/module/\\w+/(.*)");

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    URI uri = exchange.getRequestURI();
    Matcher matcher = URI_MODULE_PATTERN.matcher(uri.getPath());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Module could not be extracted from URI");
    }
    String moduleName = matcher.group(1);

    Compilation compilation = getCompilation(exchange, data, config);
    if (compilation == null) {
      return;
    }

    final boolean isDebugMode = false;
    Function<String, String> moduleNameToUri = createModuleNameToUriConverter(
        server, exchange, config.getId());
    String code = compilation.getCodeForModule(moduleName, isDebugMode, moduleNameToUri);

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/javascript");
    exchange.sendResponseHeaders(200, code.length());

    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(code);
    responseBody.close();
  }

  static Function<String,String> createModuleNameToUriConverter(
      CompilationServer server, HttpExchange exchange, final String configId) {
    final String moduleUriBase = server.getServerForExchange(exchange);
    return new Function<String, String>() {
      @Override
      public String apply(String moduleName) {
        return String.format("%smodule/%s/%s",
            moduleUriBase,
            QueryData.encode(configId),
            QueryData.encode(moduleName));
      }
    };
  }
}
