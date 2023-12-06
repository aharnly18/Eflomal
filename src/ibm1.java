import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;

public class ibm1 {

    private Random r = new Random(10);
    private final int NULL_WORD = 0;
    private final int NULL_LINK = 0xFFFF;
    private final double NULL_PRIOR = 0.2; // this seems to be what's used in eflomal
    private final double LEX_ALPHA = 0.001;
    private final double NULL_ALPHA = 0.001;
    private ArrayList<ArrayList<Integer>> links = new ArrayList<>();
    private int maxLines = 10000;
    private ArrayList<SentencePair> corpus = new ArrayList<>();
    private TreeMap<MutablePair<Integer, Integer>, Integer> counts = new TreeMap<>();
    private ArrayList<TreeMap<Integer, Double>> dirichlet = new ArrayList<>();
    private ArrayList<TreeMap<Integer, Double>> priors = new ArrayList<>();

    private boolean argmax = false;

    public ibm1(String filename) {
        readSrcTrg(filename);
        initializeLinksAndCounts();
    }

    public void readSrcTrg(String filename) {

        SentencePairReader reader = new SentencePairReader(filename, false);
        int line = 0;

        while (reader.hasNext() && line++ < maxLines) {
            SentencePair pair = reader.next();
            corpus.add(pair);
        }
    }

    public void initializeLinksAndCounts() {

        for (int i = 0; i < corpus.size(); i++) {
            links.add(i, new ArrayList<>());
            int srcLength = corpus.get(i).getSource().size();
            int trgLength = corpus.get(i).getTarget().size();
            for (int j = 0; j < trgLength; j++) {
                // to initialize counts:
                // if exists, increment counts value
                // otherwise, add new counts pair with 1
                int linkIndex = r.nextInt(srcLength);
                links.get(i).add(linkIndex);
                MutablePair<Integer, Integer> pairToUpdate = new MutablePair<>(corpus.get(i).getTarget().get(j), corpus.get(i).getSource().get(linkIndex));
                if (counts.containsKey(pairToUpdate)) {
                    counts.put(pairToUpdate, counts.get(pairToUpdate) + 1);
                } else {
                    counts.put(pairToUpdate, 1);
                }
            }
        }
    }


