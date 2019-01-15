package com.agt.liveddatagen;


import com.agt.lived.DataGenerator;
import com.agt.lived.Generator.Sensors.Sensor;
import org.hobbit.core.Commands;
import org.hobbit.sdk.docker.builders.PullBasedDockersBuilder;
import org.hobbit.sdk.docker.builders.hobbit.DataGenDockerBuilder;
import org.hobbit.sdk.utils.CommandQueueListener;
import org.hobbit.sdk.utils.CommandSender;
import org.hobbit.sdk.utils.ComponentsExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;


import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.agt.lived.Constants.RDF_FORMAT;


public class DataGeneratorTest {
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void init() {

        environmentVariables.set("HOBBIT_RABBIT_HOST", "rabbit");
        environmentVariables.set("HOBBIT_SESSION_ID", "session_1");
        environmentVariables.set("HOBBIT_GENERATOR_ID", "1");
        environmentVariables.set("HOBBIT_GENERATOR_COUNT", "1");
        //environmentVariables.set(HOBBIT_SESSION_ID_KEY, "session_"+String.valueOf(new Date().getDate()));
    }

    @Test
    public void checkRDFFormat() throws Exception {
        DataGenerator dataGenerator = new DataGenerator();
        dataGenerator.outputFormat = RDF_FORMAT;
        dataGenerator.initHouseholds();
        List<Sensor> sensors = dataGenerator.getDevices();
        StringBuilder builder = new StringBuilder();
        for(Sensor sensor : sensors)
            builder.append(sensor.toRdfFormat());

        for(int i=0; i<20; i++) {
            for (Sensor sensor : sensors)
                builder.append(sensor.generateData());
        }
        System.out.print(builder.toString());
        Files.write(Paths.get("output.nt"), builder.toString().getBytes());
        //dataGenerator.generateData();
    }

    @Test
    public void checkHealth() throws Exception {
        DataGenDockerBuilder dataGeneratorBuilder = new DataGenDockerBuilder(new PullBasedDockersBuilder("git.project-hobbit.eu:4567/smirnp/grow-smarter-benchmark/datagen"));
        org.hobbit.core.components.Component datagen = dataGeneratorBuilder.build();

        CommandQueueListener commandQueueListener = new CommandQueueListener();
        ComponentsExecutor componentsExecutor = new ComponentsExecutor();

        componentsExecutor.submit(commandQueueListener);

        componentsExecutor.submit(datagen, "datagen", new String[]{
                "SPARQL_ENDPOINT_URL=http://172.17.0.2:8890/sparql",
                "HOUSES_COUNT=1",
                "DEVICES_PER_HOUSEHOLD_MIN=1",
                "DEVICES_PER_HOUSEHOLD_MAX=1",
                "SENSORS_PER_DEVICE=1",
                "ITERATIONS_LIMIT=30",
                "DATA_SENDING_PERIOD_MS=1000",
                "OUTPUT_FORMAT=RDF"
        });
        Thread.sleep(5000);
        new CommandSender(Commands.DATA_GENERATOR_START_SIGNAL).send();
        commandQueueListener.waitForTermination();

    }

    @Test
    public void checkRestEndpointHandler() throws Exception {


    }
}
