package net.gmbx.w2v;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class Distance
{
    private static final int DEFAULT_NEIGHBORHOOD = 40;

    public static void main(String[] args) throws ClassNotFoundException,
        IOException
    {

        if (args.length < 1 || args.length > 2)
        {
            System.err
                .println("Usage: path/to/word2vec_model [N-neighbors]");
            System.exit(1);
        }

        int N = DEFAULT_NEIGHBORHOOD;
        if (args.length == 2)
        {
            try
            {
                N = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException nfe)
            {
                System.err
                    .println("Usage: path/to/binary_word2vec_model [N-neighbors]");
                System.exit(1);
            }
        }

        long t0 = System.currentTimeMillis();
        VectorModel model;
        if (args[0].endsWith(".bin"))
        {
            model = Word2VecUtils.loadGoogleBinary(args[0], Charset
                .defaultCharset(), true);
        }
        else
        {
            model = Word2VecUtils.loadVectorModelFromText(args[0], true);
        }
        long t1 = System.currentTimeMillis();
        String time = (t1 - t0) / 1000 + "." + (t1 - t0) % 1000 + "s";
        System.out.println(time + " to load " + model.getVocabSize() + " "
                           + model.getVectorSize()
                           + "-dimensional word vectors");

        InputStream in = System.in;
        String prompt = "\nEnter a word or short phrase (EXIT to break): ";
        System.out.print(prompt);

        BufferedReader br = new BufferedReader(new InputStreamReader(in,
            Charset.forName("UTF-8")));

        String line = null;
        while ((line = br.readLine()) != null && !"EXIT".equals(line))
        {
            String input = Word2VecUtils.normalizePreservingUnderscores(line);
            List<Integer> ids = new ArrayList<Integer>();

            Integer index = model.getIndex(input);
            if (index != null)
            {
                ids.add(index);
            }
            if (index == null)
            {
                for (String token : input.split("\\s+"))
                {
                    index = model.getIndex(token);
                    if (index != null)
                    {
                        ids.add(index);
                    }
                }
            }
            if (ids.isEmpty())
            {
                System.out.println("\nOut of dictionary word!");
            }
            else
            {
                int[] searchIDs = new int[ids.size()];
                for (int i = 0; i < ids.size(); i++)
                {
                    searchIDs[i] = ids.get(i).intValue();
                }
                printNearestNeighbors(line, searchIDs, model, N);
            }
            System.out.print(prompt);
        }
    }

    private static void printNearestNeighbors(String input,
                                              int[] searchIDs,
                                              VectorModel model,
                                              int k)
    {
        for (Integer id : searchIDs)
        {
            System.out.println(String
                .format("\nWord: %s  "
                        + "Position in vocabulary: %d", model.getTerm(id), id));
        }

        float[] target = getTargetVector(model, searchIDs);

        LinkedHashMap<String, Double> results =
                findNearestNeighbors(model, target, searchIDs, k);

        System.out
            .println("\n                                      "
                     + "Related Term         Cosine Similarity");
        System.out
            .println("----------------------------------------"
                     + "------------------------------------");

        for (Entry<String, Double> result : results.entrySet())
        {
            System.out.println(String.format("%50s%22.6f", result.getKey(),
                                             result.getValue()));
        }
    }

    private static float[] getTargetVector(VectorModel model,
                                           int[] searchIDs)
    {
        float[] target = null;
        if (searchIDs.length == 1)
        {
            target = model.getVector(searchIDs[0]);
        }
        else if (searchIDs.length > 1)
        {
            target = model.composeUnitVector(searchIDs);
        }
        return target;
    }

    private static LinkedHashMap<String, Double> findNearestNeighbors(VectorModel model,
                                                                      float[] target,
                                                                      int[] searchIDs,
                                                                      int radius)
    {
        LinkedHashMap<String, Double> result = new LinkedHashMap<String, Double>();

        int[] nearestNeighbors = new int[radius];
        double[] score = new double[radius];

        // initialize array to hold k-nearest neighbors
        for (int i = 0; i < radius; i++)
        {
            score[i] = Double.MIN_VALUE;
        }

        for (int i = 0; i < model.getVocabSize(); i++)
        {
            if (isContainedIn(i, searchIDs))
            {
                continue;
            }
            else
            {
                double cos_sim = dotProduct(target, model.getVector(i));

                for (int j = 0; j < radius; j++)
                {
                    if (cos_sim > score[j])
                    {
                        for (int k = radius - 1; k > j; k--)
                        {
                            score[k] = score[k - 1];
                            nearestNeighbors[k] = nearestNeighbors[k - 1];
                        }
                        score[j] = cos_sim;
                        nearestNeighbors[j] = i;
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < radius; i++)
        {
            String term = model.getTerm(nearestNeighbors[i]);
            result.put(term, score[i]);
        }
        return result;
    }

    private static boolean isContainedIn(int id, int[] searchIDs)
    {
        for (int i = 0; i < searchIDs.length; i++)
        {
            if (searchIDs[i] == id)
            {
                return true;
            }
        }
        return false;
    }

    private static double dotProduct(float[] u, float[] v)
    {
        double res = 0;
        for (int i = 0; i < u.length; i++)
        {
            res += (u[i] * v[i]);
        }
        return res;
    }

}
