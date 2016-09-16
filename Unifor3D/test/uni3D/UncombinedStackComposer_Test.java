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
import utils.MyLog;
import utils.UtilAyv;

public class UncombinedStackComposer_Test {

	@Before
	public void setUp() throws Exception {
		new ImageJ(ImageJ.NORMAL);
	}

	@After
	public void tearDown() throws Exception {
		// new WaitForUserDialog("Do something, then click OK.").show();

	}

	@Test
	public final void testContaList() {

		String CODE_FILE = "contaList.txt";

		String[][] matStr0 = InputOutput.readStringMatrixFromFileNew2(CODE_FILE, ";");
//		// MyLog.logMatrixDimensions(matStr0, "MatStr0");
//		String[][] matStr1 = Uncombined3D_.minsort2(matStr0, 2, "");
//		// MyLog.logMatrixDimensions(matStr1, "MatStr1");
//		String[][] matStr2 = Uncombined3D_.minsort2(matStr1, 1, "");
//		MyLog.logMatrix(matStr2, "matStr2");
//		// MyLog.waitHere();
//		MyLog.logMatrixDimensions(matStr2, "MatStr2");
//		MyLog.waitHere();

		// ResultsTable rt3 = Uncombined3D_.vectorResultsTable2(matStr0);
		// rt3.show("INIZIALE");

		String[][] vetOut = UncombinedStackComposer_.contaList(matStr0);
		MyLog.logMatrixDimensions(vetOut, "vetOut");
		MyLog.here();

		MyLog.logMatrix(vetOut, "vetOut");
		MyLog.waitHere();

	}

}
