import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args) {
        String filename = "piglatin_v0.zip";
//                String filename = "sl-en.zip";
//        String filename = "fr-en.zip";
        ibm1 ibm1 = new ibm1(filename);
        ibm1.runAlignmentALot();


//        SentencePairReader reader = new SentencePairReader(filename, false);
//        int maxLines = 10;
//        int line = 0;
//        while(reader.hasNext() && line++ < maxLines) {
//            SentencePair pair = reader.next();
//            System.out.printf("Sentence Pair %d\n", line);
//            System.out.printf("Source: %s\n", pair.getSource());
//            System.out.printf("Target: %s\n", pair.getTarget());
//            System.out.println();
//        }
    }
}