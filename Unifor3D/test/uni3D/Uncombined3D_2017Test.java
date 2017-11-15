package uni3D;

import static org.junit.Assert.assertTrue;

import java.awt.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.measure.ResultsTable;
import utils.InputOutput;
import utils.MyCircleDetector;
import utils.MyFilter;
import utils.MyLog;
import utils.UtilAyv;

public class Uncombined3D_2017Test {

	@Before
	public void setUp() throws Exception {
		new ImageJ(ImageJ.NORMAL);
	}

	@After
	public void tearDown() throws Exception {
		// new WaitForUserDialog("Do something, then click OK.").show();

	}

//	@Test
//	public final void testCoord3D() {
//
//		double[] point1 = { 86, 132, 10 };
//		double[] point2 = { 180, 240, 120 };
//
//		double[] out = Uncombined3D_2017.coord3D(point1, point2, 100);
//		MyLog.logVector(out, "out");
//		MyLog.waitHere();
//	}
}
