package ma.enset;

import org.apache.kafka.common.serialization.Serializer;
import java.nio.ByteBuffer;

public class DoubleArraySerializer implements Serializer<double[]> {
    @Override
    public byte[] serialize(String topic, double[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(8 * 3); // 3 doubles × 8 bytes
        for (double d : data) {
            buffer.putDouble(d);
        }
        return buffer.array();
    }
}