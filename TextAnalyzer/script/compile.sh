javac -classpath `hadoop classpath` -d textAnalyzer_class TextAnalyzer.java
jar -cvf TextAnalyzer.jar -C textAnalyzer_class/ .