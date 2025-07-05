package ma.enset;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.util.Properties;

public class WeatherStreamApp {
    public static void main(String[] args) {
        // 1. Kafka Streams Configuration
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "weather-analysis-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // 2. Define the stream topology
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> inputStream = builder.stream("weather-data");

        // 3. Filter data with temperature > 30°C
        KStream<String, String> filtered = inputStream.filter((key, value) -> {
            String[] parts = value.split(",");
            if (parts.length != 3) return false;
            try {
                double tempCelsius = Double.parseDouble(parts[1]);
                return tempCelsius > 30.0;
            } catch (NumberFormatException e) {
                return false;
            }
        });

        // 4. Convert to Fahrenheit and prepare for aggregation
        KStream<String, String> fahrenheitStream = filtered.map((key, value) -> {
            String[] parts = value.split(",");
            String station = parts[0];
            double celsius = Double.parseDouble(parts[1]);
            int humidity = Integer.parseInt(parts[2]);
            double fahrenheit = (celsius * 9.0 / 5.0) + 32;
            String newValue = station + "," + fahrenheit + "," + humidity;
            return KeyValue.pair(station, newValue);
        });

        // 5. Group by station for aggregation
        KGroupedStream<String, String> grouped = fahrenheitStream.groupByKey();

        // 6. Aggregate average temperature and humidity
        KTable<String, String> averages = grouped.aggregate(
                () -> new double[3], // [sumTemp, sumHumidity, count]
                (station, value, agg) -> {
                    String[] parts = value.split(",");
                    agg[0] += Double.parseDouble(parts[1]); // sumTemp
                    agg[1] += Double.parseDouble(parts[2]); // sumHumidity
                    agg[2] += 1;                            // count
                    return agg;
                },
                Materialized.with(Serdes.String(),
                        Serdes.serdeFrom(new DoubleArraySerializer(), new DoubleArrayDeserializer()))
        ).mapValues((station, agg) -> {
            double avgTemp = agg[0] / agg[2];
            double avgHum = agg[1] / agg[2];
            return String.format("%s : Température Moyenne = %.2f°F, Humidité Moyenne = %.1f%%", station, avgTemp, avgHum);
        });
        // 7. Output to Kafka topic "station-averages"
        averages.toStream().to("station-averages", Produced.with(Serdes.String(), Serdes.String()));

        // 8. Start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // 9. Ensure graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}