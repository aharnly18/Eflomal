import java.util.Scanner;
import java.util.ArrayList;

public class Sentence {
    public final int NULL_WORD = 0;
    private final ArrayList<Integer> tokens;
    public Sentence(String text) {
        tokens = new ArrayList<>();
        tokens.add(NULL_WORD); // begin all sentences with the null word
        try (Scanner scan = new Scanner(text.trim())) {
            while (scan.hasNextInt()) {
                tokens.add(scan.nextInt());
            }
            if (scan.hasNext()) {
                System.err.printf("WARNING: Sentence includes additional text: %s", scan.next());
            }
        }
    }

    public int size() {
        return tokens.size();
    }

    public int get(int i) {
        return tokens.get(i);
    }

    public ArrayList<Integer> getTokens() {
        return new ArrayList<>(tokens);
    }

    public String toString() {
        if (tokens.isEmpty()) {
            return "Sentence<NULL>";
        }
        StringBuilder sb = new StringBuilder("Sentence<");
        for (int i = 0; i < tokens.size()-1; i++) {
            sb.append(tokens.get(i));
            sb.append(' ');
        }
        sb.append(tokens.get(tokens.size() - 1));
        return String.format(sb.toString());
    }
}