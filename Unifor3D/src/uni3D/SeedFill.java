package uni3D;

import java.awt.Polygon;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import utils.AboutBox;
import utils.ButtonMessages;

//=====================================================
//     Elaborazioni Varie
//     12 marzo 2019 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class SeedFill implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = true;

	public void run(String arg) {

		new AboutBox().about("SeedFill", MyVersion.CURRENT_VERSION);
		IJ.wait(1000);
		new AboutBox().close();

		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.noImage();
			return;
		}
		String[] titles = new String[wList.length];
		ImagePlus imp1 = null;
		for (int i = 0; i < wList.length; i++) {
			imp1 = WindowManager.getImage(wList[i]);
			if (imp1 != null)
				titles[i] = imp1.getTitle();
			else
				titles[i] = "";
		}

		String oldTool = IJ.getToolName();
		IJ.setTool("multi");

		int ret = 0;
		do {
			imp1.killRoi();
			ImageProcessor ip1 = imp1.getProcessor();

			ret = ButtonMessages.ModelessMsg("Cliccare all'interno della ROI, poi dare OK, altrimenti premere FINE",
					"OK", "FINE");
			if (ret == 2) {
				Polygon poli1 = imp1.getRoi().getPolygon();

				int[] xPoints = poli1.xpoints;
				int[] yPoints = poli1.ypoints;
				int x = xPoints[0];
				int y = yPoints[0];
				int z = imp1.getCurrentSlice();
				// clicca sulla prima roi
				// versione 2D

				// int fillColor = (int) ip1.getPixelValue(x, y);
				// int boundaryColor = 0;
				// boundaryFill8(x, y, 254, boundaryColor, ip1);
				// imp1.updateAndDraw();
				// ip1 = imp1.getProcessor();
				// boundaryFill8(x, y, fillColor, boundaryColor, ip1);
				// imp1.updateAndDraw();

				// versione 3D

				double fillColor = (int) ip1.getPixelValue(x, y);
				double boundaryColor = 0;
				ImageStack istack = imp1.getStack();
				ImageStack fstack = istack.convertToFloat();

				boundaryFill26(x, y, z, 254.0, boundaryColor, fstack);
				imp1.updateAndDraw();
				ip1 = imp1.getProcessor();
				boundaryFill26(x, y, z, fillColor, boundaryColor, fstack);
				imp1.updateAndDraw();

			}
		} while (ret != 1);

		IJ.setTool(oldTool);

	}

	// Function for 8 connected Pixels
	void boundaryFill8(int x, int y, int fillColor, int boundaryColor, ImageProcessor ip1) {
		if (ip1.getPixelValue(x, y) != boundaryColor && ip1.getPixelValue(x, y) != fillColor) {
			ip1.putPixelValue(x, y, fillColor);
			boundaryFill8(x + 1, y, fillColor, boundaryColor, ip1);
			boundaryFill8(x, y + 1, fillColor, boundaryColor, ip1);
			boundaryFill8(x - 1, y, fillColor, boundaryColor, ip1);
			boundaryFill8(x, y - 1, fillColor, boundaryColor, ip1);
			boundaryFill8(x - 1, y - 1, fillColor, boundaryColor, ip1);
			boundaryFill8(x - 1, y + 1, fillColor, boundaryColor, ip1);
			boundaryFill8(x + 1, y - 1, fillColor, boundaryColor, ip1);
			boundaryFill8(x + 1, y + 1, fillColor, boundaryColor, ip1);
		}
	}

	/***
	 * Function 3D for 26 connected Pixels
	 * 
	 * @param x             coordinata
	 * @param y             coordinata
	 * @param z             coordinata
	 * @param fillColor     double riempimento
	 * @param boundaryColor double sfondo
	 * @param is1           float stack
	 */
	void boundaryFill26(int x, int y, int z, double fillColor, double boundaryColor, ImageStack is1) {
		if (is1.getVoxel(x, y, z) != boundaryColor && is1.getVoxel(x, y, z) != fillColor) {
			is1.setVoxel(x, y, z, fillColor);
			// slice z0
			boundaryFill26(x - 1, y + 1, z, fillColor, boundaryColor, is1); // a
			boundaryFill26(x, y + 1, z, fillColor, boundaryColor, is1); // b
			boundaryFill26(x + 1, y + 1, z, fillColor, boundaryColor, is1); // c
			boundaryFill26(x - 1, y, z, fillColor, boundaryColor, is1); // d
//			boundaryFill26(x , y, z,  fillColor, boundaryColor, is1); //e
			boundaryFill26(x + 1, y, z, fillColor, boundaryColor, is1); // f
			boundaryFill26(x - 1, y - 1, z, fillColor, boundaryColor, is1);// g
			boundaryFill26(x, y - 1, z, fillColor, boundaryColor, is1);// h
			boundaryFill26(x + 1, y - 1, z, fillColor, boundaryColor, is1);// i
			// slice z+1
			boundaryFill26(x - 1, y + 1, z + 1, fillColor, boundaryColor, is1); // a
			boundaryFill26(x, y + 1, z + 1, fillColor, boundaryColor, is1); // b
			boundaryFill26(x + 1, y + 1, z + 1, fillColor, boundaryColor, is1); // c
			boundaryFill26(x - 1, y, z + 1, fillColor, boundaryColor, is1); // d
			boundaryFill26(x, y, z + 1, fillColor, boundaryColor, is1); // e
			boundaryFill26(x + 1, y, z + 1, fillColor, boundaryColor, is1); // f
			boundaryFill26(x - 1, y - 1, z + 1, fillColor, boundaryColor, is1);// g
			boundaryFill26(x, y - 1, z + 1, fillColor, boundaryColor, is1);// h
			boundaryFill26(x + 1, y - 1, z + 1, fillColor, boundaryColor, is1);// i
			// slice z-1
			boundaryFill26(x - 1, y + 1, z - 1, fillColor, boundaryColor, is1); // a
			boundaryFill26(x, y + 1, z - 1, fillColor, boundaryColor, is1); // b
			boundaryFill26(x + 1, y + 1, z - 1, fillColor, boundaryColor, is1); // c
			boundaryFill26(x - 1, y, z - 1, fillColor, boundaryColor, is1); // d
			boundaryFill26(x, y, z - 1, fillColor, boundaryColor, is1); // e
			boundaryFill26(x + 1, y, z - 1, fillColor, boundaryColor, is1); // f
			boundaryFill26(x - 1, y - 1, z - 1, fillColor, boundaryColor, is1);// g
			boundaryFill26(x, y - 1, z - 1, fillColor, boundaryColor, is1);// h
			boundaryFill26(x + 1, y - 1, z - 1, fillColor, boundaryColor, is1);// i

		}
	}

	public static int howmanyPoints(Polygon poli1) {
		int nPunti;
		if (poli1 == null) {
			nPunti = 0;
		} else {
			nPunti = poli1.npoints;
		}
		return nPunti;
	}

	/** Creates a integer version of stack. */
	public ImageStack convertToInteger(ImageStack sfloat) {
		// leggo le dimensioni
		int width = sfloat.getWidth();
		int height = sfloat.getHeight();
		int size = sfloat.getSize();
		ImageStack stack2 = new ImageStack(width, height, sfloat.getColorModel());
		for (int i1 = 1; i1 <= size; i1++) {
			ImageProcessor ip2 = sfloat.getProcessor(i1);
			ip2 = ip2.convertToShortProcessor();
			stack2.addSlice(sfloat.getSliceLabel(i1), ip2);
		}
		return stack2;
	}

}
