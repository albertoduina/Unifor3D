package uni3D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import utils.AboutBox;

//=====================================================
//     Elaborazioni Varie
//     12 marzo 2019 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class PennellFill implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = true;

	public void run(String arg) {

		new AboutBox().about("PennellFill", MyVersion.CURRENT_VERSION);
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

		GenericDialog gd = new GenericDialog("titolo");
		gd.addNumericField("valore delle roi di sx", 0, 0);
		gd.addNumericField("valore delle roi di dx", 0, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		double valSx = gd.getNextNumber();
		double valDx = gd.getNextNumber();
		ImageStack is1 = imp1.getStack();
		boolean fuori = false;
		boolean dentro = false;
		int size = is1.getSize();
		int width = is1.getWidth();
		int height = is1.getHeight();

		for (int i1 = 1; i1 <= size; i1++) {
			ImageProcessor ip1 = is1.getProcessor(i1);
			fuori = true;
			dentro = false;
			// scansione dx
			for (int x2 = width - 1; x2 >= 0; x2--) {
				fuori = true;
				for (int y2 = 0; y2 < height; y2++) {
					if (ip1.getPixelValue(x2, y2) > 0) {
						fuori = false;
						dentro = true;
						ip1.putPixelValue(x2, y2, valDx);
					}
				}
				// se a fine colonna sono ancora fuori vuol dire che la roi dx è terminata
				if (fuori && dentro) {
					break;
				}
			}
			fuori = true;
			dentro = false;
			// scansione sx
			for (int x1 = 0; x1 < width; x1++) {
				fuori = true;
				for (int y1 = 0; y1 < height; y1++) {
					if (ip1.getPixelValue(x1, y1) > 0) {
						fuori = false;
						dentro = true;
						ip1.putPixelValue(x1, y1, valSx);
					}
				}
				// se a fine colonna sono ancora fuori vuol dire che la roi sx è terminata
				if (fuori && dentro) {
					break;
				}
			}
			imp1.updateAndDraw();
		}

	}

}
