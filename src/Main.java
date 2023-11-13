public class Main {
    public static void main(String[] args) {
        String filename = "piglatin_v2.zip";
        SentencePairReader reader = new SentencePairReader(filename, false);
        int maxLines = 10;
        int line = 0;
        while(reader.hasNext() && line++ < maxLines) {
            SentencePair pair = reader.next();
            System.out.printf("Sentence Pair %d\n", line);
            System.out.printf("Source: %s\n", pair.getSource());
            System.out.printf("Target: %s\n", pair.getTarget());
            System.out.println();
        }
    }
}