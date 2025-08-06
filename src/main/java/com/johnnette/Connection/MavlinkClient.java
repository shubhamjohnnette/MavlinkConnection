package Connection;

import io.dronefleet.mavlink.MavlinkConnection;

public interface MavlinkClient {
    void connect(String host, int port) throws Exception;
    void disconnect();
    void sendData(byte[] data) throws Exception;
    MavlinkConnection getMavlinkConnection();

    default void sendDataTo(byte[] data, String host, int port) throws Exception {
        throw new UnsupportedOperationException("Destination-specific send not supported");
    }
}