    public void alignCorpusOnce() {

        for (int k = 0; k < corpus.size(); k++) {
            Sentence S = corpus.get(k).getSource();
            Sentence T = corpus.get(k).getTarget();
            for (int j = 0; j < T.size(); j++) { // j: index of trg word in trg sentence
                int t = T.get(j);
                // get the word index in the source sentence that previously mapped to this word
                int old_i = links.get(k).get(j); // word index of the src word from src sentence it maps to
                // get that word token from the source sentence
                int old_s = -1;
                if (old_i == NULL_LINK) {
                    old_s = NULL_WORD;
                } else {
                    old_s = S.get(old_i);
                }
                MutablePair<Integer, Integer> pairToUpdate = new MutablePair<>(t, old_s);
                if (counts.containsKey(pairToUpdate)) { // this should always be true
                    if (counts.get(pairToUpdate) <= 1) {
                        // if counts reaches 0 clear these entries for RAM
                        counts.remove(pairToUpdate);
                        if (dirichlet.size() > t && dirichlet.get(t) != null) {
                            dirichlet.get(t).remove(old_s);
                        }
                    } else {
                        // decrease the count and dirichlet prior of the word old_s
                        counts.put(pairToUpdate, counts.get(pairToUpdate) - 1);
                        double dirichletVal = LEX_ALPHA; // handle case where dirichlet is null
                        if (dirichlet.size() > t && dirichlet.get(t) != null && dirichlet.get(t).containsKey(old_s)) {
                            dirichletVal = dirichlet.get(t).get(old_s);
                        }
                        double newDirichletVal = 1 / (1 / dirichletVal - 1);
                        if (dirichlet.size() <= t) {
                            while (dirichlet.size() <= t) {
                                dirichlet.add(dirichlet.size(), new TreeMap<>());
                            }
                        }
                        dirichlet.get(t).put(old_s, newDirichletVal);
                    }
                }

                // update probabilities assuming this pair is unmapped
                double ps_sum = 0;
                ArrayList<Double> ps = new ArrayList<>(); // one entry per word in source sentence + 1
                for (int i = 0; i < S.size(); i++) {
                    int s = S.get(i); // ith word in src
                    // get the number of times that t is caused by s
                    int n = counts.getOrDefault(new MutablePair<>(t, s), 0);
                    // get the prior count of t caused by s
                    double alpha = 0;
                    if (priors.size() > t) {
                        alpha = priors.get(t).get(s) + LEX_ALPHA;
                    } else {
                        alpha = LEX_ALPHA;
                    }
                    // multiply the estimated probabilities (dirichlet) by the counts to get quality
                    double dirichletVal = LEX_ALPHA;
                    if (dirichlet.size() > t && dirichlet.get(t) != null && dirichlet.get(t).containsKey(s)) { // this is me...I assume this is a good way to handle default cases?
                        dirichletVal = dirichlet.get(t).get(s);
                    }
                    ps_sum += dirichletVal * (alpha + n);
                    // add this number to the cumulative probability distribution
                    ps.add(i, ps_sum);
                    // include null word in the sum
                    double dirichletValNull = LEX_ALPHA;
                    if (dirichlet.size() > t && dirichlet.get(t) != null && dirichlet.get(t).containsKey(NULL_WORD)) {
                        dirichletValNull = dirichlet.get(t).get(NULL_WORD);
                    }
                    ps_sum += NULL_PRIOR * dirichletValNull * // change from pseudocode: +=
                            (NULL_ALPHA + counts.getOrDefault(new MutablePair<>(t, NULL_WORD), 0));
                }
                ps.add(S.size(), ps_sum);

                // determine based on ps_sum which source token caused the target token

                // select a new_i to replace old_i
                int new_i = -1;
                int new_s = -1;
                if (!argmax) {
                    // the probability of any i is proportional to its probability in ps
                    new_i = random_categorical_from_cumulative(ps);
                } else {
                    // whichever i is most probable based on ps will be chosen
                    new_i = max_categorical_from_cumulative(ps);
                }
                // identify the word token that goes with this sentence index
                if (new_i < S.size()) {
                    new_s = S.get(new_i);
                    links.get(k).set(j, new_i); // change from pseudocode
                } else {
                    new_s = NULL_WORD;
                    links.get(k).set(j, NULL_LINK); // change from pseudocode
                }

                // increase the count and dirichlet variables to reflect the new i and s
                pairToUpdate = new MutablePair<>(t, new_s);
                if (counts.containsKey(pairToUpdate)) {
                    counts.put(pairToUpdate, counts.get(pairToUpdate) + 1);
                } else {
                    counts.put(pairToUpdate, 1);
                }
                double dirichletVal = LEX_ALPHA;
                if (dirichlet.size() > t && dirichlet.get(t) != null && dirichlet.get(t).containsKey(new_s)) {
                    dirichletVal = dirichlet.get(t).get(new_s);
                }
                double newDirichletVal = 1 / (1 / dirichletVal + 1);
                if (dirichlet.size() <= t) {
                    while (dirichlet.size() <= t) {
                        dirichlet.add(dirichlet.size(), new TreeMap<>());
                    }
                } else if (dirichlet.get(t) == null) {
                    dirichlet.set(t, new TreeMap<>());
                }
                dirichlet.get(t).put(new_s, newDirichletVal);
            }
        }
    }

    public void runAlignmentALot() {
        TreeMap<MutablePair<Integer, Integer>, Integer> sortedCounts;
        for (int i = 1; i < 20000; i++) {
            alignCorpusOnce();
        }
    }

    private int max_categorical_from_cumulative(ArrayList<Double> ps) {
        double best_p = ps.get(0);
        int new_i = 0;
        for (int i = 1; i < ps.size(); i++) {
            double p = ps.get(i) - ps.get(i - 1);
            if (p > best_p) {
                new_i = i;
                best_p = p;
            }
        }
        return new_i;
    }

    private int random_categorical_from_cumulative(ArrayList<Double> ps) {
        double max = ps.get(ps.size() - 1);
        double randomValue = max * r.nextDouble();
        for (int i = 0; i < ps.size() - 1; i++) {
            if (ps.get(i) >= randomValue) {
                return i;
            }
        }
        return ps.size() - 1;
    }

}