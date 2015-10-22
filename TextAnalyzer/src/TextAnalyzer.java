package org.myorg;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class TextAnalyzer {

    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        private Text context = new Text();
        private Text query = new Text();

        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            String article = value.toString();
            Scanner scanner = new Scanner(article);
            while (scanner.hasNextLine()) {
                String sentence = scanner.nextLine().toLowerCase();
                String[] tokens = sentence.split("[^A-Za-z0-9]+");
                HashMap<String, Integer> summary = new HashMap<String, Integer>();
                for (String token : tokens) {
                    if ("".equals(token)) continue;
                    if (summary.containsKey(token)) {
                        Integer val = summary.get(token) + 1;
                        summary.put(token, val);
                    } else {
                        summary.put(token, 1);
                    }
                }
                for (String s : summary.keySet()) {
                    context.set(s);
                    for (String t : summary.keySet()) {
                        if (!t.equals(s)) {
                            query.set(t + "#" + summary.get(t));
                            output.collect(context, query);
                        }
                    }
                }
            }
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
                 HashMap<String, Integer> summary = new HashMap<String, Integer>();
            while (values.hasNext()) {
                String collector = values.next().toString();
                String word = collector.split("#")[0];
                Integer number = Integer.parseInt(collector.split("#")[1]);
                if (summary.containsKey(word)) {
                    Integer num = summary.get(word) + number;
                    summary.put(word, num);
                } else {
                    summary.put(word, number);
                }
            }
            output.collect(key, new Text(""));
            for (String s : summary.keySet()) {
                Text queryword = new Text("<" + s + ",");
                Text occurrence = new Text(summary.get(s).toString() + ">");
                output.collect(queryword, occurrence);
            }
            output.collect(new Text(""), new Text(""));
        }
    }

    public static void main(String[] args) throws Exception {
        JobConf conf = new JobConf(TextAnalyzer.class);
        
        conf.setJobName("textanalyzer");
        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);
        conf.setMapperClass(Map.class);
        conf.setReducerClass(Reduce.class);
        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        JobClient.runJob(conf);
    }
}
