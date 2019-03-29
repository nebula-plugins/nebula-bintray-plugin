package nebula.plugin.bintray

import net.jodah.failsafe.FailsafeException
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import spock.lang.Specification
import spock.lang.Subject

@Subject(BintrayClient)
class BintrayClientSpec extends Specification {

    private final BintrayService bintrayServiceMock = Mock(BintrayService)

    def 'retries calls based on configuration'() {
        setup:
        BintrayClient bintrayClient = new BintrayClient(bintrayServiceMock, new BintrayClient.RetryConfig(3, 1))

        when:
        bintrayClient.publishVersion("test", "test-repo", "test-pkg", "0.0.1", new PublishRequest())

        then:
        thrown(FailsafeException)

        4 *  bintrayServiceMock.publishVersion(*_) >>  new SocketTimeoutExceptionCallFake()
    }

    class SocketTimeoutExceptionCallFake implements Call {

        Response execute() {
            throw new SocketTimeoutException()
        }

        void enqueue(Callback callback) { }

        boolean isExecuted() { false }

        Call clone() { this }

        boolean isCanceled() { false }

        void cancel() { }

        Request request() { null }
    }
}
