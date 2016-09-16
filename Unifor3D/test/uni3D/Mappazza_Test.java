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

public class Mappazza_Test {

	@Before
	public void setUp() throws Exception {
		new ImageJ(ImageJ.NORMAL);
	}

	@After
	public void tearDown() throws Exception {
		// new WaitForUserDialog("Do something, then click OK.").show();

	}


	@Test
	public final void testGeneraMappazzaVuota() {
		int width = 256;
		int height = 256;
		int size = 176;

		ImagePlus impMappazza = Uncombined3D_MAPPAZZAold.generaMappazzaVuota(width, height, size);


	}

}
