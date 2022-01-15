package ai.platon.exotic.test

import ai.platon.pulsar.driver.Driver
import java.time.Duration

class MockDriver(
    server: String,
    authToken: String,
    httpTimeout: Duration,
): Driver(server, authToken, httpTimeout) {

}
