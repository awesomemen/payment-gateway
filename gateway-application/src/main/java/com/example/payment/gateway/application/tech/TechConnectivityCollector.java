package com.example.payment.gateway.application.tech;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.payment.gateway.governance.SentinelScaffoldService;
import com.example.payment.gateway.infrastructure.config.GatewayTechProperties;
import com.example.payment.gateway.infrastructure.mybatis.SystemParamEntity;
import com.example.payment.gateway.infrastructure.mybatis.SystemParamMapper;
import com.example.payment.gateway.infrastructure.tech.ConnectivityProbeResult;
import com.example.payment.gateway.observability.TechScaffoldMetricsRecorder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile({"local", "docker"})
public class TechConnectivityCollector {

  private static final Logger log = LoggerFactory.getLogger(TechConnectivityCollector.class);

  private final DataSource dataSource;
  private final SystemParamMapper systemParamMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectProvider<RedissonClient> redissonClientProvider;
  private final ObjectProvider<NacosConfigManager> nacosConfigManagerProvider;
  private final ObjectProvider<NacosDiscoveryProperties> nacosDiscoveryPropertiesProvider;
  private final ObjectProvider<SentinelScaffoldService> sentinelScaffoldServiceProvider;
  private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;
  private final PrometheusMeterRegistry prometheusMeterRegistry;
  private final GatewayTechProperties gatewayTechProperties;
  private final TechScaffoldMetricsRecorder metricsRecorder;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public TechConnectivityCollector(
      DataSource dataSource,
      SystemParamMapper systemParamMapper,
      StringRedisTemplate stringRedisTemplate,
      ObjectProvider<RedissonClient> redissonClientProvider,
      ObjectProvider<NacosConfigManager> nacosConfigManagerProvider,
      ObjectProvider<NacosDiscoveryProperties> nacosDiscoveryPropertiesProvider,
      ObjectProvider<SentinelScaffoldService> sentinelScaffoldServiceProvider,
      ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
      PrometheusMeterRegistry prometheusMeterRegistry,
      GatewayTechProperties gatewayTechProperties,
      TechScaffoldMetricsRecorder metricsRecorder,
      ObjectMapper objectMapper
  ) {
    this.dataSource = dataSource;
    this.systemParamMapper = systemParamMapper;
    this.stringRedisTemplate = stringRedisTemplate;
    this.redissonClientProvider = redissonClientProvider;
    this.nacosConfigManagerProvider = nacosConfigManagerProvider;
    this.nacosDiscoveryPropertiesProvider = nacosDiscoveryPropertiesProvider;
    this.sentinelScaffoldServiceProvider = sentinelScaffoldServiceProvider;
    this.rocketMQTemplateProvider = rocketMQTemplateProvider;
    this.prometheusMeterRegistry = prometheusMeterRegistry;
    this.gatewayTechProperties = gatewayTechProperties;
    this.metricsRecorder = metricsRecorder;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
  }

  public List<ConnectivityProbeResult> collect() {
    metricsRecorder.recordProbe();
    log.info("collect_tech_connectivity begin");

    List<ConnectivityProbeResult> results = new ArrayList<>();
    results.add(probeJdbc());
    results.add(probeMyBatisPlus());
    results.add(probeRedis());
    results.add(probeRedisson());
    results.add(probeNacosConfig());
    results.add(probeNacosDiscovery());
    results.add(probeSentinel());
    results.add(probeRocketMq());
    results.add(probeSeata());
    results.add(probeDubbo());
    results.add(probeSkyWalking());
    results.add(probePrometheusMetricEndpoint());
    results.add(probePrometheusServer());
    results.add(probeGrafana());
    results.add(probeLogstash());
    results.add(probeElasticsearch());
    results.add(probeKibana());

    log.info("collect_tech_connectivity finish size={}", results.size());
    return results;
  }

