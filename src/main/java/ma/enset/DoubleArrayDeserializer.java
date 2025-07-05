package ma.enset;

import org.apache.kafka.common.serialization.Deserializer;
import java.nio.ByteBuffer;

public class DoubleArrayDeserializer implements Deserializer<double[]> {
    @Override
    public double[] deserialize(String topic, byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return new double[] {
                buffer.getDouble(),
                buffer.getDouble(),
                buffer.getDouble()
        };
    }
}