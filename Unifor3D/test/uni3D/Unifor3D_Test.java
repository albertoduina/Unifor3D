package uni3D;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.ImageJ;
import ij.ImagePlus;
import utils.MyLog;
import utils.UtilAyv;



public class Unifor3D_Test {
	
		@Before
		public void setUp() throws Exception {
			new ImageJ(ImageJ.NORMAL);
		}

		@After
		public void tearDown() throws Exception {
			// new WaitForUserDialog("Do something, then click OK.").show();

		}
		@Test
		public final void testMainUniforTestGe() {

			assertTrue(true);
		}
		
		
		@Test
		public final void testPositionSearch11single() {


			String path1 = "./Dati/uno/002A";


			ImagePlus imp11 = UtilAyv.openImageNoDisplay(path1, true);

			boolean autoCalled = false;
			boolean step = true;
			boolean demo = false;
			boolean test = false;
			boolean fast = false;
			double maxFitError = 5;
			double maxBubbleGapLimit = 2;
			int timeout = 100;

			double out2[] = Unifor3D_.positionSearch11(imp11, maxFitError,
					maxBubbleGapLimit, "", autoCalled, step, demo, test, fast,
					timeout);

			// MyLog.logVector(out2, "out2");

			// 127.0, 116.0, 174.0, 127.0, 116.0, 155.0
			// 126.0, 115.0, 173.0, 126.0, 115.0, 154.0

			// 141.0, 130.0, 203.0, 141.0, 130.0, 181.0,
			double[] expected = { 141.0, 130.0, 203.0, 141.0, 130.0, 181.0 };
			// MyLog.logVector(expected, "expected");
			// MyLog.waitHere();
			MyLog.waitHere();

			boolean ok = UtilAyv.compareVectors(out2, expected, 0.001, "");
			assertTrue(ok);

		}
	}

