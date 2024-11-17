package utb.fai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Comparator;

import javax.swing.text.html.parser.ParserDelegator;

public class App {
	private static final ConcurrentHashMap<String, Integer> wordFrequency = new ConcurrentHashMap<>();
	private static final int DEFAULT_MAX_DEPTH = 2;
	private static final int DEFAULT_DEBUG_LEVEL = 0;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Missing parameter - start URL");
			return;
		}

		int maxDepth = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_MAX_DEPTH;
		int debugLevel = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_DEBUG_LEVEL;

		LinkedList<URIinfo> foundURIs = new LinkedList<>();
		ConcurrentHashMap<URI, Boolean> visitedURIs = new ConcurrentHashMap<>();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
			URI uri = new URI(args[0] + "/");
			foundURIs.add(new URIinfo(uri, 0));
			visitedURIs.put(uri, true);

			while (!foundURIs.isEmpty()) {
				URIinfo URIinfo = foundURIs.removeFirst();
				executor.submit(() -> processPage(URIinfo, visitedURIs, foundURIs, maxDepth, debugLevel));
			}

			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.MINUTES);

			// Print results
			printTopWords(20);

		} catch (Exception e) {
			System.err.println("Unhandled exception occurred:");
			e.printStackTrace();
		}
	}

	private static void processPage(URIinfo URIinfo, ConcurrentHashMap<URI, Boolean> visitedURIs, 
								  LinkedList<URIinfo> foundURIs, int maxDepth, int debugLevel) {
		try {
			ParserCallback callBack = new ParserCallback(visitedURIs, foundURIs, wordFrequency);
			callBack.depth = URIinfo.depth;
			callBack.maxDepth = maxDepth;
			callBack.debugLevel = debugLevel;
			callBack.pageURI = URIinfo.uri;

			if (debugLevel > 0) {
				System.err.println("Analyzing " + URIinfo.uri);
			}

			ParserDelegator parser = new ParserDelegator();
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(URIinfo.uri.toURL().openStream(), "UTF-8"));
			parser.parse(reader, callBack, true);
			reader.close();
		} catch (Exception e) {
			if (debugLevel > 0) {
				System.err.println("Error processing page: " + URIinfo.uri);
				e.printStackTrace();
			}
		}
	}

	private static void printTopWords(int limit) {
		Comparator<Map.Entry<String, Integer>> comparator = 
			Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
				.thenComparing(Map.Entry.comparingByKey());

		wordFrequency.entrySet().stream()
			.sorted(comparator)
			.limit(limit)
			.forEach(entry -> System.out.println(entry.getKey() + ";" + entry.getValue()));
	}
}