  private ConnectivityProbeResult probeJdbc() {
    try (Connection connection = dataSource.getConnection()) {
      boolean valid = connection.isValid(2);
      return valid
          ? ConnectivityProbeResult.up("mysql-jdbc", "JDBC connection is valid")
          : ConnectivityProbeResult.down("mysql-jdbc", "JDBC connection returned invalid");
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("mysql-jdbc", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeMyBatisPlus() {
    try {
      long count = systemParamMapper.selectCount(Wrappers.lambdaQuery(SystemParamEntity.class));
      return ConnectivityProbeResult.up("mybatis-plus", "gateway_system_param count=" + count);
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("mybatis-plus", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeRedis() {
    try (RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection()) {
      String pong = connection.ping();
      return ConnectivityProbeResult.up("redis", "PING=" + pong);
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("redis", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeRedisson() {
    try {
      RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
      if (redissonClient == null) {
        return ConnectivityProbeResult.down("redisson", "RedissonClient bean not available");
      }
      RBucket<String> bucket = redissonClient.getBucket("tech:scaffold:redisson");
      bucket.set("ok", Duration.ofSeconds(10));
      return ConnectivityProbeResult.up("redisson", "bucketValue=" + bucket.get());
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("redisson", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeNacosConfig() {
    try {
      NacosConfigManager manager = nacosConfigManagerProvider.getIfAvailable();
      if (manager == null || manager.getConfigService() == null) {
        return ConnectivityProbeResult.down("nacos-config", "NacosConfigManager bean not available");
      }
      String serverStatus = manager.getConfigService().getServerStatus();
      return ConnectivityProbeResult.up("nacos-config", "serverStatus=" + serverStatus);
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("nacos-config", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeNacosDiscovery() {
    try {
      NacosDiscoveryProperties properties = nacosDiscoveryPropertiesProvider.getIfAvailable();
      if (properties == null) {
        return ConnectivityProbeResult.down("nacos-discovery", "NacosDiscoveryProperties bean not available");
      }
      int instanceCount = properties.namingServiceInstance()
          .getAllInstances(properties.getService()).size();
      return ConnectivityProbeResult.up(
          "nacos-discovery",
          "service=" + properties.getService() + ", instances=" + instanceCount
      );
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("nacos-discovery", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeSentinel() {
    try {
      SentinelScaffoldService service = sentinelScaffoldServiceProvider.getIfAvailable();
      if (service == null) {
        return ConnectivityProbeResult.down("sentinel", "Sentinel scaffold service not available");
      }
      String localResult = service.probe();
      ConnectivityProbeResult dashboardResult = probeHttp("sentinel-dashboard", gatewayTechProperties.getSentinelDashboard().getUrl());
      if (!"UP".equals(dashboardResult.status())) {
        return ConnectivityProbeResult.down("sentinel", "local=" + localResult + ", dashboard=" + dashboardResult.detail());
      }
      return ConnectivityProbeResult.up("sentinel", "local=" + localResult + ", dashboard reachable");
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("sentinel", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeRocketMq() {
    DefaultMQAdminExt adminExt = new DefaultMQAdminExt();
    adminExt.setInstanceName("tech-scaffold-admin");
    try {
      RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
      if (template == null) {
        return ConnectivityProbeResult.down("rocketmq", "RocketMQTemplate bean not available");
      }
      adminExt.setNamesrvAddr(template.getProducer().getNamesrvAddr());
      adminExt.start();
      int brokers = adminExt.examineBrokerClusterInfo().getBrokerAddrTable().size();
      return ConnectivityProbeResult.up("rocketmq", "brokers=" + brokers);
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("rocketmq", rootMessage(exception));
    } finally {
      try {
        adminExt.shutdown();
      } catch (Exception ignored) {
      }
    }
  }

  private ConnectivityProbeResult probeSeata() {
    return probeHttp("seata", gatewayTechProperties.getSeataHealth().getUrl());
  }

  private ConnectivityProbeResult probeDubbo() {
    try {
      if (!DubboBootstrap.getInstance().isStarted()) {
        return ConnectivityProbeResult.down("dubbo", "DubboBootstrap not started");
      }
      ConnectivityProbeResult socketProbe = probeSocket(
          "dubbo-port",
          gatewayTechProperties.getDubbo().getHost(),
          gatewayTechProperties.getDubbo().getPort()
      );
      if (!"UP".equals(socketProbe.status())) {
        return ConnectivityProbeResult.down("dubbo", socketProbe.detail());
      }
      return ConnectivityProbeResult.up("dubbo", "Dubbo bootstrap started and protocol port reachable");
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("dubbo", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeSkyWalking() {
    return probeHttp("skywalking", gatewayTechProperties.getSkywalkingQuery().getUrl());
  }

  private ConnectivityProbeResult probePrometheusMetricEndpoint() {
    try {
      String scrape = prometheusMeterRegistry.scrape();
      return ConnectivityProbeResult.up("micrometer-prometheus", "scrapeLength=" + scrape.length());
    } catch (Exception exception) {
      return ConnectivityProbeResult.down("micrometer-prometheus", rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probePrometheusServer() {
    return probeHttp("prometheus-server", gatewayTechProperties.getPrometheus().getUrl());
  }

  private ConnectivityProbeResult probeGrafana() {
    return probeHttp("grafana", gatewayTechProperties.getGrafana().getUrl());
  }

  private ConnectivityProbeResult probeLogstash() {
    return probeSocket("logstash", gatewayTechProperties.getLogstash().getHost(), gatewayTechProperties.getLogstash().getPort());
  }

  private ConnectivityProbeResult probeElasticsearch() {
    return probeHttp("elasticsearch", gatewayTechProperties.getElasticsearch().getUrl());
  }

  private ConnectivityProbeResult probeKibana() {
    return probeHttp("kibana", gatewayTechProperties.getKibana().getUrl());
  }

  private ConnectivityProbeResult probeHttp(String component, String url) {
    if (!StringUtils.hasText(url)) {
      return ConnectivityProbeResult.down(component, "probe url is empty");
    }
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .timeout(Duration.ofSeconds(3))
          .GET()
          .build();
      String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
      return ConnectivityProbeResult.up(component, summarizeBody(body));
    } catch (Exception exception) {
      return ConnectivityProbeResult.down(component, rootMessage(exception));
    }
  }

  private ConnectivityProbeResult probeSocket(String component, String host, int port) {
    if (!StringUtils.hasText(host) || port <= 0) {
      return ConnectivityProbeResult.down(component, "probe host/port not configured");
    }
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 3000);
      return ConnectivityProbeResult.up(component, host + ":" + port + " reachable");
    } catch (IOException exception) {
      return ConnectivityProbeResult.down(component, rootMessage(exception));
    }
  }

  private String summarizeBody(String body) {
    if (!StringUtils.hasText(body)) {
      return "empty response body";
    }
    try {
      JsonNode jsonNode = objectMapper.readTree(body);
      if (jsonNode.has("status")) {
        return "status=" + jsonNode.get("status").asText();
      }
      if (jsonNode.has("version")) {
        return "version=" + jsonNode.get("version").asText();
      }
    } catch (Exception ignored) {
    }
    return body.length() > 80 ? body.substring(0, 80) + "..." : body;
  }

  private String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getClass().getSimpleName() + ": " + current.getMessage();
  }

  @PreDestroy
  public void shutdown() {
    log.info("tech_connectivity_collector_shutdown");
  }
}
