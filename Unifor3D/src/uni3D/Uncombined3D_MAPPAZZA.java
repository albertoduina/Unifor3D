package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
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
		String def1 = Prefs.get("prefer.Uncombined3D_MAPPAZZA_def1", "5");

		GenericDialog gd = new GenericDialog("", IJ.getInstance());
		String[] livelli = { "12", "11", "10", "9", "8", "7", "6", "5", "4", "3", "2", "1" };
		gd.addChoice("SIMULATE", livelli, def1);

		gd.addCheckbox("ALL COILS", false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		String level = gd.getNextChoice();
		boolean tutte = gd.getNextBoolean();
		int livello = Integer.parseInt(level);
		Prefs.set("prefer.Uncombined3D_MAPPAZZA_def1", level);

		int gridWidth = 2;
		int gridHeight = livello;
		int gridSize = gridWidth * gridHeight;
		TextField[] tf2 = new TextField[gridSize];
		// String[] lab2 = new String[gridSize];
		String[] lab2 = { "min% classe 1", "max% classe 1", "min% classe 2", "max% classe 2", "min% classe 3",
				"max% classe 3", "min% classe 4", "max% classe 4", "min% classe 5", "max% classe 5", "min% classe 6",
				"max% classe 6", "min% classe7", "max% classe 7", "min% classe 8", "max% classe 8", "min% classe 9",
				"max% classe 9", "min% classe 10", "max% classe 10", "min% classe 11", "max% classe 11",
				"min% classe 12", "max% classe 12" };
		double[] value2 = new double[gridSize];

		for (int i1 = 0; i1 < value2.length; i1++) {
			value2[i1] = getValue2(Prefs.get("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, "0"));
		}

		if (showDialog2(gridWidth, gridHeight, tf2, lab2, value2)) {
			// displayValues2(gridSize, value2);
		}

		for (int i1 = 0; i1 < value2.length; i1++) {
			Prefs.set("prefer.Uncombined3D_MAPPAZZA_classi_" + i1, value2[i1]);
		}

		// MyLog.resultsLog(value2, "value2");
		// MyLog.waitHere();

		int[] minimi = new int[livello];
		int[] massimi = new int[livello];
		int conta = 0;
		for (int i1 = 0; i1 < livello; i1++) {
			minimi[i1] = (int) value2[conta++];
			massimi[i1] = (int) value2[conta++];
		}
		// MyLog.resultsLog(minimi, "minimi");
		// MyLog.resultsLog(massimi, "massimi");
		// MyLog.waitHere();

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
				if (imp10.getBytesPerPixel() != 2) {
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
			int mode = 0;
			boolean pitturaPixel = false;
			boolean stampa = true;
			int latoHotspot = 0;
			ArrayList<Integer> pixListSignal11 = new ArrayList<Integer>();

			// ===========================================================
			// ===========================================================
			// ---- NUOVA SOLUZIONE NUMERO DUE
			// ---- ricerca posizione x,y,z del massimo hotspot NxNxN
			// ===========================================================
			// ===========================================================
			double[] mediaHotspot = new double[imp10.getImageStackSize()];
			int[] indiceHotspot = new int[imp10.getImageStackSize()];
			double maxHotspot = -99999;
			int xCenter = 0;
			int yCenter = 0;
			int indice = 0;
			latoHotspot = 11;

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {
				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				if (imp20 == null)
					continue;
				double[] pos20 = hotspotSearch(imp20, latoHotspot, mode, timeout);
				if (pos20 == null) {
					continue;
				}
				int xRoi = (int) (pos20[0] - latoHotspot / 2);
				int yRoi = (int) (pos20[1] - latoHotspot / 2);
				ImagePlus imp21 = MyStackUtils.imageFromStack(imp10, i1 + 1);
				imp21.setRoi(xRoi, yRoi, latoHotspot, latoHotspot);
				ImageStatistics stat21 = imp21.getStatistics();
				indiceHotspot[i1] = i1;
				mediaHotspot[i1] = stat21.mean;
				if (mediaHotspot[i1] > maxHotspot) {
					maxHotspot = mediaHotspot[i1];
					indice = i1 + 1;
					xCenter = xRoi + latoHotspot / 2;
					yCenter = yRoi + latoHotspot / 2;
				}
				imp20.close();
				imp21.close();
			}

			if (debug) {
				MyLog.resultsLog(indiceHotspot, "indiceHotspot");
				MyLog.resultsLog(mediaHotspot, "mediaHotspot");

				MyLog.waitHere("Posizione dell'hotspot piu' alto: indice slice = " + indice + " xCenter= " + xCenter
						+ " yCenter= " + yCenter);
			}
			// --------------------------------------------
			// vettorizazione pixels degli hotspot 11x11x11
			// ---------------------------------------------
			int pip = (latoHotspot - 1) / 2;
			for (int i1 = indice - pip; i1 < indice + pip; i1++) {
				double xCenterRoi = xCenter;
				double yCenterRoi = yCenter;
				ImagePlus imp21 = MyStackUtils.imageFromStack(imp10, i1);

				pixVectorize(imp21, xCenterRoi, yCenterRoi, latoHotspot, pixListSignal11, pitturaPixel);
				imp21.close();
			}
			// }
			// ===========================================================
			// ===========================================================
			// ===========================================================
			// =============================================================

			int[] pixListSignal = ArrayUtils.arrayListToArrayInt(pixListSignal11);
			double mean11 = UtilAyv.vetMean(pixListSignal);

			for (int i1 = 0; i1 < imp10.getImageStackSize(); i1++) {

				ImagePlus imp20 = MyStackUtils.imageFromStack(imp10, i1 + 1);

				mappazzaColori(mean11, imp20, impMappazza, i1 + 1, livello, minimi, massimi, color0);
			}
			impMappazza.show();

			impMappazza.updateAndRepaintWindow();
			debug1 = true;
			if (!tutte)
				MyLog.waitHere();
		}
		MyLog.waitHere("trabalho concluido, Arbeit abgeshlossen");

	} // chiude

	public static void pixVectorize(ImagePlus imp11, double xCenterMROI, double yCenterMROI, double latoMROI,
			ArrayList<Integer> pixList11, boolean verify) {

		if (imp11 == null)
			MyLog.waitHere("imp11==null");
		if (pixList11 == null)
			MyLog.waitHere("pixList1==null");

		imp11.setRoi((int) Math.round((xCenterMROI - latoMROI / 2)), (int) Math.round(yCenterMROI - latoMROI / 2), 11,
				11);
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
					if (verify)
						ip11.putPixelValue(x + r11.x, y + r11.y, 1000);
				}
			}
		}
		if (verify) {
			ip11.drawRoi(roi11);
			roi11.setFillColor(Color.green);
			imp11.show();
			MyLog.waitHere();
		}
	}

	/***
	 * Ricerca delle coordinate centro area NxN con la media max
	 * 
	 * @param imp11
	 * @param info1
	 * @param mode
	 * @param timeout
	 * @return coordinate centro
	 */

	public static double[] hotspotSearch(ImagePlus imp11, int lato, int mode, int timeout) {

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

		double[] out10 = MyFilter.maxPositionGeneric(imp11, lato);
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
			int[] minimi, int[] massimi, int color) {

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
		int colorOUT = 0;
		ImageStack stack1 = impMappazza.getStack();
		ImageProcessor ipMappa = stack1.getProcessor(slice);
		int[] pixelsMappa = (int[]) ipMappa.getPixels();
		short pixSorgente = 0;
		int aux1 = 0;
		int posizioneArrayImmagine = 0;

		int[] myColor1 = new int[12];
		int[] myColor2 = new int[12];
		int[] myColor3 = new int[12];
		for (int i1 = 0; i1 < 12; i1++) {
			myColor1[i1] = ((255 & 0xff) << 16) | (((i1 * 20) & 0xff) << 8) | ((i1 * 20) & 0xff);
			myColor2[i1] = (((i1 * 20) & 0xff) << 16) | ((255 & 0xff) << 8) | ((i1 * 20) & 0xff);
			myColor3[i1] = (((i1 * 20) & 0xff) << 16) | (((i1 * 20) & 0xff) << 8) | (255 & 0xff);
		}
		int[] myColor = new int[12];
		if (color == 1)
			myColor = myColor1;
		if (color == 2)
			myColor = myColor2;
		if (color == 3)
			myColor = myColor3;

		if (debug) {
			MyLog.resultsLog(myColor, "myColor");
		}

		double[] myMinimi = new double[livello];
		double[] myMassimi = new double[livello];
		for (int i1 = 0; i1 < livello; i1++) {
			myMinimi[i1] = ((100.0 + (double) minimi[i1]) / 100) * mean;
			myMassimi[i1] = ((100.0 + (double) massimi[i1]) / 100) * mean;
		}

		if (debug) {

			MyLog.resultsLog(minimi, "minimi");
			MyLog.resultsLog(myMinimi, "myMinimi");
			MyLog.resultsLog(massimi, "massimi");
			MyLog.resultsLog(myMassimi, "myMassimi");
			MyLog.waitHere("livello= " + livello + " mean= " + mean);
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				boolean cerca = true;
				posizioneArrayImmagine = y * width + x;
				pixSorgente = pixels1[posizioneArrayImmagine];
				for (int i1 = 0; i1 < livello; i1++) {
					if (cerca && (pixSorgente > myMinimi[i1]) && (pixSorgente <= myMassimi[i1])) {
						aux1 = myColor[i1];
						cerca = false;
					}
				}
				if (cerca)
					aux1 = colorOUT;
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
		int red = rgb1[0] + rgb2[0] / 2;
		if (red > 255)
			red = 255;
		int green = rgb1[1] + rgb2[1] / 2;
		if (green > 255)
			green = 255;
		int blue = rgb1[2] + rgb2[2] / 2;
		if (blue > 255)
			blue = 255;
		int color = ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
		return color;
	}

	boolean showDialog2(int gridWidth, int gridHeight, TextField[] tf2, String[] lab2, double[] value2) {
		GenericDialog gd2 = new GenericDialog("LIMITI CLASSI PIXELS");
		gd2.addPanel(makePanel2(gd2, gridWidth, gridHeight, tf2, lab2, value2));
		gd2.showDialog();
		if (gd2.wasCanceled())
			return false;
		getValues2(gridWidth * gridHeight, tf2, value2);
		return true;
	}

	Panel makePanel2(GenericDialog gd2, int gridWidth, int gridHeight, TextField[] tf2, String[] lab2,
			double[] value2) {
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(gridHeight, gridWidth));
		int gridSize = gridWidth * gridHeight;
		for (int i1 = 0; i1 < gridSize; i1++) {
			tf2[i1] = new TextField("  " + IJ.d2s(value2[i1], 0));
			panel.add(new Label(lab2[i1]));
			panel.add(tf2[i1]);
		}
		return panel;
	}

	void getValues2(int gridSize, TextField[] tf2, double[] value2) {
		for (int i1 = 0; i1 < gridSize; i1++) {
			String s2 = tf2[i1].getText();
			value2[i1] = getValue2(s2);
		}
	}

	void displayValues2(int gridSize, double[] value2) {
		for (int i1 = 0; i1 < gridSize; i1++)
			IJ.log(i1 + " " + IJ.d2s(value2[i1], 0));
	}

	double getValue2(String theText) {
		Double d;
		String str = theText;
		try {
			str = str.replaceAll("\\s+", "");
			d = new Double(str);
		} catch (NumberFormatException e) {
			d = null;
		}
		return d == null ? Double.NaN : d.doubleValue();
	}

} // ultima
