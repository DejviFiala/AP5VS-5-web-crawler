package utb.fai;

import java.net.URI;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Třída ParserCallback je používána parserem DocumentParser,
 * je implementován přímo v JDK a umí parsovat HTML do verze 3.0.
 * Při parsování (analýze) HTML stránky volá tento parser
 * jednotlivé metody třídy ParserCallback, co nám umožuje
 * provádět s částmi HTML stránky naše vlastní akce.
 * 
 * @author Tomá Dulík
 */
class ParserCallback extends HTMLEditorKit.ParserCallback {

    /**
     * pageURI bude obsahovat URI aktuální parsované stránky. Budeme
     * jej vyuívat pro resolving všech URL, které v kódu stránky najdeme
     * - předtím, než nalezené URL uložíme do foundURLs, musíme z něj udělat
     * absolutní URL!
     */
    URI pageURI;

    /**
     * depth bude obsahovat aktuální hloubku zanoření
     */
    int depth = 0, maxDepth = 5;

    /**
     * visitedURLs je množina všech URL, které jsme již navtívili
     * (parsovali). Pokud najdeme na stránce URL, který je v této množině,
     * nebudeme jej u dále parsovat
     */
    private final ConcurrentHashMap<URI, Boolean> visitedURIs;

    /**
     * foundURLs jsou všechna nová (zatím nenavštívená) URL, která na stránce
     * najdeme. Poté, co projdeme celou stránku, budeme z tohoto seznamu
     * jednotlivá URL brát a zpracovvat.
     */
    private final LinkedList<URIinfo> foundURIs;

    /** pokud debugLevel>1, budeme vypisovat debugovací hlášky na std. error */
    int debugLevel = 0;

    private final ConcurrentHashMap<String, Integer> wordFrequency;

    ParserCallback(ConcurrentHashMap<URI, Boolean> visitedURIs, 
                  LinkedList<URIinfo> foundURIs,
                  ConcurrentHashMap<String, Integer> wordFrequency) {
        this.visitedURIs = visitedURIs;
        this.foundURIs = foundURIs;
        this.wordFrequency = wordFrequency;
    }

    /**
     * metoda handleSimpleTag se volá např. u značky <FRAME>
     */
    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        handleStartTag(t, a, pos);
    }

    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        URI uri;
        String href = null;
        if (debugLevel > 1)
            System.err.println("handleStartTag: " + t.toString() + ", pos=" + pos + ", attribs=" + a.toString());
        if (depth <= maxDepth)
            if (t == HTML.Tag.A)
                href = (String) a.getAttribute(HTML.Attribute.HREF);
            else if (t == HTML.Tag.FRAME)
                href = (String) a.getAttribute(HTML.Attribute.SRC);
        if (href != null)
            try {
                uri = pageURI.resolve(href);
                if (!uri.isOpaque() && !visitedURIs.containsKey(uri)) {
                    visitedURIs.put(uri, true);
                    foundURIs.add(new URIinfo(uri, depth + 1));
                    if (debugLevel > 0)
                        System.err.println("Adding URI: " + uri.toString());
                }
            } catch (Exception e) {
                System.err.println("Nalezeno nekorektní URI: " + href);
                e.printStackTrace();
            }

    }

    /******************************************************************
     * V metodě handleText bude probíhat veškerá činnost, související se
     * zjiováním četnosti slov v textovém obsahu HTML stránek.
     * IMPLEMENTACE TÉTO METODY JE V TÉTO ÚLOZE VAŠÍM ÚKOLEM !!!!
     * Možný postup:
     * Ve třídě Parser (klidně v její metodě main) si vytvořte vyhledávací tabulku
     * =instanci třídy HashMap<String,Integer> nebo TreeMap<String,Integer>.
     * Do této tabulky si ukládejte dvojice klíč-data, kde
     * klíčem jsou jednotlivá slova z textového obsahu HTML stránek,
     * data typu Integer bude dosavadní počet výskytu daného slova v
     * HTML stránkách.
     *******************************************************************/
    public void handleText(char[] data, int pos) {
        String text = new String(data);

        String[] words = text.split("[\\s,.!?;:\"'\\[\\]{}()\\n\\r\\t]+");
        
        for (String word : words) {

            word = word.trim();

            if (word.length() < 1) {
                continue;
            }

            if (word.matches("[0-9]+[xX][0-9a-fA-F]+,?")) {
                wordFrequency.merge(word.toLowerCase(), 1, Integer::sum);
                continue;
            }

            if (word.matches("[-=/{}>]+")) {
                wordFrequency.merge(word, 1, Integer::sum);
            }
        }
    }
}
