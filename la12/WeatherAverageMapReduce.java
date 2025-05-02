import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.StringTokenizer;

public class WeatherAverage {

    // Custom Writable class can be defined here if required, but we use Text for key and a composite value as String for simplicity

    // Mapper Class: Processes each line and emits a composite key and a value for each variable.
    public static class WeatherMapper extends Mapper<Object, Text, Text, Text> {

        private Text variableKey = new Text();
        // The value string will contain the numerical value and a count "1"
        private Text valueAndCount = new Text();

        // Example of input record (assumed CSV format):
        // Date,Temperature,DewPoint,WindSpeed,...
        // 2025-04-01,20.5,10.0,5.5,...
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            // Skip header line if present
            String line = value.toString().trim();
            if (line.startsWith("Date") || line.isEmpty()) {
                return;
            }
            // Tokenize assuming comma separated fields.
            String[] tokens = line.split(",");
            // Expecting tokens[1] = Temperature, tokens[2] = DewPoint, tokens[3] = WindSpeed.
            try {
                double temp = Double.parseDouble(tokens[1]);
                double dew = Double.parseDouble(tokens[2]);
                double wind = Double.parseDouble(tokens[3]);
                // Emit for Temperature, DewPoint, and WindSpeed
                context.write(new Text("Temperature"), new Text(temp + ",1"));
                context.write(new Text("DewPoint"), new Text(dew + ",1"));
                context.write(new Text("WindSpeed"), new Text(wind + ",1"));
            } catch (NumberFormatException e) {
                // Skip invalid record
            }
        }
    }

    // Reducer Class: Sums up the values and counts for each variable and calculates the average.
    public static class AverageReducer extends Reducer<Text, Text, Text, DoubleWritable> {
        private DoubleWritable averageWritable = new DoubleWritable();

        public void reduce(Text key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            double sum = 0.0;
            long count = 0;
            for (Text val : values) {
                // Each value is in the form "value,count"
                String[] parts = val.toString().split(",");
                sum += Double.parseDouble(parts[0]);
                count += Long.parseLong(parts[1]);
            }
            double avg = (count == 0) ? 0.0 : sum / count;
            averageWritable.set(avg);
            context.write(key, averageWritable);
        }
    }

    // Main Driver: Configures and runs the job
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: WeatherAverage <input path> <output path>");
            System.exit(-1);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Weather Average Calculation");
        job.setJarByClass(WeatherAverage.class);
        job.setMapperClass(WeatherMapper.class);
        job.setReducerClass(AverageReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}





































Step 1: Install Java and Hadoop
If you haven't installed Hadoop yet, run:

bash
sudo apt update
sudo apt install openjdk-11-jdk
wget https://downloads.apache.org/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
tar -xzf hadoop-3.3.6.tar.gz
mv hadoop-3.3.6 /usr/local/hadoop
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export HADOOP_HOME=/usr/local/hadoop
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
(Configure Hadoop further in $HADOOP_HOME/etc/hadoop/ if needed.)

Step 2: Prepare the Input File
Create a sample CSV file (weather_data.csv):

bash
echo -e "Date,Temperature,DewPoint,WindSpeed\n2025-04-01,20.5,10.0,5.5\n2025-04-02,22.0,11.5,6.0\n2025-04-03,19.5,9.5,4.5" > weather_data.csv
Upload it to HDFS:

bash
hdfs dfs -mkdir -p /input
hdfs dfs -put weather_data.csv /input/
Step 3: Compile the Java Code
Save the code as WeatherAverage.java.

Compile it using Hadoop classpath:

bash
javac -classpath $(hadoop classpath) WeatherAverage.java
Create a JAR file:

bash
jar cf weather_avg.jar WeatherAverage*.class
Step 4: Run the Hadoop Job
bash
hadoop jar weather_avg.jar WeatherAverage /input/weather_data.csv /output
Step 5: Check the Output
bash
hdfs dfs -cat /output/part-r-00000
Expected Output:

DewPoint    10.333333333333334
Temperature 20.666666666666668
WindSpeed   5.333333333333333
Step 6: Clean Up (Optional)
bash
hdfs dfs -rm -r /output  # Remove output directory if re-running
Troubleshooting
"Class not found" error? → Ensure the JAR was created correctly.

HDFS permission issues? → Run hdfs dfs -chmod -R 777 /input (not recommended for production).

Hadoop not running? → Start Hadoop services with:

bash
start-dfs.sh
start-yarn.sh
