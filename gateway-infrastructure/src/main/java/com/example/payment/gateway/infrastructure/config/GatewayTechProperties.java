package com.example.payment.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.tech")
public class GatewayTechProperties {

  private final HttpProbe sentinelDashboard = new HttpProbe();
  private final HttpProbe seataHealth = new HttpProbe();
  private final HttpProbe skywalkingQuery = new HttpProbe();
  private final HttpProbe prometheus = new HttpProbe();
  private final HttpProbe grafana = new HttpProbe();
  private final HttpProbe elasticsearch = new HttpProbe();
  private final HttpProbe kibana = new HttpProbe();
  private final SocketProbe logstash = new SocketProbe();
  private final SocketProbe dubbo = new SocketProbe();

  public HttpProbe getSentinelDashboard() {
    return sentinelDashboard;
  }

  public HttpProbe getSeataHealth() {
    return seataHealth;
  }

  public HttpProbe getSkywalkingQuery() {
    return skywalkingQuery;
  }

  public HttpProbe getPrometheus() {
    return prometheus;
  }

  public HttpProbe getGrafana() {
    return grafana;
  }

  public HttpProbe getElasticsearch() {
    return elasticsearch;
  }

  public HttpProbe getKibana() {
    return kibana;
  }

  public SocketProbe getLogstash() {
    return logstash;
  }

  public SocketProbe getDubbo() {
    return dubbo;
  }

  public static class HttpProbe {
    private String url;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }

  public static class SocketProbe {
    private String host;
    private int port;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }
  }
}
