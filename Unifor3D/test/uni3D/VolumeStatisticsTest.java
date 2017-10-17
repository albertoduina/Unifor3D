package uni3D;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import utils.MyLog;
import utils.UtilAyv;

public class VolumeStatisticsTest {

	@Before
	public void setUp() throws Exception {
		new ImageJ(ImageJ.NORMAL);
	}

	@After
	public void tearDown() throws Exception {
		// new WaitForUserDialog("Do something, then click OK.").show();

	}

	@Test
	public final void testPixStackVectorize() {

		String pathImage = "./Data/image.tif";
		ImagePlus impImage = UtilAyv.openImageNoDisplay(pathImage, false);

		String pathMask = "./Data/mask.tif";
		ImagePlus impMask = UtilAyv.openImageNoDisplay(pathMask, false);

		ArrayList<Integer> pixList = new ArrayList<Integer>();
		VolumeStatistics.pixStackVectorize(impImage, impMask, pixList, 0);
		MyLog.logArrayListInteger(pixList, "pixList");
		MyLog.waitHere();
	}

	@Test
	public final void testPixStackVectorizeString() {

		String pathImage = "./Data/image.tif";
		ImagePlus impImage = UtilAyv.openImageNoDisplay(pathImage, false);

		String pathMask = "./Data/mask.tif";
		ImagePlus impMask = UtilAyv.openImageNoDisplay(pathMask, false);

		ArrayList<String> pixList = new ArrayList<String>();
		VolumeStatistics.pixStackVectorizeString(impImage, impMask, pixList, 0);
		MyLog.logArrayListVertical(pixList);
		MyLog.waitHere();
	}

}
