package marytts.language.id

import groovy.xml.*
import org.custommonkey.xmlunit.*
import org.testng.Assert
import org.testng.annotations.*

import marytts.datatypes.MaryData
import marytts.util.dom.DomUtils

/**
 * @author Tristan Hamilton
 */
class IndonesianPreprocessTest {

    def preprocessor

    @BeforeSuite
    void setUp() {
        preprocessor = new IndonesianPreprocess()
    }

    /**
     * Add any number of lemmas with their corresponding expected word form.
     * @return data provider 2-D array for parameterized testing
     */
    @DataProvider
    public Object[][] docData() {
        def data = []
        this.getClass().getResourceAsStream('preprocess_testdata.txt')?.splitEachLine(/,/){ toks ->
            data.add([toks[0], toks[1]])
        }
        return data
    }

    @DataProvider
	private Object[][] cardinalExpandData() {
		// @formatter:off
		def testArray = [
			[ "1", "satu" ], 
			[ "2", "dua" ],
			[ "3", "tiga" ],
			[ "4", "empat" ],
			[ "24", "dua puluh empat" ],
			[ "42", "empat puluh dua" ],
            [ "2000000", "dua juta" ],
            [ "3567", "tiga ribu lima ratus enam puluh tujuh" ]
        ]
		// @formatter:on
		return testArray
	}

    @Test(dataProvider = 'docData')
    void testProcess(String token, String word) {
        // create XML
        def xmlStr = new StreamingMarkupBuilder().bind {
            mkp.declareNamespace 'xsi': 'http://www.w3.org/2001/XMLSchema-instance', '': 'http://mary.dfki.de/2002/MaryXML'
            maryxml('xml:lang': 'id', version: 0.5) {
                p {
                    s {
                        phrase {
                            t token
                        }
                    }
                }
            }
        }.toString()

        // wrap XML into input MaryData
        def input = new MaryData(preprocessor.inputType, new Locale('id'))
        input.document = DomUtils.parseDocument xmlStr

        // expected XML string
        def xml = new XmlSlurper(false, false).parseText xmlStr
        xml.p.s.phrase.t[0].replaceNode {
            mtu(orig:token) {
                t(word)
            }
        }
        def expected = new StreamingMarkupBuilder().bind { mkp.yield xml }.toString()

        // actual XML string after processing
        def output = preprocessor.process input
        def actual = DomUtils.serializeToString output.document

        // compare XML
        XMLUnit.ignoreWhitespace = true
        def diff = XMLUnit.compareXML expected, actual
        def detailedDiff = new DetailedDiff(diff)
        Assert.assertTrue(detailedDiff.identical(), detailedDiff.toString())
    }

    @Test(dataProvider = 'cardinalExpandData')
    void testExpandCardinal(String token, String word) {
    	double x = Double.parseDouble(token);
		String actual = preprocessor.expandCardinal(x);
		Assert.assertEquals(actual, word);
    }
}
