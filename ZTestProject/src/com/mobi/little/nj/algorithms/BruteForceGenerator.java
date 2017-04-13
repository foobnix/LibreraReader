package com.mobi.little.nj.algorithms;

/**
 * A class to create a set of permutations recursively based on an
 * arbitrary array of bytes
 */
public class BruteForceGenerator
{
    /**
     * An interface for users of the generator to influence
     * its operation
     */
    interface Callback
    {
        /**
         * Return true when the algorithm finds the permutation you're
         * interested in
         *
         * @param permutation
         * @return
         */
        boolean found(byte[] permutation);

        /**
         * Return true to allow the algorithm to skip a branch of the
         * search
         *
         * @param branch
         * @return
         */
        boolean prune(byte[] branch);
    }

    /**
     * Gets a generator for upper case english letters
     *
     * @param cb
     * @return
     */
    public static BruteForceGenerator createUpperCaseEnglish(Callback cb)
    {
        return new BruteForceGenerator(cb, "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes());
    }

    final Callback cb;
    final byte[] sym;
    boolean stop;

    public BruteForceGenerator(Callback cb, byte... symbols)
    {
        if (symbols.length == 0)
            throw new IllegalArgumentException("No Symbols");

        this.cb = cb;
        this.sym = symbols;
    }

    /**
     * Start generating permutations of the specified length
     *
     * @param len
     */
    public void start(int len)
    {
        stop = false;
        recurse(new byte[len], len - 1);
    }

    private void recurse(byte[] word, int at)
    {
        if (at < 0)
        {
            stop = cb.found(word);
            return;
        }

        for(byte i : sym)
        {
            if (stop)
                return;

            word[at] = i;

            if (!cb.prune(word))
            {
                recurse(word.clone(), at - 1);
            }
        }
    }
}
