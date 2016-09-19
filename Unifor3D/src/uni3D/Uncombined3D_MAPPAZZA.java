package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import utils.ArrayUtils;
import utils.ImageUtils;
import utils.InputOutput;
import utils.AboutBox;
import utils.MyCircleDetector;
import utils.MyConst;
import utils.MyFilter;
import utils.MyLine;
import utils.MyLog;
import utils.MyPlot;
import utils.MyStackUtils;
import utils.MyVersionUtils;
import utils.ReadDicom;
import utils.TableSequence;
import utils.TableUtils;
import utils.UtilAyv;

//=====================================================
//     Programma per plot 3D per immagini uncombined circolari
//     1 settembre 2016 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class Uncombined3D_MAPPAZZA implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = false;
	public static String VERSION = "CDQ 3D";

	public void run(String arg) {

		new AboutBox().about("Uncombined3D", MyVersion.CURRENT_VERSION);
		IJ.wait(20);
		new AboutBox().close();
		GenericDialog gd = new GenericDialog("", IJ.getInstance());
		String[] livelli = { "5", "4", "3", "2", "1" };
		gd.addChoice("SIMULATE", livelli, "3");
		gd.addCheckbox("ALL COILS", false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		String level = gd.getNextChoice();
		boolean tutte = gd.getNextBoolean();
		int livello = Integer.parseInt(level);
		ArrayList<Integer> pixListSignal11 = new ArrayList<Integer>();
		IJ.log("-----IW2AYV----");
		UtilAyv.logResizer(200, 200, 400, 400);
		try {
			Class.forName("utils.IW2AYV");
		} catch (ClassNotFoundException e) {
			IJ.error("ATTENZIONE, manca il file iw2ayv_xxx.jar");
			return;
		}
		ImagePlus imp10 = null;
		String path10 = null;
		int color0 = 0;
		boolean loop1 = true;
		ImagePlus impMappazza = null;
		boolean debug1 = false;
		String[] dir1a = null;
		String[] dir1b = null;

		if (tutte) {
			DirectoryChooser od1 = new DirectoryChooser("SELEZIONARE CARTELLA STACK");
			String dir1 = od1.getDirectory();
			dir1a = new File(dir1).list();
			dir1b = new String[dir1a.length];
			for (int i1 = 0; i1 < dir1a.length; i1++) {
				dir1b[i1] = dir1 + "\\" + dir1a[i1];
			}
		}

		int conta2 = -1;

		while (loop1) {

			if (tutte) {
				conta2++;
				if (conta2 >= dir1b.length) {
					loop1 = false;
					continue;
				}
				path10 = dir1b[conta2];
				IJ.log("carico stack " + (conta2 + 1) + " / " + dir1b.length);
				imp10 = UtilAyv.openImageNoDisplay(path10, false);
				if (imp10 == null) {
					MyLog.waitHere("imp10==null");
					continue;
				}

			} else {

				path10 = UtilAyv.imageSelection("SELEZIONARE LO STACK DA ELABORARE");
				if (path10 == null) {
					loop1 = false;
					continue;
				}
			}
			color0++;
			if (color0 > 3)
				color0 = 1;

			// imp10 = UtilAyv.openImageNormal(path10);
			imp10 = UtilAyv.openImageNoDisplay(path10, false);
			int width = imp10.getWidth();
			int height = imp10.getHeight();
			if (impMappazza == null) {
				impMappazza = generaMappazzaVuota(width, height, imp10.getImageStackSize());
			}

			ImageStack imaStack = imp10.getImageStack();
			if (imaStack == null) {
				IJ.log("imageFromStack.imaStack== null");
				return;
			}

			if (imaStack.getSize() < 2) {
				MyLog.waitHere("Per le elaborazioni 3D ci vuole uno stack di più immagini!");
				return;
			}
			int mode = 3;
			double profond = 30;
			mode = 0;

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {
				// if (!auto)
				// IJ.log("localizzo hotspot " + i1 + " / " +
				// imp10.getImageStackSize());
				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				if (imp20 == null)
					MyLog.waitHere("imp20==null");
				double[] pos20 = hotspotSearch(imp20, profond, "", mode, timeout);
				if (pos20 == null) {
					continue;
				}
				double diamMROI = 11;
				double xCenterRoi = pos20[0];
				double yCenterRoi = pos20[1];
				ImagePlus imp21 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				pixVectorize(imp21, xCenterRoi, yCenterRoi, diamMROI, pixListSignal11);
				// IJ.wait(timeout);
				imp20.close();
				imp21.close();
			}
			int[] pixListSignal = ArrayUtils.arrayListToArrayInt(pixListSignal11);
			double mean11 = UtilAyv.vetMean(pixListSignal);

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {
				// if (!auto)
				// IJ.log("calcolo mappazza " + i1 + " / " +
				// imp10.getImageStackSize());
				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				mappazzaColori(mean11, imp20, impMappazza, i1 + 1, livello, color0, debug1);
			}
			impMappazza.show();

			impMappazza.updateAndRepaintWindow();
			debug1 = true;
			if (!tutte)
				MyLog.waitHere();
		}

	} // chiude

	public static void pixVectorize(ImagePlus imp11, double xCenterMROI, double yCenterMROI, double diamMROI,
			ArrayList<Integer> pixList11) {

		if (imp11 == null)
			MyLog.waitHere("imp11==null");
		if (pixList11 == null)
			MyLog.waitHere("pixList1==null");

		imp11.setRoi((int) Math.round((xCenterMROI - 5.5)), (int) Math.round(yCenterMROI - 5.5), 11, 11);
		Roi roi11 = imp11.getRoi();
		ImageProcessor ip11 = imp11.getProcessor();
		if (ip11 == null)
			MyLog.waitHere("ip11==null");
		ImageProcessor mask11 = roi11 != null ? roi11.getMask() : null;
		Rectangle r11 = roi11 != null ? roi11.getBounds() : new Rectangle(0, 0, ip11.getWidth(), ip11.getHeight());
		for (int y = 0; y < r11.height; y++) {
			for (int x = 0; x < r11.width; x++) {
				if (mask11 == null || mask11.getPixel(x, y) != 0) {
					pixList11.add((int) ip11.getPixelValue(x + r11.x, y + r11.y));
				}
			}
		}
	}

	public static double[] hotspotSearch(ImagePlus imp11, double profond, String info1, int mode, int timeout) {

		boolean demo = false;
		if (mode == 10 || mode == 3) {
			demo = true;
		}

		if (imp11 == null)
			MyLog.waitHere("imp11==null");

		if (demo)
			imp11.show();
		ImageWindow iw11 = null;
		if (demo)
			iw11 = imp11.getWindow();

		double[] out10 = MyFilter.maxPosition11x11_NEW(imp11);
		if (out10 == null) {
			if (iw11 == null) {
			} else
				iw11.close();
			imp11.close();
			return null;
		}
		int latoMaxima = 11;
		int xMaxima = (int) Math.round(out10[0] - latoMaxima / 2);
		int yMaxima = (int) Math.round(out10[1] - latoMaxima / 2);

		imp11.setRoi(xMaxima, yMaxima, latoMaxima, latoMaxima);

		imp11.updateAndDraw();
		if (demo)
			IJ.wait(timeout);
		if (iw11 == null) {
		} else
			iw11.close();
		imp11.close();

		return out10;
	}

	public static ImagePlus generaMappazzaVuota(int width, int height, int depth) {

		int bitdepth = 24;
		ImageStack newStack = ImageStack.create(width, height, depth, bitdepth);
		ImagePlus impMappazza = new ImagePlus("MAPPAZZA", newStack);
		return impMappazza;
	}

	public static void mappazzaColori(double mean11, ImagePlus imp1, ImagePlus impMappazza, int slice, int livello,
			int color, boolean debug) {

		if (imp1 == null) {
			MyLog.waitHere("imp1==null");
			return;
		}
		if (impMappazza == null) {
			MyLog.waitHere("impMappazza==null");
			return;
		}

		int width = imp1.getWidth();
		int height = imp1.getHeight();
		short[] pixels1 = UtilAyv.truePixels(imp1);
		double mean = mean11;
		double minus20 = mean * MyConst.MINUS_20_PERC;
		double minus10 = mean * MyConst.MINUS_10_PERC;
		double plus10 = mean * MyConst.PLUS_10_PERC;
		double plus20 = mean * MyConst.PLUS_20_PERC;
		int colorOUT = 0;
		ImageStack stack1 = impMappazza.getStack();
		ImageProcessor ipMappa = stack1.getProcessor(slice);
		int[] pixelsMappa = (int[]) ipMappa.getPixels();
		short pixSorgente = 0;
		int aux1 = 0;
		int posizioneArrayImmagine = 0;
		int colorP20 = 0;
		int colorP10 = 0;
		int colorMED = 0;
		int colorM10 = 0;
		int colorM20 = 0;

		if (color == 1) {
			if (livello > 0)
				colorP20 = ((160 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff);
			if (livello > 1)
				colorP10 = ((160 & 0xff) << 16) | ((40 & 0xff) << 8) | (40 & 0xff);
			if (livello > 2)
				colorMED = ((160 & 0xff) << 16) | ((60 & 0xff) << 8) | (60 & 0xff);
			if (livello > 3)
				colorM10 = ((160 & 0xff) << 16) | ((80 & 0xff) << 8) | (80 & 0xff);
			if (livello > 4)
				colorM20 = ((160 & 0xff) << 16) | ((100 & 0xff) << 8) | (100 & 0xff);
		}
		if (color == 2) {
			if (livello > 0)
				colorP20 = ((0 & 0xff) << 16) | ((160 & 0xff) << 8) | (0 & 0xff);
			if (livello > 1)
				colorP10 = ((40 & 0xff) << 16) | ((160 & 0xff) << 8) | (40 & 0xff);
			if (livello > 2)
				colorMED = ((60 & 0xff) << 16) | ((160 & 0xff) << 8) | (60 & 0xff);
			if (livello > 3)
				colorM10 = ((80 & 0xff) << 16) | ((160 & 0xff) << 8) | (80 & 0xff);
			if (livello > 4)
				colorM20 = ((100 & 0xff) << 16) | ((160 & 0xff) << 8) | (100 & 0xff);
		}

		if (color == 3) {
			if (livello > 0)
				colorP20 = ((0 & 0xff) << 16) | ((0 & 0xff) << 8) | (160 & 0xff);
			if (livello > 1)
				colorP10 = ((40 & 0xff) << 16) | ((40 & 0xff) << 8) | (160 & 0xff);
			if (livello > 2)
				colorMED = ((60 & 0xff) << 16) | ((60 & 0xff) << 8) | (160 & 0xff);
			if (livello > 3)
				colorM10 = ((80 & 0xff) << 16) | ((80 & 0xff) << 8) | (160 & 0xff);
			if (livello > 4)
				colorM20 = ((100 & 0xff) << 16) | ((100 & 0xff) << 8) | (160 & 0xff);
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				posizioneArrayImmagine = y * width + x;
				pixSorgente = pixels1[posizioneArrayImmagine];
				if (pixSorgente > plus20) {
					aux1 = colorP20;
				} else if (pixSorgente > plus10) {
					aux1 = colorP10;
				} else if (pixSorgente > minus10) {
					aux1 = colorMED;
				} else if (pixSorgente > minus20) {
					aux1 = colorM10;
				} else if (pixSorgente > 100) {
					aux1 = colorM20;
				} else {
					aux1 = colorOUT;
				}
				int[] color1 = getColor(pixelsMappa[posizioneArrayImmagine]);

				int[] color2 = getColor(aux1);

				int color3 = mixColor(color1, color2);
				pixelsMappa[posizioneArrayImmagine] = color3;
			}
		}
		ipMappa.resetMinAndMax();
		return;
	}

	public static int[] getColor(int pixel) {
		int[] c1 = new int[3];
		int r1, g1, b1;
		r1 = (pixel & 0xff0000) >> 16;
		g1 = (pixel & 0xff00) >> 8;
		b1 = (pixel & 0xff);
		c1[0] = r1;
		c1[1] = g1;
		c1[2] = b1;
		return c1;
	}

	public static int mixColor(int[] rgb1, int[] rgb2) {
		int red = rgb1[0] + rgb2[0];
		if (red > 255)
			red = 255;
		int green = rgb1[1] + rgb2[1];
		if (green > 255)
			green = 255;
		int blue = rgb1[2] + rgb2[2];
		if (blue > 255)
			blue = 255;
		int color = ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
		return color;
	}

} // ultima
