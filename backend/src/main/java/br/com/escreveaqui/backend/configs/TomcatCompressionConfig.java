package br.com.escreveaqui.backend.configs;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class TomcatCompressionConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> compressionCustomizer(ServerProperties serverProperties) {
        return factory -> factory.addConnectorCustomizers(connector -> {
            Compression compression = serverProperties.getCompression();
            if (compression == null || !compression.getEnabled()) return;
            if (!(connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol)) return;

            protocol.setCompression("on");
            protocol.setCompressionMinSize((int) compression.getMinResponseSize().toBytes());
            protocol.setCompressibleMimeType(StringUtils.arrayToCommaDelimitedString(compression.getMimeTypes()));
        });
    }
}
