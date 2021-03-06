package run.var.teamcity.cloud.docker;

import jetbrains.buildServer.serverSide.InvalidProperty;
import org.junit.Before;
import org.junit.Test;
import run.var.teamcity.cloud.docker.client.DockerAPIVersion;
import run.var.teamcity.cloud.docker.client.DockerClientConfig;
import run.var.teamcity.cloud.docker.test.TestDockerClient;
import run.var.teamcity.cloud.docker.test.TestUtils;
import run.var.teamcity.cloud.docker.util.DockerCloudUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * {@link DockerCloudClient} test suite.
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class DockerCloudClientConfigTest {
    private TestDockerCloudSupportRegistry testCloudSupportRegistry;
    private TestDockerCloudSupport testCloudSupport;
    private URL serverURL;

    @Before
    public void init() throws MalformedURLException {
        testCloudSupportRegistry = new TestDockerCloudSupportRegistry();
        testCloudSupport = testCloudSupportRegistry.getCloudSupport();
        serverURL = new URL("http://not.a.real.server:8111");
    }

    @Test
    public void validConstructorArguments() {
        DockerClientConfig dockerConfig = new DockerClientConfig(DockerCloudUtils.DOCKER_DEFAULT_SOCKET_URI,
                DockerCloudUtils.DOCKER_API_TARGET_VERSION);

        DockerCloudClientConfig config = new DockerCloudClientConfig(testCloudSupport, TestUtils.TEST_UUID,
                dockerConfig, true, Duration.ofSeconds(42), Duration.ofSeconds(43),  serverURL);

        assertThat(config.getUuid()).isEqualTo(TestUtils.TEST_UUID);
        assertThat(config.getDockerClientConfig()).isSameAs(dockerConfig);
        assertThat(config.isUsingDaemonThreads()).isTrue();
        assertThat(config.getDockerSyncRate()).isEqualTo(Duration.ofSeconds(42));
        assertThat(config.getTaskTimeout()).isEqualTo(Duration.ofSeconds(43));
        assertThat(config.getServerURL()).isEqualTo(serverURL);

        config = new DockerCloudClientConfig(testCloudSupport, TestUtils.TEST_UUID, dockerConfig, true,
                Duration.ofSeconds(42), Duration.ofSeconds(43),null);

        assertThat(config.getServerURL()).isNull();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void invalidConstructorArguments() {

        DockerClientConfig dockerConfig = new DockerClientConfig(DockerCloudUtils.DOCKER_DEFAULT_SOCKET_URI,
                DockerAPIVersion.DEFAULT);

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new DockerCloudClientConfig
                (null, TestUtils.TEST_UUID, dockerConfig, true, Duration.ofSeconds(2), Duration.ofSeconds(10),
                        serverURL));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new DockerCloudClientConfig
                (testCloudSupport, null, dockerConfig, true, Duration.ofSeconds(2), Duration.ofSeconds(10), serverURL));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new DockerCloudClientConfig
                (testCloudSupport, TestUtils.TEST_UUID,
                null, true, Duration.ofSeconds(2), Duration.ofSeconds(10), serverURL));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new DockerCloudClientConfig
                (testCloudSupport, TestUtils.TEST_UUID,
                dockerConfig, true, Duration.ofSeconds(1), Duration.ofSeconds(10), serverURL));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new DockerCloudClientConfig
                (testCloudSupport, TestUtils.TEST_UUID,
                dockerConfig, true, Duration.ofSeconds(2), Duration.ofSeconds(9), serverURL));
    }

    @Test
    public void minimalConfigMap() {
        Map<String, String> params = new HashMap<>();
        params.put(DockerCloudUtils.CLIENT_UUID_PARAM, TestUtils.TEST_UUID.toString());
        params.put(DockerCloudUtils.INSTANCE_URI, TestDockerClient.TEST_CLIENT_URI.toString());
        params.put(DockerCloudUtils.CLOUD_TYPE_PARAM, TestDockerCloudSupport.CODE);

        DockerCloudClientConfig config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);
        DockerClientConfig dockerConfig = config.getDockerClientConfig();

        assertThat(config.getUuid()).isEqualTo(TestUtils.TEST_UUID);
        assertThat(config.getTaskTimeout()).isEqualTo(DockerCloudClientConfig.DEFAULT_TASK_TIMEOUT_MILLIS);
        assertThat(config.getDockerSyncRate()).isEqualTo(DockerCloudClientConfig.DEFAULT_DOCKER_SYNC_RATE);
        assertThat(dockerConfig.getInstanceURI()).isEqualTo(TestDockerClient.TEST_CLIENT_URI);
    }

    @Test
    public void serverUrlInConfigMap() {
        Map<String, String> params = new HashMap<>();
        params.put(DockerCloudUtils.CLIENT_UUID_PARAM, TestUtils.TEST_UUID.toString());
        params.put(DockerCloudUtils.INSTANCE_URI, TestDockerClient.TEST_CLIENT_URI.toString());
        params.put(DockerCloudUtils.CLOUD_TYPE_PARAM, TestDockerCloudSupport.CODE);

        DockerCloudClientConfig config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);

        assertThat(config.getServerURL()).isNull();

        params.put(DockerCloudUtils.SERVER_URL_PARAM, serverURL.toExternalForm());

        config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);

        assertThat(config.getServerURL()).isEqualTo(serverURL);

        params.put(DockerCloudUtils.SERVER_URL_PARAM, "not an url");

        assertInvalidProperty(params, DockerCloudUtils.SERVER_URL_PARAM);
    }

    @Test
    public void tlsFlagInConfigMap() {
        Map<String, String> params = new HashMap<>();
        params.put(DockerCloudUtils.CLIENT_UUID_PARAM, TestUtils.TEST_UUID.toString());
        params.put(DockerCloudUtils.INSTANCE_URI, TestDockerClient.TEST_CLIENT_URI.toString());
        params.put(DockerCloudUtils.CLOUD_TYPE_PARAM, TestDockerCloudSupport.CODE);

        DockerCloudClientConfig config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);
        DockerClientConfig dockerConfig = config.getDockerClientConfig();

        assertThat(dockerConfig.isUsingTLS()).isFalse();

        params.put(DockerCloudUtils.USE_TLS, Boolean.TRUE.toString());

        config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);
        dockerConfig = config.getDockerClientConfig();

        assertThat(dockerConfig.isUsingTLS()).isTrue();
    }

    @Test
    public void defaultLocalInstanceFlag() {
        URI defaultLocalInstanceURI;
        String defaultLocalInstanceParam;
        String unsupportedInstanceParam;
        if (DockerCloudUtils.isWindowsHost()) {
            defaultLocalInstanceURI = DockerCloudUtils.DOCKER_DEFAULT_NAMED_PIPE_URI;
            defaultLocalInstanceParam = DockerCloudUtils.USE_DEFAULT_WIN_NAMED_PIPE_PARAM;
            unsupportedInstanceParam = DockerCloudUtils.USE_DEFAULT_UNIX_SOCKET_PARAM;
        } else {
            defaultLocalInstanceURI = DockerCloudUtils.DOCKER_DEFAULT_SOCKET_URI;
            defaultLocalInstanceParam = DockerCloudUtils.USE_DEFAULT_UNIX_SOCKET_PARAM;
            unsupportedInstanceParam = DockerCloudUtils.USE_DEFAULT_WIN_NAMED_PIPE_PARAM;
        }

        Map<String, String> params = new HashMap<>();
        params.put(DockerCloudUtils.CLIENT_UUID_PARAM, TestUtils.TEST_UUID.toString());
        params.put(DockerCloudUtils.INSTANCE_URI, TestDockerClient.TEST_CLIENT_URI.toString());
        params.put(DockerCloudUtils.CLOUD_TYPE_PARAM, TestDockerCloudSupport.CODE);

        // Default local instance flag must have precedence over explicitly set URI.
        params.put(defaultLocalInstanceParam, "true");

        DockerCloudClientConfig config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);
        DockerClientConfig dockerConfig = config.getDockerClientConfig();

        assertThat(dockerConfig.getInstanceURI()).isEqualTo(defaultLocalInstanceURI);

        // Default local instance flag must be ignored when the flag is not supported on the host system.
        params.put(unsupportedInstanceParam, "true");

        config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);
        dockerConfig = config.getDockerClientConfig();

        assertThat(dockerConfig.getInstanceURI()).isEqualTo(defaultLocalInstanceURI);

        // When the default local instance flag is not set, the explicitly given URI must but used, while ignoring
        // the other unsupported flag.
        params.remove(defaultLocalInstanceParam);

        config = DockerCloudClientConfig.processParams(params, testCloudSupportRegistry);
        dockerConfig = config.getDockerClientConfig();

        assertThat(dockerConfig.getInstanceURI()).isEqualTo(TestDockerClient.TEST_CLIENT_URI);
    }


    @Test
    public void requiredPropertiesInConfigMap() {
        Map<String, String> params = new HashMap<>();
        params.put(DockerCloudUtils.CLIENT_UUID_PARAM, TestUtils.TEST_UUID.toString());
        params.put(DockerCloudUtils.CLOUD_TYPE_PARAM, TestDockerCloudSupport.CODE);

        assertInvalidProperty(params, DockerCloudUtils.INSTANCE_URI);

        params = new HashMap<>();
        params.put(DockerCloudUtils.INSTANCE_URI, TestDockerClient.TEST_CLIENT_URI.toString());
        params.put(DockerCloudUtils.CLOUD_TYPE_PARAM, TestDockerCloudSupport.CODE);

        assertInvalidProperty(params, DockerCloudUtils.CLIENT_UUID_PARAM);
    }

    private void assertInvalidProperty(Map<String, String> params, String name) {
        Throwable throwable = catchThrowable(() -> DockerCloudClientConfig.processParams(params, testCloudSupportRegistry));
        assertThat(throwable).isInstanceOf(DockerCloudClientConfigException.class);

        List<InvalidProperty> invalidProperties = ((DockerCloudClientConfigException) throwable).getInvalidProperties();
        assertThat(invalidProperties).hasSize(1);

        InvalidProperty property = invalidProperties.get(0);

        assertThat(property.getPropertyName()).isEqualTo(name);
    }
}